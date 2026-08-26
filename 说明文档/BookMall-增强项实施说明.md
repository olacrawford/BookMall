# BookMall 增强项实施说明

## 1. 当前已落地增强项

当前已实现三项增强能力：

- `bookmall-book` 图书详情使用 Spring Cache + Redis
- `bookmall-book` 图书列表使用 Sentinel QPS 限流
- `bookmall-payment` / `bookmall-order` 使用 RabbitMQ 做支付成功事件的最终一致性补偿
- `bookmall-order` / `bookmall-stock` / `bookmall-payment` 补充核心服务单元测试

## 2. Redis 缓存

### 2.1 实现方式

- [RedisConfig.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/config/RedisConfig.java) 提供基于 Redis 的 `CacheManager`
- [BookApplication.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/BookApplication.java) 开启 `@EnableCaching`
- [BookServiceImpl.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/service/impl/BookServiceImpl.java) 在 `getBookById()` 上使用 `@Cacheable(cacheNames = "book")`
- 新增、修改、删除图书使用 `@CacheEvict(cacheNames = "book", allEntries = true)`

### 2.2 当前缓存范围

- 缓存对象：图书详情
- Redis 缓存键：`book::<id>`
- 不缓存图书列表
- 不缓存分类数据

## 3. Sentinel 限流

### 3.1 实现方式

- [SentinelConfig.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/config/SentinelConfig.java) 使用代码定义流控规则
- [BookServiceImpl.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/service/impl/BookServiceImpl.java) 在 `listBooks()` 上使用 `@SentinelResource`

### 3.2 当前规则

- 资源名：`listBooks`
- 流控模式：QPS
- 阈值：每秒 1 次
- 超限响应：`429 图书列表请求过于频繁，请稍后再试`

当前只有 `GET /books` 被限流，没有为详情、分页或分类接口配置独立规则。

## 4. RabbitMQ 最终一致性

### 4.1 实现链路

- `PaymentServiceImpl` 支付成功后调用 `PaySuccessPublisher`
- `PaySuccessPublisher` 使用 `RabbitTemplate` 把 `PaySuccessMessage` 发布到 `bookmall.pay.success.exchange`
- `bookmall-order` 的 `PaySuccessConsumer` 使用 `@RabbitListener` 订阅 `bookmall.order.pay.success.queue`
- 消费端调用 `markPaid`，订单状态和库存确认按 `orderId` 幂等处理

订单消费 `PaySuccessMessage` 后调用 `markPaid`，并发布 `OrderStockEvent` 给库存服务：

- 路由键 `order.paid`：库存服务确认库存
- 路由键 `order.stock.release`：库存服务释放库存
- 下单前的库存预占仍使用同步 Feign，因为创建订单时需要立即确认库存是否充足

消息重复消费通过支付订单幂等和库存重复确认判断保证不会重复扣减库存。

## 5. 核心服务单元测试

- `bookmall-order`：确认收货幂等、支付幂等、取消/超时释放事件
- `bookmall-stock`：库存预占失败、释放幂等、确认幂等
- `bookmall-payment`：支付成功发布事件、MQ 发送失败回滚
- 测试依赖使用 `spring-boot-starter-test`，可运行 `mvn -pl bookmall-order,bookmall-stock,bookmall-payment -am test`

## 6. 验证方式

Redis 缓存验证：

```text
GET http://localhost:8080/api/books/1
GET http://localhost:8080/api/books/1
```

第二次请求命中 `book::1` 缓存。

Sentinel 验证：

```bash
for i in 1 2 3; do curl http://localhost:8080/api/books; echo; done
```

连续请求超过每秒 1 次后返回 429。

## 7. 当前状态

本文档只描述当前已实现并验证的增强能力。
