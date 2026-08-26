# BookMall 增强项实施说明

## 1. 当前已落地增强项

当前只实现两项增强能力：

- `bookmall-book` 图书详情使用 Spring Cache + Redis
- `bookmall-book` 图书列表使用 Sentinel QPS 限流

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

## 4. 验证方式

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

## 5. 当前状态

本文档只描述当前已实现并验证的增强能力。
