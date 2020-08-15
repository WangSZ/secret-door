package com.wangsz.netty.proxy.proxy;

import com.wangsz.netty.proxy.Command;
import com.wangsz.netty.proxy.CommandDecoder;
import com.wangsz.netty.proxy.CommandEncoder;
import com.wangsz.netty.proxy.ProxyConfig;
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

@Getter
@Slf4j
public class ManageChannelInitializer extends ChannelInitializer<Channel> {
    private ManageServer manageServer;

    public ManageChannelInitializer(ManageServer manageServer) {
        this.manageServer = manageServer;
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

            String clientId;

            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                super.channelActive(ctx);
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Command command) throws Exception {
                if (clientId == null) {
                    if (Command.login.equals(command.getName())) {
                        try {
                            clientId = command.getArgs().get(0);
                            String token = manageServer.getEncryptHelper().decrypt(command.getArgs().get(1));
                            if (!clientId.equals(token.split(" ")[0])) {
                                log.info("bad token : {}", command);
                                ctx.close();
                                return;
                            }
                        } catch (Exception ex) {
                            log.warn("bad token {}", command, ex);
                            ctx.close();
                            return;
                        }
                        manageServer.registerClient(ctx);
                    } else {
                        log.warn("not login {}",command);
                    }
                } else {
                    if (Command.register.equals(command.getName())) {
                        // 注册节点 register service1
                        String serviceName = command.getArgs().get(0);
                        String requiredClientId = manageServer.getProxyConfig().getServices().get(serviceName).getClientId();
                        if(!clientId.equals(requiredClientId)){
                            log.error("client {} cant register service {} cause requiredClientId is {}", this.clientId, serviceName,requiredClientId);
                        }else {
                            log.info("client {} register service {}", this.clientId, serviceName);
                            manageServer.registerService(serviceName, ctx);
                        }
                    } else if (Command.clientStart.equals(command.getName())) {
                        manageServer.updateConfig(ctx,clientId);
                    } else if (Command.ping.getName().equals(command.getName())) {
                        ctx.writeAndFlush(Command.pong);
                    } else {
                        log.info("receive command {}", command);
                    }
                }
            }

        });

    }
}
