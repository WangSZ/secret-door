package com.wangsz.netty.proxy.proxy;

import com.wangsz.netty.proxy.ProxyConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TunnelChannelInitializer extends ChannelInitializer<Channel> {
    private TunnelServer tunnelServer;

    public TunnelChannelInitializer(TunnelServer tunnelServer) {
        this.tunnelServer = tunnelServer;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        if(ProxyConfig.enableLog){
            ch.pipeline().addLast(new LoggingHandler());
        }
        ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
            private boolean ready=false;
            private String channelId;
            private String serviceName;
            private Tunnel tunnel;
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                if(ready){
                    msg.retain();
                    response(msg);
                }else {
                    int channelIdLength=msg.readInt();
                    int serviceNameLength=msg.readInt();
                    byte[] channelIdBytes=new byte[channelIdLength];
                    byte[] serviceNameBytes=new byte[serviceNameLength];
                    msg.readBytes(channelIdBytes,0,channelIdLength);
                    msg.readBytes(serviceNameBytes,0,serviceNameLength);
                    channelId=new String(channelIdBytes, StandardCharsets.UTF_8);
                    serviceName=new String(serviceNameBytes, StandardCharsets.UTF_8);
                    log.info("tunnel ready {}:{}",serviceName,channelId);
                    ready=true;
                    // 绑定 tunnel
                    ConcurrentHashMap<String, Tunnel> tunnels = tunnelServer.getProxyApp().getManageServer().getTunnelCache().get(serviceName);
                    Assert.notNull(tunnels,"service not exist :"+serviceName);
                    tunnel = tunnels.get(channelId);
                    Assert.notNull(tunnels,"channelId not exist :"+serviceName+" "+channelId);
                    tunnel.setClientToTunnelServer(ctx);
                    tunnel.getReady().set(true);
                    tunnel.getProxyIn().channel().config().setAutoRead(true);
                    if(msg.readableBytes()>0){
                        ByteBuf buf = msg.readBytes(msg.readableBytes());
                        response(buf);
                    }
                }
            }

            private void response(ByteBuf msg){
                tunnel.getProxyIn().channel().writeAndFlush(msg);
            }
        });

    }
}
