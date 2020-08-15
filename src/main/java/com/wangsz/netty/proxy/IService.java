package com.wangsz.netty.proxy;

public interface IService {
    void start();
    void stop();
    void reload();
    String getName();
}
