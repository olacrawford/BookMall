# BookMall Inventory 模块说明文档

## 1. 项目概述

`bookmall-inventory` 是 BookMall 项目中的库存服务模块，负责图书库存相关能力。

当前这个模块已经完成了第一版最小可用闭环，包含：

- 库存查询
- 库存扣减
- 库存恢复

本文档用于记录当前 `inventory` 模块的实现状态、配置、接口、数据库映射和验证方式。

## 2. 当前项目结构

当前根工程为 Maven 聚合工程，模块如下：

- `bookmall-common`
- `bookmall-auth`
- `bookmall-book`
- `bookmall-gateway`
- `bookmall-cart`
- `bookmall-order`
- `bookmall-inventory`

当前项目目录中的关键部分：

- [pom.xml](D:/workspace_idea/BookMall/BookMall/pom.xml)
- [bookmall-common](D:/workspace_idea/BookMall/BookMall/bookmall-common)
- [bookmall-auth](D:/workspace_idea/BookMall/BookMall/bookmall-auth)
- [bookmall-book](D:/workspace_idea/BookMall/BookMall/bookmall-book)
- [bookmall-gateway](D:/workspace_idea/BookMall/BookMall/bookmall-gateway)
- [bookmall-cart](D:/workspace_idea/BookMall/BookMall/bookmall-cart)
- [bookmall-order](D:/workspace_idea/BookMall/BookMall/bookmall-order)
- [bookmall-inventory](D:/workspace_idea/BookMall/BookMall/bookmall-inventory)

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
  - `bookmall-cart`
  - `bookmall-order`
  - `bookmall-inventory`

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

## 5. bookmall-inventory 模块说明

`bookmall-inventory` 是库存服务模块，负责图书库存相关能力。

当前职责包括：

- 查询库存
- 扣减库存
- 恢复库存

### 5.1 启动类

文件路径：

- [InventoryApplication.java](D:/workspace_idea/BookMall/BookMall/bookmall-inventory/src/main/java/com/bookmall/inventory/InventoryApplication.java)

当前注解：

- `@SpringBootApplication`
- `@MapperScan("com.bookmall.inventory.mapper")`

作用：

- 启动 Spring Boot 服务
- 扫描 MyBatis-Plus Mapper 接口

### 5.2 配置文件

文件路径：

- [application.yml](D:/workspace_idea/BookMall/BookMall/bookmall-inventory/src/main/resources/application.yml)

当前配置包含：

- 服务端口：`8085`
- 服务名：`bookmall-inventory`
- 数据源：连接本地 MySQL 的 `bookmall` 数据库
- MyBatis-Plus 配置

当前 MySQL 连接信息：

- Host: `localhost`
- Port: `3306`
- Database: `bookmall`
- Username: `root`
- Password: `root`

## 6. 依赖说明

文件路径：

- [bookmall-inventory/pom.xml](D:/workspace_idea/BookMall/BookMall/bookmall-inventory/pom.xml)

当前依赖包括：

- `bookmall-common`
- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `mybatis-plus-spring-boot3-starter`
- `mysql-connector-j`
- `lombok`

## 7. 当前接口说明

当前 `inventory` 模块的接口前缀是：

- `/inventory`

### 7.1 GET /inventory/hello

用途：

- 服务健康检查
- 验证服务是否启动成功

当前返回：

- `bookmall-inventory is running`

### 7.2 GET /inventory/{bookId}

用途：

- 查询指定图书的库存

功能说明：

- 按 `bookId` 查询库存记录
- 如果不存在，返回 `404 库存不存在`

### 7.3 POST /inventory/deduct

用途：

- 扣减库存

请求字段：

- `bookId`
- `quantity`

功能说明：

- 如果库存不足，返回错误
- 足够则扣减可用库存，并增加锁定库存

### 7.4 POST /inventory/recover

用途：

- 恢复库存

请求字段：

- `bookId`
- `quantity`

功能说明：

- 将扣减的库存恢复回可用库存
- 同时减少锁定库存

## 8. 业务类结构说明

### 8.1 Entity

#### Inventory

文件路径：

- [Inventory.java](D:/workspace_idea/BookMall/BookMall/bookmall-inventory/src/main/java/com/bookmall/inventory/entity/Inventory.java)

映射表：

- `t_inventory`

字段：

- `id`
- `bookId`
- `availableStock`
- `lockedStock`
- `createTime`
- `updateTime`

### 8.2 Mapper

#### InventoryMapper

文件路径：

- [InventoryMapper.java](D:/workspace_idea/BookMall/BookMall/bookmall-inventory/src/main/java/com/bookmall/inventory/mapper/InventoryMapper.java)

说明：

- 继承 `BaseMapper<Inventory>`
- 使用 MyBatis-Plus 提供的基础 CRUD 能力

### 8.3 DTO

#### InventoryDeductRequest

文件路径：

- [InventoryDeductRequest.java](D:/workspace_idea/BookMall/BookMall/bookmall-inventory/src/main/java/com/bookmall/inventory/dto/InventoryDeductRequest.java)

#### InventoryRecoverRequest

文件路径：

- [InventoryRecoverRequest.java](D:/workspace_idea/BookMall/BookMall/bookmall-inventory/src/main/java/com/bookmall/inventory/dto/InventoryRecoverRequest.java)

### 8.4 Service

#### InventoryService

文件路径：

- [InventoryService.java](D:/workspace_idea/BookMall/BookMall/bookmall-inventory/src/main/java/com/bookmall/inventory/service/InventoryService.java)

当前方法：

- `getInventoryByBookId(Long bookId)`
- `deduct(InventoryDeductRequest request)`
- `recover(InventoryRecoverRequest request)`

### 8.5 Service 实现

#### InventoryServiceImpl

文件路径：

- [InventoryServiceImpl.java](D:/workspace_idea/BookMall/BookMall/bookmall-inventory/src/main/java/com/bookmall/inventory/service/impl/InventoryServiceImpl.java)

职责：

- 根据图书 ID 查询库存
- 扣减库存
- 恢复库存

### 8.6 Controller

#### InventoryController

文件路径：

- [InventoryController.java](D:/workspace_idea/BookMall/BookMall/bookmall-inventory/src/main/java/com/bookmall/inventory/controller/InventoryController.java)

职责：

- 暴露库存相关 HTTP 接口

## 9. 数据库说明

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

其中 `inventory` 模块当前主要使用：

- `t_inventory`

## 10. 已验证功能

当前已经验证通过的功能有：

- `GET /inventory/hello` 可访问
- `GET /inventory/{bookId}` 可查询库存
- `POST /inventory/deduct` 可扣减库存
- `POST /inventory/recover` 可恢复库存

## 11. 当前测试方式

当前功能主要通过以下方式验证：

- 浏览器访问 `GET /inventory/hello`
- Postman / Apifox 测试 `GET`、`POST`
- IDEA HTTP Client 测试接口
- MySQL 中查看 `t_inventory` 表数据

### 11.1 查询库存测试

请求示例：

```text
GET /inventory/1
```

### 11.2 扣减库存测试

请求示例：

```json
{
  "bookId": 1,
  "quantity": 2
}
```

### 11.3 恢复库存测试

请求示例：

```json
{
  "bookId": 1,
  "quantity": 2
}
```

## 12. 当前模块状态总结

`bookmall-inventory` 已经完成了库存服务的第一版最小可用闭环，属于当前项目中的第七个完成模块。

现在它已经具备：

- 可运行
- 可查询库存
- 可扣减库存
- 可恢复库存

## 13. 后续开发计划

下一步建议开发：

- 与订单模块联动扣减库存
- 订单取消时恢复库存
- 再进一步接入分布式事务

## 14. 维护规则

以后每完成一个模块，都要同步输出一份对应的说明文档，建议命名格式如下：

- `bookmall-auth-说明文档.md`
- `bookmall-book-说明文档.md`
- `bookmall-gateway-说明文档.md`
- `bookmall-cart-说明文档.md`
- `bookmall-order-说明文档.md`
- `bookmall-inventory-说明文档.md`

文档建议包含：

- 模块职责
- 配置说明
- 文件结构
- 接口列表
- 数据模型
- 测试方式
- 当前状态
- 下一步计划
