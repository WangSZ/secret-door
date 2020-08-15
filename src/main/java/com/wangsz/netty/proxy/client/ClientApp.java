package com.wangsz.netty.proxy.client;

import com.wangsz.netty.proxy.IService;
import com.wangsz.netty.proxy.ProxyConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author shaoze.wang
 */
@Getter
public class ClientApp implements IService {
    private ProxyConfig proxyConfig;
    private NioEventLoopGroup worker = new NioEventLoopGroup();
    private ConcurrentHashMap<String, ClientService> clientServices = new ConcurrentHashMap<>();

    public ClientApp(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public void start() {
        ManageClient manageClient = new ManageClient(proxyConfig, newServerBootstrap(), this);
        manageClient.start();
    }

    public Bootstrap newServerBootstrap() {
        return new Bootstrap().group(worker)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .option(ChannelOption.TCP_NODELAY, true);
    }

    public Bootstrap newServerBootstrap(EventLoopGroup group) {
        return new Bootstrap().group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .option(ChannelOption.TCP_NODELAY, true);
    }
    @Override
    public void stop() {

    }

    @Override
    public void reload() {

    }

    @Override
    public String getName() {
        return "ProxyApp";
    }

}