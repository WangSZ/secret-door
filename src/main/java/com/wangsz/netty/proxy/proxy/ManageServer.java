package com.wangsz.netty.proxy.proxy;

import com.google.gson.Gson;
import com.wangsz.netty.proxy.Command;
import com.wangsz.netty.proxy.EncryptHelper;
import com.wangsz.netty.proxy.IService;
import com.wangsz.netty.proxy.ProxyConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
public class ManageServer implements IService {

    private ProxyConfig proxyConfig;
    private ServerBootstrap proxyServerBootstrap;
    private ProxyApp proxyApp;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Tunnel>> tunnelCache=new ConcurrentHashMap<>();
    private EncryptHelper encryptHelper;

    public ManageServer(ProxyConfig proxyConfig, ServerBootstrap proxyServerBootstrap, ProxyApp proxyApp) {
        this.proxyConfig = proxyConfig;
        this.proxyServerBootstrap = proxyServerBootstrap;
        this.proxyApp = proxyApp;
        this.encryptHelper=new EncryptHelper(proxyConfig.getManagePassword());
    }

    public void newTunnel(ProxyServer proxyServer, ChannelHandlerContext ctx){
        Tunnel tunnel=new Tunnel();
        tunnel.setProxyIn(ctx);
        tunnel.setServiceName(proxyServer.getName());
        if(!tunnelCache.containsKey(proxyServer.getName())){
            tunnelCache.computeIfAbsent(proxyServer.getName(),s -> new ConcurrentHashMap<>());
        }
        tunnelCache.get(proxyServer.getName()).put(ctx.channel().id().asLongText(),tunnel);
        ctx.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                Tunnel remove = tunnelCache.get(proxyServer.getName()).remove(ctx.channel().id().asLongText());
                log.info("remove tunnel {}:{}. remains {}",proxyServer.getName(),ctx.channel().id().asLongText(),tunnelCache.get(proxyServer.getName()).size());
                if(remove!=null){
                    remove.closeByProxy();
                }
            }
        });
        ChannelHandlerContext clientCtx = managedServiceCache.get(proxyServer.getName());
        if(clientCtx==null){
            log.info("no client for service {}. closing channel {}",proxyServer.getName(),ctx.channel());
            ctx.close();
        }else {
            clientCtx.writeAndFlush(new Command(Command.newTunnel, Arrays.asList(proxyServer.getName(),tunnel.getProxyIn().channel().id().asLongText())));
        }
    }

    public void proxyChannelRead(ProxyServer proxyServer, ChannelHandlerContext ctx, ByteBuf msg) {
        Tunnel tunnel = tunnelCache.get(proxyServer.getName()).get(ctx.channel().id().asLongText());
        Assert.isTrue(tunnel.getReady().get(),"tunnel not ready");
        msg.retain();
        tunnel.getClientToTunnelServer().channel().writeAndFlush(msg);
    }


    @Override
    public void start() {
        ChannelInitializer<Channel> initializer = new ManageChannelInitializer(this);
        proxyServerBootstrap.childHandler(initializer).bind(proxyConfig.getManageAddress(),proxyConfig.getManagePort());
        log.info("ManageServer started at {}:{}",proxyConfig.getManageAddress(),proxyConfig.getManagePort());
    }

    private final ConcurrentHashMap<String,ChannelHandlerContext> manageClientCache =new ConcurrentHashMap<>();

    public void registerClient(ChannelHandlerContext ctx) {
        manageClientCache.put(ctx.channel().id().asLongText(),ctx);
        ctx.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                manageClientCache.remove(ctx.channel().id().asLongText());
            }
        });
    }

    void updateConfig(ChannelHandlerContext ctx, String clientId) {
        Map<String, ProxyConfig.Service> update=new HashMap<>();
        proxyConfig.getServices().forEach((k,v)->{
            if(v.getClientId().equals(clientId)){
                update.put(k,v);
            }
        });
        ctx.channel().writeAndFlush(new Command(Command.updateConfig, Collections.singletonList(new Gson().toJson(update))));
    }

    private final ConcurrentHashMap<String,ChannelHandlerContext> managedServiceCache =new ConcurrentHashMap<>();

    void registerService(String serviceName, ChannelHandlerContext ctx) {
        synchronized (managedServiceCache){
            if(managedServiceCache.containsKey(serviceName)&&!ctx.equals(managedServiceCache.get(serviceName))){
                log.warn("service {} exist. closing channel {}",serviceName,ctx.channel());
                ctx.close();
            }
            log.info("service {} register at {}",serviceName,ctx.channel());
            managedServiceCache.put(serviceName,ctx);
        }
        ctx.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                managedServiceCache.remove(serviceName);
            }
        });
    }

    @Override
    public void stop() {

    }

    @Override
    public void reload() {

    }

    @Override
    public String getName() {
        return "ManageServer";
    }

}
