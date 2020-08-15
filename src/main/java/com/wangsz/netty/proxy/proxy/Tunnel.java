package com.wangsz.netty.proxy.proxy;

import io.netty.channel.ChannelHandlerContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Slf4j
public final class Tunnel {
    private volatile String serviceName;
    private volatile ChannelHandlerContext proxyIn;
    private volatile ChannelHandlerContext clientToTunnelServer;
    private volatile AtomicBoolean ready=new AtomicBoolean(false);

    public void closeByProxy(){
        if(clientToTunnelServer!=null){
            clientToTunnelServer.close();
        }
    }
}
