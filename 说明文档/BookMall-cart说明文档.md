# BookMall Cart 模块说明文档

## 1. 项目概述

`bookmall-cart` 是 BookMall 项目中的购物车服务模块，负责用户购物车相关能力。

当前这个模块已经完成了第一版可用闭环，包含：

- 加入购物车
- 查看购物车
- 修改购物车数量
- 删除购物车商品

该文档用于记录当前 `cart` 模块的实现状态、配置、接口、数据库映射和验证方式。

## 2. 当前项目结构

当前根工程为 Maven 聚合工程，模块如下：

- `bookmall-common`
- `bookmall-auth`
- `bookmall-book`
- `bookmall-gateway`
- `bookmall-cart`

当前项目目录中的关键部分：

- [pom.xml](D:/workspace_idea/BookMall/BookMall/pom.xml)
- [bookmall-common](D:/workspace_idea/BookMall/BookMall/bookmall-common)
- [bookmall-auth](D:/workspace_idea/BookMall/BookMall/bookmall-auth)
- [bookmall-book](D:/workspace_idea/BookMall/BookMall/bookmall-book)
- [bookmall-gateway](D:/workspace_idea/BookMall/BookMall/bookmall-gateway)
- [bookmall-cart](D:/workspace_idea/BookMall/BookMall/bookmall-cart)

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

## 5. bookmall-cart 模块说明

`bookmall-cart` 是购物车服务模块，负责用户购物车相关能力。

当前职责包括：

- 加入购物车
- 查看购物车
- 修改购物车数量
- 删除购物车商品

### 5.1 启动类

文件路径：

- [CartApplication.java](D:/workspace_idea/BookMall/BookMall/bookmall-cart/src/main/java/com/bookmall/cart/CartApplication.java)

当前注解：

- `@SpringBootApplication`
- `@MapperScan("com.bookmall.cart.mapper")`

作用：

- 启动 Spring Boot 服务
- 扫描 MyBatis-Plus Mapper 接口

### 5.2 配置文件

文件路径：

- [application.yml](D:/workspace_idea/BookMall/BookMall/bookmall-cart/src/main/resources/application.yml)

当前配置包含：

- 服务端口：`8083`
- 服务名：`bookmall-cart`
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

- [bookmall-cart/pom.xml](D:/workspace_idea/BookMall/BookMall/bookmall-cart/pom.xml)

当前依赖包括：

- `bookmall-common`
- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `mybatis-plus-spring-boot3-starter`
- `mysql-connector-j`
- `lombok`

## 7. 当前接口说明

当前 `cart` 模块的接口前缀是：

- `/cart`

### 7.1 GET /cart/hello

用途：

- 服务健康检查
- 验证服务是否启动成功

当前返回：

- `bookmall-cart is running`

### 7.2 GET /cart

用途：

- 查询用户购物车

请求参数：

- `userId`

功能说明：

- 按用户 ID 查询购物车项
- 按更新时间倒序返回

### 7.3 POST /cart/items

用途：

- 加入购物车

请求字段：

- `userId`
- `bookId`
- `quantity`

功能说明：

- 如果用户购物车里已有相同图书，则数量累加
- 如果没有，则新增一条购物车记录

### 7.4 PUT /cart/items/{id}

用途：

- 修改购物车数量

请求字段：

- `quantity`

功能说明：

- 根据购物车项 id 修改数量
- 如果购物车项不存在，返回 `404 购物车项不存在`

### 7.5 DELETE /cart/items/{id}

用途：

- 删除购物车商品

功能说明：

- 根据购物车项 id 删除记录
- 如果购物车项不存在，返回 `404 购物车项不存在`

## 8. 业务类结构说明

### 8.1 Entity

#### Cart

文件路径：

- [Cart.java](D:/workspace_idea/BookMall/BookMall/bookmall-cart/src/main/java/com/bookmall/cart/entity/Cart.java)

映射表：

- `t_cart`

字段：

- `id`
- `userId`
- `bookId`
- `quantity`
- `selected`
- `createTime`
- `updateTime`

### 8.2 Mapper

#### CartMapper

文件路径：

- [CartMapper.java](D:/workspace_idea/BookMall/BookMall/bookmall-cart/src/main/java/com/bookmall/cart/mapper/CartMapper.java)

说明：

- 继承 `BaseMapper<Cart>`
- 使用 MyBatis-Plus 提供的基础 CRUD 能力

### 8.3 DTO

#### CartAddRequest

文件路径：

- [CartAddRequest.java](D:/workspace_idea/BookMall/BookMall/bookmall-cart/src/main/java/com/bookmall/cart/dto/CartAddRequest.java)

#### CartUpdateRequest

文件路径：

- [CartUpdateRequest.java](D:/workspace_idea/BookMall/BookMall/bookmall-cart/src/main/java/com/bookmall/cart/dto/CartUpdateRequest.java)

### 8.4 Service

#### CartService

文件路径：

- [CartService.java](D:/workspace_idea/BookMall/BookMall/bookmall-cart/src/main/java/com/bookmall/cart/service/CartService.java)

当前方法：

- `listCartByUserId(Long userId)`
- `addToCart(CartAddRequest request)`
- `updateQuantity(Long id, Integer quantity)`
- `deleteItem(Long id)`

### 8.5 Service 实现

#### CartServiceImpl

文件路径：

- [CartServiceImpl.java](D:/workspace_idea/BookMall/BookMall/bookmall-cart/src/main/java/com/bookmall/cart/service/impl/CartServiceImpl.java)

职责：

- 查询购物车
- 加入购物车
- 修改购物车数量
- 删除购物车项

### 8.6 Controller

#### CartController

文件路径：

- [CartController.java](D:/workspace_idea/BookMall/BookMall/bookmall-cart/src/main/java/com/bookmall/cart/controller/CartController.java)

职责：

- 暴露购物车相关 HTTP 接口

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

其中 `cart` 模块当前主要使用：

- `t_cart`

## 10. 已验证功能

当前已经验证通过的功能有：

- `GET /cart/hello` 可访问
- `GET /cart?userId=...` 可查询购物车
- `POST /cart/items` 可加入购物车
- `PUT /cart/items/{id}` 可修改购物车数量
- `DELETE /cart/items/{id}` 可删除购物车项

## 11. 当前测试方式

当前功能主要通过以下方式验证：

- 浏览器访问 `GET /cart/hello`
- Postman / Apifox 测试 `GET`、`POST`、`PUT`、`DELETE`
- IDEA HTTP Client 测试接口
- MySQL 中查看 `t_cart` 表数据

### 11.1 加入购物车测试

请求示例：

```json
{
  "userId": 3,
  "bookId": 1,
  "quantity": 2
}
```

### 11.2 修改数量测试

请求示例：

```json
{
  "quantity": 5
}
```

### 11.3 查询购物车测试

请求示例：

```text
GET /cart?userId=3
```

## 12. 当前模块状态总结

`bookmall-cart` 已经完成了购物车服务的第一版可用闭环，属于当前项目中的第四个完成模块。

现在它已经具备：

- 可运行
- 可查询
- 可新增
- 可修改
- 可删除

## 13. 后续开发计划

下一步建议开发：

- `bookmall-order`：订单
- `bookmall-inventory`：库存
- 登录态与购物车/订单联动
- 网关统一鉴权完善

## 14. 维护规则

以后每完成一个模块，都要同步输出一份对应的说明文档，建议命名格式如下：

- `bookmall-auth-说明文档.md`
- `bookmall-book-说明文档.md`
- `bookmall-gateway-说明文档.md`
- `bookmall-cart-说明文档.md`

文档建议包含：

- 模块职责
- 配置说明
- 文件结构
- 接口列表
- 数据模型
- 测试方式
- 当前状态
- 下一步计划
