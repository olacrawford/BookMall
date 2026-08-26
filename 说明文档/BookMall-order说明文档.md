# BookMall Order 模块说明文档

## 1. 当前职责

`bookmall-order` 是订单服务模块，负责用户直接下单、购物车下单和订单管理。

当前已实现：

- 根据 `X-User-Id` 创建订单，不信任前端传入的用户 ID
- 通过 OpenFeign 调用图书服务，快照图书标题、价格
- 通过 OpenFeign 读取购物车已选条目，一次创建多条订单明细
- 创建订单主表和订单明细
- 查询当前用户订单列表
- 查询订单详情（只能查看自己的订单）
- 取消待支付订单（只能取消自己的订单）

## 2. 当前项目结构

启动类：

- [OrderApplication.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/OrderApplication.java)

业务代码：

- `controller`：`OrderController`
- `service`：`OrderService`
- `service.impl`：`OrderServiceImpl`
- `mapper`：`OrderMapper`、`OrderItemMapper`
- `entity`：`Order`、`OrderItem`
- `dto`：`OrderCreateRequest`、`OrderFromCartRequest`
- `vo`：`OrderVO`、`OrderDetailVO`
- `client`：`BookClient`、`CartClient`
- `client.dto`：`BookSnapshot`、`CartItemSnapshot`

## 3. 配置说明

服务配置：

- 端口：`8050`
- 服务名：`order`
- Nacos 注册中心：`localhost:8848`
- Nacos Config：`order.yaml`
- MySQL：`localhost:3306/bookmall`

数据库连接和 JWT 配置在 [nacos-config/order.yaml](D:/workspace_idea/BookMall/nacos-config/order.yaml) 中维护。

## 4. 当前接口

接口前缀：`/orders`

### 4.1 GET /orders/hello

健康检查，返回 `bookmall-order is running`。

### 4.2 POST /orders

创建订单。请求头必须携带 `X-User-Id`，该值由网关解析 JWT 后透传。

请求体：

```json
{
  "bookId": 1,
  "quantity": 2,
  "receiverName": "张三",
  "receiverPhone": "13800000000",
  "receiverAddress": "上海市 浦东新区 测试路 1 号"
}
```

### 4.3 POST /orders/from-cart

从购物车下单。请求头必须携带 `X-User-Id`，订单服务会读取购物车中已选中的商品。

请求体：

```json
{
  "receiverName": "张三",
  "receiverPhone": "13800000000",
  "receiverAddress": "上海市 浦东新区 测试路 1 号"
}
```

### 4.4 GET /orders

查询当前用户订单列表。

### 4.5 GET /orders/{id}

查询订单详情，只能查询当前用户的订单。

### 4.6 PUT /orders/{id}/cancel

取消订单，只允许取消状态为待支付的订单。

## 5. 数据模型

`Order` 映射 `t_order`：

- `orderNo`、`userId`、`totalAmount`
- `status`（0 待支付，2 已取消）
- `receiverName`、`receiverPhone`、`receiverAddress`
- `createTime`、`updateTime`

`OrderItem` 映射 `t_order_item`：

- `orderId`、`bookId`、`bookTitle`
- `bookPrice`、`quantity`、`subtotal`
- `createTime`

订单明细保存下单时的标题和价格快照，后续修改图书价格不会影响历史订单。

## 6. 服务间调用

- [BookClient.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/client/BookClient.java) 使用 OpenFeign
- 服务名：`book`
- 调用接口：`GET /books/{id}`
- 返回体转换为订单模块自己的 `BookSnapshot`

- [CartClient.java](D:/workspace_idea/BookMall/BookMall/bookmall-order/src/main/java/com/bookmall/order/client/CartClient.java) 使用 OpenFeign
- 服务名：`cart`
- 调用接口：`GET /cart/selected`
- 返回体转换为订单模块自己的 `CartItemSnapshot`

当前订单模块不依赖库存和地址微服务；下单成功后，前端会清理已下单的购物车条目。

## 7. 验证方式

通过网关验证：

```text
GET http://localhost:8080/api/orders/hello
POST http://localhost:8080/api/orders
POST http://localhost:8080/api/orders/from-cart
GET http://localhost:8080/api/orders
GET http://localhost:8080/api/orders/1
PUT http://localhost:8080/api/orders/1/cancel
```

除 `GET /orders/hello` 外，其他接口都需要在请求头携带：

```text
Authorization: Bearer xxxxx.yyyyy.zzzzz
```

网关校验 JWT 后会写入 `X-User-Id`。
