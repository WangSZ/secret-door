package com.wangsz.netty.proxy;

import com.wangsz.netty.proxy.client.ClientApp;
import com.wangsz.netty.proxy.proxy.ProxyApp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Slf4j
@Import(ProxyConfig.class)
public class DemoApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	private final String MODE_SERVER="server";
	private final String MODE_CLIENT="client";

	@Autowired
	private ProxyConfig proxyConfig;
	@Value("${mode:both}")
	private String mode;

	@Override
	public void run(String... args) throws Exception {
		if(MODE_CLIENT.equals(mode)){
			ClientApp clientApp=new ClientApp(proxyConfig);
			clientApp.start();
		}else if(MODE_SERVER.equals(mode)){
			EncryptHelper encryptHelper=new EncryptHelper(proxyConfig.getManagePassword());
			proxyConfig.getClients().forEach((clientId,pass)->{
				log.info("clientId: {} clientToken: {}",clientId,encryptHelper.encrypt(String.format("%s %s %s",clientId,pass,System.nanoTime())));
			});
			ProxyApp proxyApp=new ProxyApp(proxyConfig);
			proxyApp.start();
		}else {
			ProxyConfig server=new ProxyConfig();
			ProxyConfig client=new ProxyConfig();
			BeanUtils.copyProperties(proxyConfig,server);
			BeanUtils.copyProperties(proxyConfig,client);
			ProxyApp proxyApp=new ProxyApp(server);
			proxyApp.start();
			ClientApp clientApp=new ClientApp(client);
			clientApp.start();
		}
	}
}
