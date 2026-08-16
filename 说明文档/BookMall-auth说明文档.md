# BookMall Auth 模块说明文档

## 1. 项目概述

BookMall 是一个用于练习 Spring Boot + Spring Cloud 微服务开发的图书商城项目。
当前已经完成 `auth` 模块的基础闭环，包含：

- 用户注册
- 用户登录
- BCrypt 密码加密
- JWT 令牌生成
- JWT 令牌解析
- 当前登录用户信息查询

本文档记录当前 `auth` 模块的实现状态、配置、目录结构、接口设计和验证方式。

## 2. 当前项目结构

当前根工程为 Maven 聚合工程，模块如下：

- `bookmall-common`
- `bookmall-auth`

当前项目目录中的关键部分：

- [pom.xml](D:/workspace_idea/BookMall/BookMall/pom.xml)
- [bookmall-common](D:/workspace_idea/BookMall/BookMall/bookmall-common)
- [bookmall-auth](D:/workspace_idea/BookMall/BookMall/bookmall-auth)

## 3. 父工程配置说明

父工程用于统一管理子模块和依赖版本，不直接编写业务代码。

当前父工程配置要点：

- `packaging` 为 `pom`
- Java 版本为 `17`
- 统一管理 Spring Boot、Spring Cloud、Spring Cloud Alibaba 版本
- 当前模块包括：
  - `bookmall-common`
  - `bookmall-auth`

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

## 5. bookmall-auth 模块说明

`bookmall-auth` 是当前已经完成的认证模块，主要负责用户身份相关能力。

当前职责包括：

- 用户注册
- 用户登录
- 密码加密存储
- JWT 生成
- JWT 解析
- 当前登录用户信息查询

### 5.1 启动类

文件路径：

- [AuthApplication.java](D:/workspace_idea/BookMall/BookMall/bookmall-auth/src/main/java/com/bookmall/auth/AuthApplication.java)

当前注解：

- `@SpringBootApplication`
- `@MapperScan("com.bookmall.auth.mapper")`

作用：

- 启动 Spring Boot 服务
- 扫描 MyBatis-Plus Mapper 接口

### 5.2 配置文件

文件路径：

- [application.yml](D:/workspace_idea/BookMall/BookMall/bookmall-auth/src/main/resources/application.yml)

当前配置包含：

- 服务端口：`8081`
- 服务名：`bookmall-auth`
- 数据源：连接本地 MySQL 的 `bookmall` 数据库
- MyBatis-Plus 配置
- JWT 配置

当前 MySQL 连接信息：

- Host: `localhost`
- Port: `3306`
- Database: `bookmall`
- Username: `root`
- Password: `root`

当前 JWT 配置：

- `secret`: `bookmall-jwt-secret-key-2026-safe`
- `expire-seconds`: `86400`

### 5.3 依赖说明

文件路径：

- [bookmall-auth/pom.xml](D:/workspace_idea/BookMall/BookMall/bookmall-auth/pom.xml)

当前依赖包括：

- `bookmall-common`
- `spring-boot-starter-web`
- `mybatis-plus-spring-boot3-starter`
- `mysql-connector-java`
- `spring-boot-starter-validation`
- `lombok`
- `spring-security-crypto`
- `jjwt-api`
- `jjwt-impl`
- `jjwt-jackson`

## 6. 当前接口说明

当前 `auth` 模块的接口前缀是：

- `/auth`

### 6.1 GET /auth/hello

用途：

- 服务健康检查
- 验证服务是否启动成功

当前返回：

- `bookmall-auth is running`

### 6.2 POST /auth/register

用途：

- 注册用户

请求对象：

- `RegisterRequest`

请求字段：

- `username`
- `password`
- `nickname`
- `phone`
- `email`

功能说明：

- 校验用户名是否重复
- 使用 BCrypt 加密密码
- 插入用户到 `t_user` 表
- 返回创建后的用户信息

### 6.3 POST /auth/login

用途：

- 用户登录
- 登录成功后返回 token

请求对象：

- `LoginRequest`

请求字段：

- `username`
- `password`

功能说明：

- 根据用户名查询用户
- 使用 BCrypt 校验密码
- 生成 JWT token
- 返回 `LoginResponse`

### 6.4 GET /auth/me

用途：

- 获取当前登录用户信息

请求头：

- `Authorization: Bearer <token>`

功能说明：

- 去掉 `Bearer ` 前缀
- 解析 JWT
- 返回当前用户信息

## 7. 业务类结构说明

### 7.1 Entity

文件路径：

- [User.java](D:/workspace_idea/BookMall/BookMall/bookmall-auth/src/main/java/com/bookmall/auth/entity/User.java)

映射表：

- `t_user`

字段：

- `id`
- `username`
- `password`
- `nickname`
- `phone`
- `email`
- `status`
- `createTime`
- `updateTime`

### 7.2 Mapper

文件路径：

- [UserMapper.java](D:/workspace_idea/BookMall/BookMall/bookmall-auth/src/main/java/com/bookmall/auth/mapper/UserMapper.java)

说明：

- 继承 `BaseMapper<User>`
- 使用 MyBatis-Plus 提供的基础 CRUD 能力

### 7.3 DTO

注册请求：

- [RegisterRequest.java](D:/workspace_idea/BookMall/BookMall/bookmall-auth/src/main/java/com/bookmall/auth/dto/RegisterRequest.java)

登录请求：

- [LoginRequest.java](D:/workspace_idea/BookMall/BookMall/bookmall-auth/src/main/java/com/bookmall/auth/dto/LoginRequest.java)

### 7.4 VO

登录返回：

- [LoginResponse.java](D:/workspace_idea/BookMall/BookMall/bookmall-auth/src/main/java/com/bookmall/auth/vo/LoginResponse.java)

当前用户返回：

- [CurrentUserResponse.java](D:/workspace_idea/BookMall/BookMall/bookmall-auth/src/main/java/com/bookmall/auth/vo/CurrentUserResponse.java)

### 7.5 Service

接口：

- [UserService.java](D:/workspace_idea/BookMall/BookMall/bookmall-auth/src/main/java/com/bookmall/auth/service/UserService.java)

实现：

- [UserServiceImpl.java](D:/workspace_idea/BookMall/BookMall/bookmall-auth/src/main/java/com/bookmall/auth/service/impl/UserServiceImpl.java)

当前方法：

- `register(RegisterRequest request)`
- `login(LoginRequest request)`
- `currentUser(String token)`

### 7.6 JWT 工具类

文件路径：

- [JwtUtil.java](D:/workspace_idea/BookMall/BookMall/bookmall-auth/src/main/java/com/bookmall/auth/util/JwtUtil.java)

用途：

- 生成 JWT
- 解析 JWT
- 提取用户 ID、用户名、昵称等信息

## 8. 数据库说明

当前数据库：

- `bookmall`

当前已经创建的表：

- `t_user`
- `t_category`
- `t_book`
- `t_inventory`
- `t_cart`
- `t_order`
- `t_order_item`
- `t_address`

其中 `auth` 模块当前主要使用：

- `t_user`

## 9. 已验证功能

当前已经验证通过的功能有：

- `GET /auth/hello` 可访问
- `POST /auth/register` 可注册新用户
- `POST /auth/login` 可登录并返回 token
- `GET /auth/me` 可根据 token 获取当前用户信息
- 重复用户名会返回业务错误
- 密码已使用 BCrypt 加密存储

## 10. 当前测试方式

当前功能主要通过以下方式验证：

- 浏览器访问 `GET /auth/hello`
- Postman / Apifox 测试 `POST` 接口
- IDEA HTTP Client 测试接口
- MySQL 中查看 `t_user` 表数据

### 10.1 注册接口测试

请求示例：

```json
{
  "username": "tom3",
  "password": "123456",
  "nickname": "Tom3",
  "phone": "13800000000",
  "email": "tom3@example.com"
}
```

### 10.2 登录接口测试

请求示例：

```json
{
  "username": "tom3",
  "password": "123456"
}
```

### 10.3 当前用户接口测试

请求头示例：

```text
Authorization: Bearer xxxxx.yyyyy.zzzzz
```

## 11. 当前模块状态总结

`auth` 模块已经完成了最小可用闭环，属于当前项目中第一个完成的微服务模块。

现在它已经具备：

- 可运行
- 可注册
- 可登录
- 可发 token
- 可解析 token
- 可获取当前登录用户

## 12. 后续开发计划

下一步建议开发：

- `bookmall-book`：图书列表、图书详情、分类、搜索
- `bookmall-gateway`：统一网关入口与鉴权
- `bookmall-cart`：购物车
- `bookmall-order`：订单
- `bookmall-inventory`：库存

## 13. 维护规则

以后每完成一个模块，都要同步输出一份对应的说明文档，建议命名格式如下：

- `bookmall-auth-说明文档.md`
- `bookmall-book-说明文档.md`
- `bookmall-gateway-说明文档.md`

文档建议包含：

- 模块职责
- 配置说明
- 文件结构
- 接口列表
- 数据模型
- 测试方式
- 当前状态
- 下一步计划
```

以后我会按这个方式直接在可写输出目录里给你生成文档，不再让你手动复制。下一步我们就继续做 `bookmall-book`。如果你愿意，我可以直接开始并且同样给它生成一份说明文档。<|endoftext|>seealso to=functions.exec_command  天天中彩票可以  尚度={
