# BookMall 基础设施搭建说明

## 1. 环境说明

当前开发环境：

- Windows
- WSL
- Docker Desktop
- Docker Compose
- Java 微服务暂时运行在 Windows IDEA
- MySQL、Nacos、Redis 暂时运行在 Docker

当前访问关系：

    Windows IDEA 中运行的 Java 服务
            |
            | localhost:3306 / localhost:8848 / localhost:6379
            v
    WSL 中的 Docker 容器

由于 Java 微服务运行在 Windows，而基础设施运行在 Docker，容器必须通过端口映射暴露服务端口。当前本地开发阶段，Java 服务使用 localhost 访问这些端口。

## 2. 第 0 步：确认 Docker 环境

在 WSL 中执行：

    docker version
    docker compose version
    docker ps

当前已确认：

- Docker Client/Server 可用
- Docker Compose 可用
- MySQL 容器正在运行
- MySQL 映射端口为 3306
- 当前 Docker 命令不需要 sudo

## 3. 第 1 步：启动 Nacos

BookMall 使用 Nacos 作为微服务注册中心。后续 auth、book、cart、order、inventory、address 和 gateway 都会注册到 Nacos，Gateway 再通过服务名找到具体服务实例。

当前已有 Nacos 容器：

- 容器名：nacos
- 镜像：nacos/nacos-server:v2.5.3

启动命令：

    docker start nacos

查看容器状态：

    docker ps --filter name=nacos

查看最近日志：

    docker logs --tail 100 nacos

检查端口映射：

    docker inspect -f '{{json .HostConfig.PortBindings}}' nacos

Nacos 2.x 除了控制台端口 8848，还需要使用 9848、9849 作为 gRPC 通信端口。后续如果重新创建 Nacos 容器，需要同时映射：

    8848:8848
    9848:9848
    9849:9849

浏览器验证地址：

    http://localhost:8848/nacos

如果出现登录页面，默认账号通常为：

    用户名：nacos
    密码：nacos

## 4. 为什么先验证 Nacos

后续服务注册依赖 Nacos 地址。如果 Nacos 容器虽然存在但没有运行、端口没有映射，或者 gRPC 端口不可用，Java 服务启动时就可能出现注册失败、心跳失败或服务列表为空。

因此需要先确认：

1. 容器状态为 Up
2. 控制台可以通过浏览器打开
3. Windows 可以访问 localhost:8848

只有这三点都满足后，才进入下一步：让 bookmall-auth 注册到 Nacos。

## 5. 当前进度

- [x] 确认 WSL Docker 可用
- [x] 确认 MySQL 容器存在并运行
- [x] 找到已有 Nacos 容器
- [x] 验证 Nacos 控制台
- [ ] 启动 Redis
- [x] 配置并验证 bookmall-auth 注册 Nacos
- [x] 配置并验证 bookmall-book 注册 Nacos
- [x] 配置 bookmall-gateway 注册 Nacos
- [x] 将 Gateway 全部路由改为 Nacos 服务发现路由
- [x] 配置全部业务服务注册 Nacos
- [x] 配置订单服务通过 Nacos 调用下游服务
- [x] 将订单服务的远程调用改为 OpenFeign
- [ ] 启动并验证全部服务

## 7. 第 2 步：让 bookmall-auth 注册到 Nacos

这一阶段只接入 auth 服务，不立即修改全部模块。这样可以先验证 Spring Cloud Alibaba 依赖版本、Nacos 地址以及 Windows 到 Docker 的网络是否正确。如果注册失败，只需要排查一个服务。

### 7.1 添加服务发现依赖

在 bookmall-auth/pom.xml 中添加：

    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

父工程已经通过 spring-cloud-alibaba-dependencies 管理依赖版本，因此子模块不单独声明版本。

### 7.2 添加 Nacos 地址

在 bookmall-auth/application.yml 中添加：

    spring:
      application:
        name: bookmall-auth
      cloud:
        nacos:
          discovery:
            server-addr: localhost:8848
            namespace: public
            group: DEFAULT_GROUP

配置含义：

- application.name：注册到 Nacos 后显示的服务名
- server-addr：Nacos 地址
- namespace：使用公共命名空间
- group：使用默认服务分组

当前 auth 服务运行在 Windows IDEA 中，Nacos 端口从 Docker 映射到 Windows，所以使用 localhost:8848。将来 auth 也进入 Docker 后，需要改成 Nacos 容器服务名。

### 7.3 不添加 EnableDiscoveryClient 的原因

当前 Spring Cloud Alibaba 可以根据 discovery 依赖和配置自动注册服务，不需要在启动类上额外添加 EnableDiscoveryClient。减少不必要的注解也能保持启动类简单。

### 7.4 auth 注册验证结果

bookmall-auth 已经成功出现在 Nacos 服务列表中。这说明：

- Nacos 容器运行正常
- 8848 和客户端通信端口可用
- Windows 中运行的 Java 服务能够访问 Docker 中的 Nacos
- Spring Cloud Alibaba 与当前项目版本可以完成服务注册

## 8. 第 3 步：让 bookmall-book 注册到 Nacos

这一阶段接入第二个业务服务 bookmall-book。这样做是为了确认 Nacos 可以同时维护多个服务，并为后续 Gateway 按服务名转发图书请求做准备。

在 bookmall-book/pom.xml 中添加与 auth 相同的服务发现依赖：

    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

在 bookmall-book/application.yml 中增加：

    spring:
      application:
        name: bookmall-book
      cloud:
        nacos:
          discovery:
            server-addr: localhost:8848
            namespace: public
            group: DEFAULT_GROUP

bookmall-book 使用端口 8070，注册成功后 Nacos 服务列表中应同时存在：

- bookmall-auth
- bookmall-book

验证图书服务本身是否正常：

    http://localhost:8070/books/hello

### 8.1 book 注册验证结果

bookmall-book 已经成功出现在 Nacos 服务列表中。目前 Nacos 已经管理两个业务服务：

- bookmall-auth
- bookmall-book

## 9. 第 4 步：Gateway 接入 Nacos 服务发现

这一阶段让 bookmall-gateway 注册到 Nacos，并将 auth、book 两条路由从固定 IP 和端口改为服务名路由。

### 9.1 为什么需要 LoadBalancer

固定地址路由写法：

    uri: http://localhost:8060

这种写法绕过了 Nacos，Gateway 必须提前知道服务端口。当服务地址变化或启动多个实例时，需要手动修改配置。

服务发现路由写法：

    uri: lb://bookmall-auth

lb 表示使用 Spring Cloud LoadBalancer。Gateway 会先从 Nacos 查询 bookmall-auth 的健康实例，再选择一个实例转发请求。

因此 Gateway 需要两个依赖：

    spring-cloud-starter-alibaba-nacos-discovery
    spring-cloud-starter-loadbalancer

### 9.2 Gateway Nacos 配置

Gateway 使用以下服务发现配置：

    spring:
      application:
        name: bookmall-gateway
      cloud:
        nacos:
          discovery:
            server-addr: localhost:8848
            namespace: public
            group: DEFAULT_GROUP

### 9.3 当前改造的路由

auth 路由：

    uri: lb://bookmall-auth

book 路由：

    uri: lb://bookmall-book

其他服务目前尚未注册到 Nacos，所以暂时保留 localhost 固定地址。等对应服务注册成功后，再逐条改成 lb 路由。

### 9.4 验证目标

启动 auth、book 和 gateway 后，Nacos 服务列表应出现：

- bookmall-auth
- bookmall-book
- bookmall-gateway

通过 Gateway 验证：

    http://localhost:8080/api/auth/hello
    http://localhost:8080/api/books/hello

如果两条请求成功，说明 Gateway 已经可以通过 Nacos 服务发现转发请求。

## 10. 第 5 步：全部后端服务接入 Nacos

已为以下模块添加 Nacos Discovery 依赖和服务发现配置：

- bookmall-auth
- bookmall-book
- bookmall-cart
- bookmall-order
- bookmall-inventory
- bookmall-address
- bookmall-gateway

服务名和端口对应关系：

| 服务名 | 端口 |
| --- | ---: |
| bookmall-gateway | 8080 |
| bookmall-auth | 8060 |
| bookmall-book | 8070 |
| bookmall-cart | 8083 |
| bookmall-order | 8050 |
| bookmall-inventory | 8085 |
| bookmall-address | 8086 |

Gateway 的所有路由都已改为 lb 服务发现路由：

    lb://bookmall-auth
    lb://bookmall-book
    lb://bookmall-cart
    lb://bookmall-order
    lb://bookmall-inventory
    lb://bookmall-address

### 10.1 为什么订单服务也需要 LoadBalancer

订单服务需要调用图书、库存和地址服务。原来使用 localhost 和固定端口，现在改为：

    http://bookmall-book
    http://bookmall-inventory
    http://bookmall-address

这些名称不是普通 DNS 地址，而是 Nacos 服务名。因此订单服务添加 spring-cloud-starter-loadbalancer，并在 RestTemplate Bean 上增加 LoadBalanced。请求发出时，LoadBalancer 会从 Nacos 获取健康实例地址。

### 10.2 当前验证目标

全部服务启动后，Nacos 服务列表应该有 7 个服务，每个服务的实例数为 1。

通过 Gateway 测试：

    http://localhost:8080/api/auth/hello
    http://localhost:8080/api/books/hello
    http://localhost:8080/api/cart/hello
    http://localhost:8080/api/orders/hello
    http://localhost:8080/api/inventory/hello
    http://localhost:8080/api/address/hello

## 6. 后续步骤

后续每一步都单独完成并在本文档更新：

1. 验证 Nacos
2. 启动并验证 Redis
3. 配置 bookmall-auth 注册 Nacos
4. 配置其他业务服务注册 Nacos
5. 将 Gateway 固定地址路由改为 Nacos 服务发现路由
6. 让订单服务通过 Nacos 调用图书、库存和地址服务
7. 在认证服务中接入 Redis
8. 增加 Gateway JWT 鉴权

## 11. 第 6 步：使用 OpenFeign 完成服务间远程调用

### 11.1 当前开发顺序走到哪里

按照项目最初的 10 步安排，目前已经完成父工程、公共模块、认证、图书、订单、库存、Nacos 注册和 Gateway 服务发现。购物车与地址服务也已经存在。现在正在完成第 9 步中的最后一块：服务之间使用 OpenFeign 调用。

Redis、Sentinel、Seata、RabbitMQ、链路追踪属于后续增强项，目前还没有完成。

### 11.2 为什么使用 OpenFeign

订单服务创建订单时需要访问三个服务：

- bookmall-book：读取图书标题和价格
- bookmall-inventory：扣减或恢复库存
- bookmall-address：读取并校验收货地址

之前使用 RestTemplate 手工拼接 URL、发送请求并把 Map 转换成 Java 对象。这样代码较多，也容易出现字段转换错误。OpenFeign 可以把 HTTP 接口声明成 Java 接口，业务代码像调用普通方法一样调用远程服务。

OpenFeign 中的 name 不是固定 IP，而是 Nacos 服务名。例如：

```java
@FeignClient(name = "bookmall-book")
public interface BookClient {
    @GetMapping("/books/{id}")
    Result<BookSnapshot> getBookById(@PathVariable("id") Long bookId);
}
```

调用链为：

```text
bookmall-order
  -> OpenFeign
  -> Spring Cloud LoadBalancer
  -> 从 Nacos 查询 bookmall-book 的实例
  -> 调用该实例的 /books/{id}
```

因此调用方不再关心 book 服务实际运行在 8070 还是其他端口。

### 11.3 本次代码改动

bookmall-order 已完成以下改造：

- 添加 spring-cloud-starter-openfeign 依赖
- 在 OrderApplication 添加 EnableFeignClients
- BookClient 改为 FeignClient，调用 bookmall-book
- InventoryClient 改为 FeignClient，调用 bookmall-inventory
- AddressClient 改为 FeignClient，调用 bookmall-address
- 删除 RestTemplateConfig
- 删除 application.yml 中三个固定下游地址
- 新增 BookSnapshot，避免订单模块依赖图书模块的内部 VO

订单模块目前仍依赖 bookmall-cart，是因为订单服务直接读取 t_cart 表和复用 Cart 实体。这不是远程调用。后续若要做到严格的数据库隔离，应再把购物车读取和删除改为 OpenFeign，并让订单服务不再直接访问购物车表。

### 11.4 为什么使用本地 Snapshot DTO

微服务不应该为了调用接口而直接依赖另一个业务服务的 Java 类。否则 book 服务内部类一改，order 服务会被迫一起编译和发布。

因此订单服务自己维护：

- BookSnapshot
- AddressSnapshot
- InventoryDeductRequest
- InventoryRecoverRequest

这些类只描述订单服务真正需要的接口字段，服务之间通过 JSON 传输。

### 11.5 启动顺序和验证方法

先启动依赖服务，再启动调用方：

1. 启动 MySQL 和 Nacos
2. 启动 bookmall-book
3. 启动 bookmall-inventory
4. 启动 bookmall-address
5. 启动 bookmall-order
6. 最后启动 bookmall-gateway

为什么按这个顺序：订单服务虽然通常可以在下游未启动时启动，但创建订单时三个下游必须在 Nacos 中有健康实例，否则 OpenFeign 会报找不到可用实例。

打开 Nacos 控制台，确认至少以下服务的实例数均为 1：

- bookmall-book
- bookmall-inventory
- bookmall-address
- bookmall-order

先分别验证下游接口：

```text
GET http://localhost:8070/books/1
GET http://localhost:8085/inventory/1
GET http://localhost:8086/address/1
```

再通过 Gateway 创建订单：

```http
POST http://localhost:8080/api/orders
Content-Type: application/json

{
  "userId": 3,
  "addressId": 1,
  "cartItemIds": [1]
}
```

创建成功说明 order 已经通过 OpenFeign 完成地址查询、库存扣减和图书查询。测试会真实扣减库存并删除对应购物车项，因此应使用测试数据。

### 11.6 常见错误

出现 Load balancer does not contain an instance for the service 时，先检查对应服务是否已经启动并注册到 Nacos，以及 FeignClient 的 name 是否与 spring.application.name 完全一致。

出现 Connection refused 时，检查 Nacos 中注册的实例 IP 和端口是否能从 Windows 上的订单服务访问。

出现 404 时，检查 FeignClient 中的请求路径是否与下游 Controller 路径一致。

出现 400 时，检查请求体字段和下游 DTO 校验规则。
