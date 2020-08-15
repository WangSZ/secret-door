package com.wangsz.netty.proxy.proxy;

import com.wangsz.netty.proxy.IService;
import com.wangsz.netty.proxy.ProxyConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class TunnelServer implements IService {

    private ProxyConfig proxyConfig;
    private ServerBootstrap proxyServerBootstrap;
    private ProxyApp proxyApp;

    public TunnelServer(ProxyConfig proxyConfig, ServerBootstrap proxyServerBootstrap, ProxyApp proxyApp) {
        this.proxyConfig = proxyConfig;
        this.proxyServerBootstrap = proxyServerBootstrap;
        this.proxyApp = proxyApp;
    }

    @Override
    public void start() {
        ChannelInitializer<Channel> initializer = new TunnelChannelInitializer(this);
        proxyServerBootstrap.childHandler(initializer).bind(proxyConfig.getTunnelAddress(),proxyConfig.getTunnelPort());
        log.info("TunnelServer started at {}:{}",proxyConfig.getTunnelAddress(),proxyConfig.getTunnelPort());
    }

    @Override
    public void stop() {

    }

    @Override
    public void reload() {

    }

    @Override
    public String getName() {
        return "TunnelServer";
    }

}
