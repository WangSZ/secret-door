package com.wangsz.netty.proxy.client;

import com.wangsz.netty.proxy.IService;
import com.wangsz.netty.proxy.ProxyConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
public class ClientService implements IService {
    private ClientApp clientApp;
    private ProxyConfig.Service service;
    private ConcurrentHashMap<String, ChannelFuture> tunnelCache = new ConcurrentHashMap<>();
    private String name;


    public ClientService(ClientApp clientApp, ProxyConfig.Service service, String name) {
        this.clientApp = clientApp;
        this.service = service;
        this.name = name;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        tunnelCache.forEachValue(1, channelFuture -> {
            channelFuture.channel().close();
        });
    }

    @Override
    public void reload() {

    }

    @Override
    public String getName() {
        return name;
    }

    public void newTunnel(String channelId, ChannelHandlerContext ctx) {
        synchronized (tunnelCache) {
            ChannelFuture existTunnel = tunnelCache.get(channelId);
            if (existTunnel != null) {
                log.warn("tunnel exist for channelId {} . channel: {}", channelId, existTunnel.channel());
                if (existTunnel.channel().isActive()) {
                    return;
                }
                tunnelCache.remove(channelId);
            }
        }

        ChannelFuture channelFuture = clientApp.newServerBootstrap()
                .handler(new ClientServiceChannelInitializer(this, channelId))
                .connect(clientApp.getProxyConfig().getTunnelAddress(), clientApp.getProxyConfig().getTunnelPort())
                .addListener(new GenericFutureListener<ChannelFuture>() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        log.info("connected to proxy Tunnel {}", future.channel());
                        initTunnel(future, channelId);
                    }
                });
        channelFuture.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                tunnelCache.remove(channelId);
                log.info("remove tunnel {}:{}. remains {}", getName(), channelId, tunnelCache.size());
            }
        });
        tunnelCache.put(channelId, channelFuture);

    }

    public void initTunnel(ChannelFuture future, String channelId) {

        Assert.isTrue(future.channel().isActive(), "channel not active");
        byte[] channelIdBytes = channelId.getBytes(StandardCharsets.UTF_8);
        byte[] serviceNameBytes = getName().getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = future.channel().alloc().buffer(100);
        buf.writeInt(channelIdBytes.length);
        buf.writeInt(serviceNameBytes.length);
        buf.writeBytes(channelIdBytes);
        buf.writeBytes(serviceNameBytes);
        future.channel().writeAndFlush(buf);
    }


}
