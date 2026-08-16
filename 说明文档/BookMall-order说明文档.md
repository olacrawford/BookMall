# BookMall Order 模块说明文档

## 1. 项目概述

`bookmall-order` 是 BookMall 项目中的订单服务模块，负责订单创建、订单查询、订单取消等能力。

当前这个模块已经完成了第一版可用闭环，包含：

- 订单创建
- 订单列表查询
- 订单详情查询
- 订单取消
- 创建订单时联动库存扣减
- 取消订单时联动库存恢复
- 订单明细落库
- 从购物车生成订单
- 关联收货地址信息

本文档用于记录当前 `order` 模块的实现状态、配置、接口、数据库映射和验证方式。

## 2. 当前项目结构

当前根工程为 Maven 聚合工程，模块如下：

- `bookmall-common`
- `bookmall-auth`
- `bookmall-book`
- `bookmall-gateway`
- `bookmall-cart`
- `bookmall-order`
- `bookmall-inventory`
- `bookmall-address`

当前项目目录中的关键部分：

- [pom.xml](D:/workspace_idea/BookMall/BookMall/pom.xml)
- [bookmall-common](D:/workspace_idea/BookMall/BookMall/bookmall-common)
- [bookmall-auth](D:/workspace_idea/BookMall/BookMall/bookmall-auth)
- [bookmall-book](D:/workspace_idea/BookMall/BookMall/bookmall-book)
- [bookmall-gateway](D:/workspace_idea/BookMall/BookMall/bookmall-gateway)
- [bookmall-cart](D:/workspace_idea/BookMall/BookMall/bookmall-cart)
- [bookmall-order](D:/workspace_idea/BookMall/BookMall/bookmall-order)
- [bookmall-inventory](D:/workspace_idea/BookMall/BookMall/bookmall-inventory)
- [bookmall-address](D:/workspace_idea/BookMall/BookMall/bookmall-address)

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
  - `bookmall-address`

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

## 5. bookmall-order 模块说明

`bookmall-order` 是订单服务模块，负责订单生成与订单生命周期管理。

当前职责包括：

- 从购物车创建订单
- 查询用户订单列表
- 查询订单详情
- 取消订单
- 调用库存服务扣减/恢复库存
- 调用图书服务获取图书快照
- 调用地址服务获取收货地址快照

### 5.1 启动类

文件路径：

- [OrderApplication.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/OrderApplication.java)

当前注解：

- `@SpringBootApplication`
- `@MapperScan("com.bookmall.order.mapper")`

作用：

- 启动 Spring Boot 服务
- 扫描 MyBatis-Plus Mapper 接口

### 5.2 配置文件

文件路径：

- [application.yml](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/resources/application.yml)

当前配置包含：

- 服务端口：`8084`
- 服务名：`bookmall-order`
- 数据源：连接本地 MySQL 的 `bookmall` 数据库
- MyBatis-Plus 配置
- 下游服务地址配置

当前 MySQL 连接信息：

- Host: `localhost`
- Port: `3306`
- Database: `bookmall`
- Username: `root`
- Password: `root`

下游服务不再配置固定 URL。订单服务使用 OpenFeign 和 Nacos 服务名调用 `bookmall-book`、`bookmall-inventory`、`bookmall-address`。

## 6. 依赖说明

文件路径：

- [bookmall-order/pom.xml](D:/workspace_idea/BookMall/BookMall/bookmall-order/pom.xml)

当前依赖包括：

- `bookmall-common`
- `spring-boot-starter-web`
- `spring-cloud-starter-openfeign`
- `spring-cloud-starter-alibaba-nacos-discovery`
- `spring-boot-starter-validation`
- `mybatis-plus-spring-boot3-starter`
- `mysql-connector-j`
- `lombok`
- `bookmall-cart`
- `bookmall-book`

## 7. 当前接口说明

当前 `order` 模块的接口前缀是：

- `/orders`

### 7.1 GET /orders/hello

用途：

- 服务健康检查
- 验证服务是否启动成功

当前返回：

- `bookmall-order is running`

### 7.2 POST /orders

用途：

- 创建订单

请求对象：

- `OrderCreateRequest`

请求字段：

- `userId`
- `addressId`
- `cartItemIds`

功能说明：

- 根据用户和购物车项查询购物车数据
- 查询地址信息
- 调用库存服务扣减库存
- 调用图书服务获取图书信息
- 计算订单总金额
- 插入订单主表
- 插入订单明细表
- 删除对应购物车项

### 7.3 GET /orders

用途：

- 查询用户订单列表

请求参数：

- `userId`

功能说明：

- 根据用户 ID 查询订单列表
- 按创建时间倒序排列

### 7.4 GET /orders/{id}

用途：

- 查询订单详情

功能说明：

- 查询订单主表
- 查询订单明细表
- 组合成完整订单详情返回

### 7.5 PUT /orders/{id}/cancel

用途：

- 取消订单

功能说明：

- 查询订单及明细
- 调用库存服务恢复库存
- 更新订单状态为已取消

## 8. 业务类结构说明

### 8.1 Entity

#### Order

文件路径：

- [Order.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/entity/Order.java)

映射表：

- `t_order`

字段：

- `id`
- `orderNo`
- `userId`
- `totalAmount`
- `status`
- `payStatus`
- `receiverName`
- `receiverPhone`
- `receiverAddress`
- `createTime`
- `updateTime`

#### OrderItem

文件路径：

- [OrderItem.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/entity/OrderItem.java)

映射表：

- `t_order_item`

字段：

- `id`
- `orderId`
- `bookId`
- `bookTitle`
- `bookPrice`
- `quantity`
- `subtotal`
- `createTime`

### 8.2 Mapper

#### OrderMapper

文件路径：

- [OrderMapper.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/mapper/OrderMapper.java)

#### OrderItemMapper

文件路径：

- [OrderItemMapper.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/mapper/OrderItemMapper.java)

#### CartMapper

文件路径：

- [CartMapper.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/mapper/CartMapper.java)

说明：

- `OrderMapper` 和 `OrderItemMapper` 分别操作订单主表和订单明细表
- `CartMapper` 在订单模块中用于读取购物车数据

### 8.3 DTO

#### OrderCreateRequest

文件路径：

- [OrderCreateRequest.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/dto/OrderCreateRequest.java)

#### InventoryDeductRequest

文件路径：

- [InventoryDeductRequest.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/dto/InventoryDeductRequest.java)

#### InventoryRecoverRequest

文件路径：

- [InventoryRecoverRequest.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/dto/InventoryRecoverRequest.java)

### 8.4 VO

#### OrderVO

文件路径：

- [OrderVO.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/vo/OrderVO.java)

#### OrderDetailVO

文件路径：

- [OrderDetailVO.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/vo/OrderDetailVO.java)

### 8.5 Service

#### OrderService

文件路径：

- [OrderService.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/service/OrderService.java)

当前方法：

- `createOrder(OrderCreateRequest request)`
- `listOrdersByUserId(Long userId)`
- `getOrderDetail(Long id)`
- `cancelOrder(Long id)`

### 8.6 Service 实现

#### OrderServiceImpl

文件路径：

- [OrderServiceImpl.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/service/impl/OrderServiceImpl.java)

职责：

- 汇总购物车数据
- 生成订单号
- 创建订单主表和订单明细表
- 联动库存扣减与恢复
- 删除已下单购物车项

### 8.7 Controller

#### OrderController

文件路径：

- [OrderController.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/controller/OrderController.java)

职责：

- 暴露订单相关 HTTP 接口

### 8.8 外部调用客户端

#### BookClient

文件路径：

- [BookClient.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/client/BookClient.java)

#### InventoryClient

文件路径：

- [InventoryClient.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/client/InventoryClient.java)

#### AddressClient

文件路径：

- [AddressClient.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/client/AddressClient.java)

#### AddressSnapshot

文件路径：

- [AddressSnapshot.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/client/dto/AddressSnapshot.java)

说明：

- 这些客户端使用 `OpenFeign` 调用其他微服务
- `@FeignClient` 的 `name` 对应 Nacos 中的服务名
- 不再手工拼接 localhost 和固定端口
- `BookSnapshot`、`AddressSnapshot` 是订单服务自己的远程响应 DTO，避免直接依赖下游服务内部 VO

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

其中 `order` 模块当前主要使用：

- `t_order`
- `t_order_item`
- `t_cart`
- `t_book`
- `t_address`
- `t_inventory`

## 10. 已验证功能

当前已经验证通过的功能有：

- `GET /orders/hello` 可访问
- `POST /orders` 可创建订单
- `GET /orders?userId=...` 可查询订单列表
- `GET /orders/{id}` 可查询订单详情
- `PUT /orders/{id}/cancel` 可取消订单

## 11. 当前测试方式

当前功能主要通过以下方式验证：

- 浏览器访问 `GET /orders/hello`
- Postman / Apifox 测试订单接口
- IDEA HTTP Client 测试接口
- MySQL 中查看 `t_order`、`t_order_item`、`t_cart` 表数据

### 11.1 创建订单测试

请求示例：

```json
{
  "userId": 3,
  "addressId": 1,
  "cartItemIds": [1, 2, 3]
}
```

### 11.2 查询订单列表测试

请求示例：

```text
GET /orders?userId=3
```

### 11.3 查询订单详情测试

请求示例：

```text
GET /orders/1
```

### 11.4 取消订单测试

请求示例：

```text
PUT /orders/1/cancel
```

## 12. 当前模块状态总结

`bookmall-order` 已经完成了订单服务的第一版可用闭环，属于当前项目中的第六个完成模块。

现在它已经具备：

- 可运行
- 可创建订单
- 可查询订单
- 可取消订单
- 可联动库存与购物车

## 13. 当前改进点

当前订单模块已经补齐了以下关键点：

- 下游调用已经由 RestTemplate 改为 OpenFeign
- 下游实例通过 Nacos 服务发现，不使用固定端口
- 下单过程增加事务控制
- 删除购物车项时只删除实际参与下单的项
- 取消订单增加状态判断，避免重复取消
- 网关路由可以继续转发到订单服务

## 14. 后续开发计划

下一步建议开发：

- 订单支付状态流转
- 订单完成逻辑
- 订单取消的更细粒度状态控制
- 统一鉴权接入
- 分布式事务优化

## 15. 维护规则

以后每完成一个模块，都要同步输出一份对应的说明文档，建议命名格式如下：

- `bookmall-order-说明文档.md`

文档建议包含：

- 模块职责
- 配置说明
- 文件结构
- 接口列表
- 数据模型
- 测试方式
- 当前状态
- 下一步计划
