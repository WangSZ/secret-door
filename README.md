# secret-door

自家用内网穿透工具，方便的通过控台增减内网服务。tcp tunnel for your home

## 原理


角色
1. server 代表拥有公网IP的服务器，比如阿里云的机器
2. client 代表内网运行穿透工具的机器，比如路由器、开发机等
3. service 代表内网运行的要被穿透的服务，比如提供云盘的服务器（client 和 service也可以在一起）
4. user 代表要从外网访问内网服务的人

端口：
1. 公网管理控制端口 managePort。用于 client 和 server 通讯
2. 公网隧道端口 tunnelPort。用于提供穿透流量的隧道，链接 server 和 client
3. 公网代理端口 proxy.serverPort。公网访问服务的端口
4. 内网服务端口 client.servicePort。内网提供服务的端口

流量链路：
```text
user <-> proxy.serverPort <-channel-> tunnelPort <-channel-> client.servicePort
```

# Docker 方式运行
运行容器，`/etc/secret-door`下新建`application.yaml`文件，内容参考后文
```shell
docker run  --restart=always -d --name sds -v /share/nas/data/secret-door-s/:/etc/secret-door wszd/sds
docker run  --restart=always -d --name sdc -v /share/nas/data/secret-door-c/:/etc/secret-door wszd/sdc
```

# jar 方式运行

jar包和application.yaml放到同一个目录，运行`java -jar app.jar`启动
## server
```yaml
# 0.0.0.0表示监听所有ip
manageAddress: 0.0.0.0
managePort: 2150
tunnelAddress: 0.0.0.0
tunnelPort: 2151

# 服务端配置
managePassword: passwd
# 可以配置多个client，每个client用不同的密码。如果密码修改了，那么token需要重新生成
clients:
  user1: pass
  user2: pass
  user3: pass
  user4: pass
services:
  nas: # 服务名称
    clientId: user2 # 表示 nas 服务由 user2 提供
    protocol: tcp
    proxy:
      serverAddress: 0.0.0.0
      serverPort: 2199
    client:
      serviceAddress: 172.17.1.118
      servicePort: 888
  nas1: # 服务名称
    clientId: user1 # 表示 nas1 服务由 user1 提供
    protocol: tcp
    proxy:
      serverAddress: 0.0.0.0
      serverPort: 2199
    client:
      serviceAddress: 172.17.1.118
      servicePort: 888

```

`java -Dmode=server -jar app.jar` 
启动后会打印每个client的token，把token放到client配置中即可（token每次重启都会变，但是，只要client的password没有变，token就不会失效）

```text
2020-08-15 17:43:10.151  INFO 13616 --- [           main] DemoApplication         : clientId: user1 clientToken: dTudDyvOj+JD1Qnvl32ydU3M1NY5VpzDjDcxSD+nl9E=
2020-08-15 17:43:10.155  INFO 13616 --- [           main] DemoApplication         : clientId: user2 clientToken: WIiUEIyS3tgUW/ZqGISnXBiQrRLxJ7oPfCSvxKDM/x4=
2020-08-15 17:43:10.156  INFO 13616 --- [           main] DemoApplication         : clientId: user3 clientToken: Bmyw6UAvOwz3ZwdZMIUkeeEWCO6UtGt5E5VbUjEICbQ=
2020-08-15 17:43:10.156  INFO 13616 --- [           main] DemoApplication         : clientId: user4 clientToken: ymBmaO6/+Uo4Qpcnzm62EFrZqiADRFrGx5R5tvwBnqU=
```

## client
`java -Dmode=client -jar app.jar`
```yaml
# 公网ip
manageAddress: 121.55.55.55
managePort: 2150
tunnelAddress: 121.55.55.55
tunnelPort: 2151

# 客户端配置
clientId: user1
clientToken: 4V9sFuHg1I3s4KE2fppTnkTbQ0JuG3zj9qhzHu7Q3iM=
```



# 构建Docker镜像

1. 编译项目 `mvn install`
2. 构建server镜像
`docker build --network=host -f dockerfile.server -t wszd/sds .`
3. 构建client镜像
`docker build --network=host -f dockerfile.client -t wszd/sdc .`
