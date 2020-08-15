package com.wangsz.netty.proxy;

import com.google.gson.Gson;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author shaoze.wang
 */
@Data
@ConfigurationProperties(prefix = "")
public class ProxyConfig {

    public static volatile boolean enableLog="true".equals(System.getProperty("netty.debug"));
    public static final String DELIMITER_STRING="#_$";
    public static final byte[] DELIMITER=DELIMITER_STRING.getBytes(StandardCharsets.UTF_8);


    private int managePort;
    private String manageAddress="0.0.0.0";
    private String managePassword;

    private int tunnelPort;
    private String tunnelAddress="0.0.0.0";

    private String clientId="defaultClient";
    private String clientToken="none";

    private Map<String,Service> services=new ConcurrentHashMap<>();

    private Map<String,String> clients=new ConcurrentHashMap<>();

    @Data
    public static final class Proxy{
        private int serverPort;
        private String serverAddress="0.0.0.0";
    }

    @Data
    public static final class Client{
        private int servicePort;
        private String serviceAddress;
    }

    @Data
    public static final class Service{
        private String clientId;
        private Proxy proxy;
        private Client client;
        private String protocol=Protocol.TCP;
    }

    public static final class Protocol{
        public static final String TCP="tcp";
    }

}
