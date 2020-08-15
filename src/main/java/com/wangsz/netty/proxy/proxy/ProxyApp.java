package com.wangsz.netty.proxy.proxy;

import com.wangsz.netty.proxy.EncryptHelper;
import com.wangsz.netty.proxy.IService;
import com.wangsz.netty.proxy.ProxyConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author shaoze.wang
 */
@Slf4j
@Getter
public class ProxyApp implements IService {


    private NioEventLoopGroup boss=new NioEventLoopGroup(1);
    private NioEventLoopGroup worker=new NioEventLoopGroup();
    private ProxyConfig proxyConfig;
    private ManageServer manageServer;
    private TunnelServer tunnelServer;
    private ConcurrentHashMap<String,ProxyServer> proxyServers=new ConcurrentHashMap<>();
    private EncryptHelper manageEncryptHelper;

    public ProxyApp(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
        this.manageEncryptHelper=new EncryptHelper(proxyConfig.getManagePassword());
    }

    @Override
    public void start() {
        manageServer=new ManageServer(proxyConfig,newServerBootstrap(), ProxyApp.this);
        manageServer.start();
        tunnelServer=new TunnelServer(proxyConfig,newServerBootstrap(), ProxyApp.this);
        tunnelServer.start();
        proxyConfig.getServices().forEach((name,serviceConfig)->{
            ProxyServer proxyServer=new ProxyServer(name,serviceConfig,proxyConfig,newServerBootstrap(), ProxyApp.this);
            proxyServers.put(name,proxyServer);
            proxyServer.start();
        });
    }

    public ServerBootstrap newServerBootstrap(){
        return new ServerBootstrap().group(boss,worker).channelFactory(NioServerSocketChannel::new).childOption(ChannelOption.TCP_NODELAY, true);
    }

    @Override
    public void stop() {
        manageServer.stop();
        proxyServers.forEachValue(1,ProxyServer::stop);
        try {
            boss.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            log.error(e.getMessage(),e);
        }
        try {
            worker.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            log.error(e.getMessage(),e);
        }
    }

    @Override
    public void reload() {
        manageServer.reload();
        proxyServers.forEachValue(1,ProxyServer::reload);
    }

    @Override
    public String getName() {
        return "ProxyApp";
    }

}