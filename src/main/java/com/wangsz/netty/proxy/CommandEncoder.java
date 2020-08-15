package com.wangsz.netty.proxy;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class CommandEncoder extends MessageToMessageEncoder<Command> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Command msg, List<Object> out) throws Exception {
        Gson gson = new Gson();
        out.add(gson.toJson(msg)+ProxyConfig.DELIMITER_STRING);
    }
}