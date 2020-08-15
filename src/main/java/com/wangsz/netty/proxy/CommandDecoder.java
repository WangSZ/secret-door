package com.wangsz.netty.proxy;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class CommandDecoder  extends MessageToMessageDecoder<String>{

    @Override
    protected void decode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
        Gson gson = new Gson();
        out.add(gson.fromJson(msg, Command.class));
    }
}