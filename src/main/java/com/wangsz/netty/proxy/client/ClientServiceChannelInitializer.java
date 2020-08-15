package com.wangsz.netty.proxy.client;

import com.wangsz.netty.proxy.ProxyConfig;
import io.netty.channel.*;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ClientServiceChannelInitializer extends ChannelInitializer<Channel> {

    private ClientService clientService;
    private String channelId;

    public ClientServiceChannelInitializer(ClientService clientService, String channelId) {
        this.clientService = clientService;
        this.channelId = channelId;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        if (ProxyConfig.enableLog) {
            ch.pipeline().addLast(new LoggingHandler());
        }
        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            private ChannelFuture clientServiceChannelFuture;

            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                clientServiceChannelFuture = clientService.getClientApp()
                        .newServerBootstrap(ctx.channel().eventLoop())
                        .handler(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                clientService.getTunnelCache().get(channelId).channel().writeAndFlush(msg);
                            }
                        })
                        .connect(clientService.getService().getClient().getServiceAddress(), clientService.getService().getClient().getServicePort());
                ctx.channel().config().setAutoRead(false);
                clientServiceChannelFuture.addListener((GenericFutureListener<ChannelFuture>) future -> {
                    Object ex = future.get();
                    if (ex instanceof Throwable) {
                        log.error(((Throwable) ex).getMessage(), ex);
                    }
                    ctx.channel().config().setAutoRead(true);
                });
                super.channelActive(ctx);
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                clientServiceChannelFuture.channel().writeAndFlush(msg);
            }
        });
    }
}
