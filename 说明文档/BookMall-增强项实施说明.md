# BookMall 增强项实施说明

## 1. 当前进度

本阶段先落地 Redis 热点图书缓存，并在 `book` 服务内接入 Sentinel 限流，避免 Gateway WebFlux 与 Servlet 兼容问题。

已完成：
- `bookmall-book` 接入 Redis
- 图书列表缓存
- 图书详情缓存
- 分类树缓存
- 写操作后缓存失效
- `bookmall-book` 接入 Sentinel
- 图书列表、详情、搜索、分页接口限流
- 统一 JSON 限流错误返回
- 已恢复为适合浏览器联调的正常限流阈值

暂缓：
- `bookmall-gateway` Sentinel 限流

## 2. 本次新增的功能

### 2.1 热点图书缓存

目标：
- 减少图书列表、图书详情、分类树对数据库的重复读取
- 提高首页和图书页的响应速度

缓存内容：
- 图书列表 `book:list`
- 图书详情 `book:detail:{id}`
- 分类树 `book:category:tree`

缓存策略：
- 热点数据缓存到 Redis
- 空数据做短期空值缓存，防止缓存穿透
- 刷新时清理相关缓存键
- TTL 随机抖动，减少同一时间过期

### 2.2 Book 服务 Sentinel 限流

目标：
- 在业务服务内部直接保护高频接口
- 避开 Gateway WebFlux 和 Servlet 自动配置冲突

当前限流资源：
- `GET /books`：默认 `6 QPS`
- `GET /books/{id}`：默认 `12 QPS`
- `GET /books/search`：默认 `4 QPS`
- `GET /books/page`：默认 `4 QPS`

当前效果：
- 正常浏览器访问可直接查看数据
- 在短时间高频刷新或批量请求时触发限流
- 超过阈值后返回统一 `429` JSON 结构

## 3. 相关代码位置

### 3.1 Redis 相关

#### 模块依赖

文件：
- [D:\workspace_idea\BookMall\BookMall\bookmall-book\pom.xml](D:\workspace_idea\BookMall\BookMall\bookmall-book\pom.xml)

新增依赖：
- `spring-boot-starter-data-redis`

作用：
- 提供 Redis 连接与 `RedisTemplate`

#### Redis 配置

文件：
- [D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\config\RedisConfig.java](D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\config\RedisConfig.java)

作用：
- 配置 `RedisTemplate<String, Object>`
- 使用 JSON 序列化保存对象

#### 缓存参数

文件：
- [D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\config\CacheProperties.java](D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\config\CacheProperties.java)

作用：
- 统一管理缓存过期时间

#### 缓存常量

文件：
- [D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\constant\CacheKeys.java](D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\constant\CacheKeys.java)

作用：
- 统一管理 Redis key

#### 缓存封装

文件：
- [D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\service\support\CacheSupport.java](D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\service\support\CacheSupport.java)

作用：
- 提供统一缓存读写方法
- 封装空值缓存
- 封装缓存清理

#### 图书服务改造

文件：
- [D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\service\impl\BookServiceImpl.java](D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\service\impl\BookServiceImpl.java)

改动点：
- `listBooks()` 先查 Redis，命中则直接返回
- `getBookById()` 先查 Redis，命中则直接返回
- `createBook()`、`updateBook()`、`deleteBook()`、`updateBookStatus()` 后清理缓存
- `searchBooks()`、`pageBooks()` 保持走数据库
- 通过 `@SentinelResource` 接入限流

#### 分类服务改造

文件：
- [D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\service\impl\CategoryServiceImpl.java](D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\service\impl\CategoryServiceImpl.java)

改动点：
- `listCategoryTree()` 增加 Redis 缓存
- 分类树为空时做短期空值缓存

#### 应用配置

文件：
- [D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\resources\application.yml](D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\resources\application.yml)

新增：
- Redis 连接配置
- `bookmall.cache` 配置段
- `bookmall.sentinel` 配置段

### 3.2 Sentinel 相关

#### 限流参数配置

文件：
- [D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\config\SentinelRuleProperties.java](D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\config\SentinelRuleProperties.java)

作用：
- 管理图书列表、详情、搜索、分页接口的 QPS 阈值

#### 限流规则配置

文件：
- [D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\config\SentinelConfig.java](D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\config\SentinelConfig.java)

作用：
- 通过 `FlowRuleManager` 加载接口限流规则
- 为 `list / detail / search / page` 接口设置 QPS

#### 限流异常

文件：
- [D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\exception\SentinelBlockedException.java](D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\exception\SentinelBlockedException.java)

作用：
- 统一封装限流后的 429 异常

#### 启动类配置

文件：
- [D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\BookApplication.java](D:\workspace_idea\BookMall\BookMall\bookmall-book\src\main\java\com\bookmall\book\BookApplication.java)

新增：
- `@EnableConfigurationProperties({CacheProperties.class, SentinelRuleProperties.class})`

作用：
- 让缓存和限流参数类生效

## 4. 运行要求

### 4.1 Redis

当前 Redis 需要先启动，默认连接：
- Host: `localhost`
- Port: `6379`

### 4.2 Sentinel

这次 Sentinel 不再依赖 Gateway，而是直接在 `book` 服务内工作。

因此不需要额外 Dashboard 就能先验证限流效果。

## 5. 验证方式

### 5.1 Redis 验证

建议按这个顺序验证：

1. 启动 Redis
2. 启动 `bookmall-book`
3. 访问 `GET /books`
4. 再次访问同接口，观察 Redis 命中
5. 修改图书后检查缓存是否失效

### 5.2 Sentinel 验证

建议按这个顺序验证：

1. 启动 `bookmall-book`
2. 浏览器访问 `http://localhost:8082/books`，确认正常返回图书数据
3. 浏览器访问 `http://localhost:8082/books/1`，确认正常返回图书详情
4. 浏览器访问 `http://localhost:8082/books/search?keyword=Java`，确认搜索接口可正常使用
5. 在浏览器中连续快速刷新，或者使用 Postman、Apifox、curl 在 1 秒内连续发起多次请求
6. 当请求速率超过阈值时，检查是否返回：

```json
{
  "code": 429,
  "message": "图书列表请求过于频繁，请稍后再试",
  "data": null
}
```

如果后续需要做压测演示，可以临时把配置里的阈值调低，例如：
- `list-qps: 1`
- `detail-qps: 1`
- `search-qps: 1`

## 6. 后续增强计划

下一步建议按以下顺序继续：

1. `RabbitMQ` 异步通知
2. `Seata` 分布式事务
3. 如果需要，再把 Sentinel 扩展到其他服务

## 7. 维护规则

以后每加一项增强，都补两部分说明：

- 做了什么功能
- 哪些代码实现了这个功能

建议每次都记录：
- 新增依赖
- 新增配置
- 新增核心类
- 改造的业务方法
- 验证方式
