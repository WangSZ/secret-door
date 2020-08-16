package com.wangsz.netty.proxy.client;

import com.wangsz.netty.proxy.Command;
import com.wangsz.netty.proxy.IService;
import com.wangsz.netty.proxy.ProxyConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class ManageClient implements IService {
    private ProxyConfig proxyConfig;
    private Bootstrap bootstrap;
    private ChannelFuture channelFuture;
    private ClientApp clientApp;
    private boolean running = false;

    public ManageClient(ProxyConfig proxyConfig, Bootstrap bootstrap, ClientApp clientApp) {
        this.proxyConfig = proxyConfig;
        this.bootstrap = bootstrap;
        this.clientApp = clientApp;
    }

    private ChannelFuture login(Channel ctx) {
        return ctx.writeAndFlush(new Command(Command.login, Arrays.asList(getProxyConfig().getClientId(),getProxyConfig().getClientToken())));
    }

    void stopAllService() {
        clientApp.getClientServices().forEachValue(1, ClientService::stop);
    }

    @Override
    public void start() {
        running = true;
        channelFuture = bootstrap
                .handler(new ManageClientChannelInitializer(clientApp, this))
                .connect(proxyConfig.getManageAddress(), proxyConfig.getManagePort())
                .addListener(new GenericFutureListener<ChannelFuture>() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            log.info("connected to proxy management {}:{}", proxyConfig.getManageAddress(), proxyConfig.getManagePort());
                            channelFuture.channel().eventLoop().scheduleAtFixedRate(() -> {
                                channelFuture.channel().writeAndFlush(Command.ping);
                            }, 1, 66, TimeUnit.SECONDS);
                            login(future.channel()).addListener(loginFuture -> {
                                if(loginFuture.isSuccess()){
                                    future.channel().writeAndFlush(new Command(Command.clientStart));
                                }else {
                                    log.error("login fail");
                                    System.exit(0);
                                }
                            });
                        }else {
                            log.error("cant connect to proxy server {}:{}",proxyConfig.getManageAddress(), proxyConfig.getManagePort());
                        }
                    }
                });
        channelFuture.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                log.info("connection is closed {}", future.get());
                if (running) {
                    int delay = 3;
                    log.info("reconnect after {} seconds", delay);
                    channelFuture.channel().eventLoop().schedule(() -> {
                        reconnect();
                    }, delay, TimeUnit.SECONDS);
                }
            }
        });
    }

    private void reconnect() {
        start();
    }

    public void registerService() {

        proxyConfig.getServices().forEach((name, service) -> {
            ClientService clientService = new ClientService(clientApp, service,name);
            clientApp.getClientServices().put(name, clientService);
            clientService.start();
        });
        clientApp.getClientServices().forEach((name, clientService) -> {
            Assert.isTrue(channelFuture.channel().isActive(), "channel not active");
            channelFuture.channel().writeAndFlush(new Command(Command.register, Arrays.asList(name)));
            log.info("register service {}", name);
        });

    }

    @Override
    public void stop() {
        running = false;
        channelFuture.channel().close();
    }

    @Override
    public void reload() {

    }

    @Override
    public String getName() {
        return "ManageClient";
    }
}
