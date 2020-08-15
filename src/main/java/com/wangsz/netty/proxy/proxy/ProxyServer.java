package com.wangsz.netty.proxy.proxy;

import com.wangsz.netty.proxy.IService;
import com.wangsz.netty.proxy.ProxyConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyServer implements IService {
    private ProxyConfig.Service service;
    private ProxyConfig proxyConfig;
    private ServerBootstrap proxyServerBootstrap;
    private ProxyApp proxyApp;
    private ChannelFuture channelFuture;
    private String name;

    public ProxyServer(String name, ProxyConfig.Service service, ProxyConfig proxyConfig, ServerBootstrap proxyServerBootstrap, ProxyApp proxyApp) {
        this.name=name;
        this.service = service;
        this.proxyConfig = proxyConfig;
        this.proxyServerBootstrap = proxyServerBootstrap;
        this.proxyApp = proxyApp;
    }

    @Override
    public void start() {
        ChannelInitializer<Channel> initializer = new ProxyChannelInitializer(proxyApp,this);
        channelFuture = proxyServerBootstrap.childHandler(initializer).bind(service.getProxy().getServerAddress(), service.getProxy().getServerPort());

        log.info("ProxyServer {} started at {}:{}",getName(),service.getProxy().getServerAddress(), service.getProxy().getServerPort());
    }

    @Override
    public void stop() {
        proxyApp.getProxyServers().remove(getName());
        channelFuture.channel().closeFuture().addListener(new FutureListener(){

            @Override
            public void operationComplete(Future future) throws Exception {
                log.info("ProxyServer {} stopped", ProxyServer.this.getName());
            }
        });
        channelFuture.channel().close();
    }

    @Override
    public void reload() {

    }

    @Override
    public String getName() {
        return name;
    }

    @Slf4j
    public static class ProxyChannelInitializer extends ChannelInitializer<Channel> {

        private ProxyApp proxyApp;
        private ProxyServer proxyServer;

        public ProxyChannelInitializer(ProxyApp proxyApp, ProxyServer proxyServer) {
            this.proxyApp = proxyApp;
            this.proxyServer = proxyServer;
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                @Override
                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                    log.info("new channel active {}",ctx.channel());
                    ctx.channel().config().setAutoRead(false);
                    proxyApp.getManageServer().newTunnel(proxyServer,ctx);
                    super.channelActive(ctx);
                }

                @Override
                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                    proxyApp.getManageServer().proxyChannelRead(proxyServer,ctx,msg);
                }

            });
        }
    }
}
