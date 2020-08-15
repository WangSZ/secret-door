package com.wangsz.netty.proxy.client;

import com.wangsz.netty.proxy.Command;
import com.wangsz.netty.proxy.CommandDecoder;
import com.wangsz.netty.proxy.CommandEncoder;
import com.wangsz.netty.proxy.ProxyConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Map;

@Getter
@Slf4j
public final class ManageClientChannelInitializer extends ChannelInitializer<Channel> {

    private ClientApp clientApp;
    private ManageClient manageClient;

    public ManageClientChannelInitializer(ClientApp clientApp, ManageClient manageClient) {
        this.clientApp = clientApp;
        this.manageClient = manageClient;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        if (ProxyConfig.enableLog) {
            ch.pipeline().addLast(new LoggingHandler());
        }
        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(10 * 1024 * 1024, Unpooled.wrappedBuffer(ProxyConfig.DELIMITER)));
        ch.pipeline().addLast(new StringDecoder(CharsetUtil.UTF_8));
        ch.pipeline().addLast(new CommandDecoder());
        ch.pipeline().addLast(new StringEncoder(CharsetUtil.UTF_8));
        ch.pipeline().addLast(new CommandEncoder());
        ch.pipeline().addLast(new SimpleChannelInboundHandler<Command>() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                super.channelActive(ctx);
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Command command) throws Exception {
                log.debug("receive command {}", command);
                if (Command.newTunnel.equals(command.getName())) {
                    // 打开新的隧道
                    String serviceName = command.getArgs().get(0);
                    String channelId = command.getArgs().get(1);
                    ClientService clientService = clientApp.getClientServices().get(serviceName);
                    Assert.notNull(clientService, "clientService not exist " + serviceName);
                    clientService.newTunnel(channelId,ctx);
                } else if (Command.updateConfig.equals(command.getName())) {
                    Map<String, ProxyConfig.Service> services = new Gson().fromJson(command.getArgs().get(0), new TypeToken<Map<String, ProxyConfig.Service>>() {
                    }.getType());
                    manageClient.stopAllService();
                    clientApp.getProxyConfig().setServices(services);
                    manageClient.registerService();
                }
            }

        });

    }

}
