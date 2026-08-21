# BookMall Gateway 模块说明文档

## 1. 项目概述

`bookmall-gateway` 是 BookMall 项目中的统一网关模块，负责所有前端请求的统一入口和路由转发。

当前这个模块已经完成了第一版可用配置，能够将请求转发到：

- `bookmall-auth`
- `bookmall-book`

这个文档用于记录当前网关模块的实现状态、配置、路由规则、测试方式和后续计划。

## 2. 当前项目结构

当前根工程为 Maven 聚合工程，模块如下：

- `bookmall-common`
- `bookmall-auth`
- `bookmall-book`
- `bookmall-gateway`

当前项目目录中的关键部分：

- [pom.xml](D:/workspace_idea/BookMall/BookMall/pom.xml)
- [bookmall-common](D:/workspace_idea/BookMall/BookMall/bookmall-common)
- [bookmall-auth](D:/workspace_idea/BookMall/BookMall/bookmall-auth)
- [bookmall-book](D:/workspace_idea/BookMall/BookMall/bookmall-book)
- [bookmall-gateway](D:/workspace_idea/BookMall/BookMall/bookmall-gateway)

## 3. 父工程配置说明

父工程用于统一管理子模块和依赖版本，不直接编写业务代码。

当前父工程配置要点：

- `packaging` 为 `pom`
- Java 版本为 `17`
- 统一管理 Spring Boot、Spring Cloud、Spring Cloud Alibaba 版本
- 当前模块包括：
  - `bookmall-common`
  - `bookmall-auth`
  - `bookmall-book`
  - `bookmall-gateway`

当前使用的版本：

- Spring Boot `3.2.5`
- Spring Cloud `2023.0.2`
- Spring Cloud Alibaba `2023.0.1.0`

## 4. bookmall-common 模块说明

`bookmall-common` 是公共基础模块，用于存放所有服务都能复用的通用类。

当前包含的类：

- `Result<T>`：统一接口返回体
- `ErrorCode`：错误码枚举
- `BusinessException`：业务异常类

当前包结构：

- `com.bookmall.common.result`
- `com.bookmall.common.constant`
- `com.bookmall.common.exception`

### 4.1 Result<T>

文件路径：

- [Result.java](D:/workspace_idea/BookMall/BookMall/bookmall-common/src/main/java/com/bookmall/common/result/Result.java)

用途：

- 统一接口返回格式
- 所有服务尽量都使用这个结构返回数据

常用方法：

- `Result.success(data)`
- `Result.success()`
- `Result.fail(code, message)`
- `Result.fail(message)`

### 4.2 ErrorCode

文件路径：

- [ErrorCode.java](D:/workspace_idea/BookMall/BookMall/bookmall-common/src/main/java/com/bookmall/common/constant/ErrorCode.java)

当前错误码：

- `SUCCESS(200, "success")`
- `PARAM_ERROR(400, "参数错误")`
- `UNAUTHORIZED(401, "未授权")`
- `FORBIDDEN(403, "无权限")`
- `NOT_FOUND(404, "资源不存在")`
- `SYSTEM_ERROR(500, "系统异常")`

### 4.3 BusinessException

文件路径：

- [BusinessException.java](D:/workspace_idea/BookMall/BookMall/bookmall-common/src/main/java/com/bookmall/common/exception/BusinessException.java)

用途：

- 业务层主动抛出异常
- 例如：用户名已存在、密码错误、token 为空等

## 5. bookmall-gateway 模块说明

`bookmall-gateway` 是统一入口模块，负责将外部请求分发到后端各个微服务。

当前职责包括：

- 统一入口
- 路由转发
- 跨域配置
- 后续鉴权扩展预留

### 5.1 启动类

文件路径：

- [GatewayApplication.java](D:/workspace_idea/BookMall/BookMall/bookmall-gateway/src/main/java/com/bookmall/gateway/GatewayApplication.java)

当前注解：

- `@SpringBootApplication`

作用：

- 启动 Spring Boot 网关服务

### 5.2 配置文件

文件路径：

- [application.yml](D:/workspace_idea/BookMall/BookMall/bookmall-gateway/src/main/resources/application.yml)

当前配置包含：

- 服务端口：`8080`
- 服务名：`bookmall-gateway`
- 路由规则
- 跨域配置

## 6. 当前路由规则

### 6.1 转发到 auth 服务

路由配置：

- `Path=/api/auth/**`
- `uri=http://localhost:8060`
- `StripPrefix=1`

说明：

- 访问网关 `/api/auth/**` 会转发到 auth 服务
- 网关会去掉第一个路径前缀 `api`

例如：

- `GET http://localhost:8080/api/auth/hello`
- 实际转发到：`GET http://localhost:8060/auth/hello`

### 6.2 转发到 book 服务

路由配置：

- `Path=/api/books/**`
- `uri=http://localhost:8070`
- `StripPrefix=1`

说明：

- 访问网关 `/api/books/**` 会转发到 book 服务
- 网关会去掉第一个路径前缀 `api`

例如：

- `GET http://localhost:8080/api/books/hello`
- 实际转发到：`GET http://localhost:8070/books/hello`

## 7. 当前跨域配置

当前网关已配置全局跨域，支持：

- 所有来源
- 常见请求方法：GET、POST、PUT、DELETE、OPTIONS
- 所有请求头
- 允许携带凭证

这为前端页面后续接入提供了基础支持。

## 8. 依赖说明

文件路径：

- [bookmall-gateway/pom.xml](D:/workspace_idea/BookMall/BookMall/bookmall-gateway/pom.xml)

当前依赖包括：

- `bookmall-common`
- `spring-cloud-starter-gateway`
- `spring-boot-starter-actuator`

## 9. 当前已验证功能

当前已经验证通过的功能有：

- `GET http://localhost:8080/api/auth/hello` 可正确转发到 auth 服务
- `GET http://localhost:8080/api/books/hello` 可正确转发到 book 服务
- 网关端口可正常启动
- 路由配置生效

## 10. 当前测试方式

当前网关主要通过浏览器、Postman 或 Apifox 验证。

### 10.1 测试 auth 转发

请求：

```text
GET http://localhost:8080/api/auth/hello
```

### 10.2 测试 book 转发

请求：

```text
GET http://localhost:8080/api/books/hello
```

### 10.3 测试 auth 业务接口

例如：

```text
POST http://localhost:8080/api/auth/register
POST http://localhost:8080/api/auth/login
GET  http://localhost:8080/api/auth/me
```

### 10.4 测试 book 业务接口

例如：

```text
GET    http://localhost:8080/api/books
GET    http://localhost:8080/api/books/1
POST   http://localhost:8080/api/books
PUT    http://localhost:8080/api/books/1
DELETE http://localhost:8080/api/books/1
```

## 11. 当前模块状态总结

`bookmall-gateway` 已经完成了第一版统一入口和路由转发能力，属于当前项目中的第三个完成模块。

现在它已经具备：

- 可启动
- 可转发 auth 请求
- 可转发 book 请求
- 已配置跨域

## 12. 当前局限

当前网关还是最小可用版本，尚未接入以下能力：

- Nacos 注册中心
- Nacos 配置中心
- JWT 统一鉴权
- 请求拦截与白名单
- 限流
- 熔断降级
- 日志链路追踪

## 13. 后续开发计划

下一步建议开发：

- `bookmall-cart`：购物车
- `bookmall-order`：订单
- `bookmall-inventory`：库存
- 网关统一鉴权
- 网关接入 Nacos

## 14. 维护规则

以后每完成一个模块，都要同步输出一份对应的说明文档，建议命名格式如下：

- `bookmall-auth-说明文档.md`
- `bookmall-book-说明文档.md`
- `bookmall-gateway-说明文档.md`

文档建议包含：

- 模块职责
- 配置说明
- 文件结构
- 路由/接口列表
- 测试方式
- 当前状态
- 下一步计划
