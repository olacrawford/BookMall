# BookMall 项目说明文档

> 一个基于 Spring Cloud Alibaba 的微服务图书商城项目，当前保留用户、图书、购物车、库存、支付、订单六个核心业务服务 + 网关与公共模块，前后端主链路已跑通，适合作为微服务课程设计、毕业设计或个人作品集项目。

## 项目定位

BookMall 是一个围绕图书商城业务拆分的前后端分离项目，目标不是只完成接口演示，而是尽量贴近真实企业项目中的分层、服务拆分和基础设施接入方式。

它目前更适合用于：

- 微服务项目学习与练手
- Java 后端作品集展示
- Spring Cloud Alibaba 技术栈实践
- 前后端联调演示

## 项目概览

### 技术栈

| 分类 | 技术 |
|---|---|
| 语言 | Java 17 |
| 后端框架 | Spring Boot 3.2.5 |
| 微服务 | Spring Cloud 2023.0.2 |
| 微服务套件 | Spring Cloud Alibaba 2023.0.1.0 |
| 注册发现 | Nacos |
| 配置中心 | Nacos Config |
| 接口文档 | Knife4j |
| 网关 | Spring Cloud Gateway |
| 远程调用 | OpenFeign |
| 负载均衡 | Spring Cloud LoadBalancer |
| ORM | MyBatis-Plus 3.5.14 |
| 数据库 | MySQL 8.x |
| 缓存 | Redis |
| 限流 | Sentinel |
| 前端 | Vue 3 + Vite + Vue Router + Axios |
| 认证 | JWT + BCrypt |

### 当前模块

| 模块 | 端口 | 职责 |
|---|---:|---|
| `bookmall-common` | - | 公共返回体、错误码、异常处理、分页对象 |
| `bookmall-gateway` | 8080 | 统一入口、路由转发、JWT 鉴权、跨域 |
| `bookmall-auth` | 8060 | 注册、登录、收货地址管理 |
| `bookmall-book` | 8070 | 图书增删改查、分页、分类 |
| `bookmall-cart` | 8083 | 购物车增加、查询、修改、删除、清空、结算 |
| `bookmall-stock` | 8090 | 库存查询、下单预占、支付确认、取消释放 |
| `bookmall-order` | 8050 | 订单（直接下单、购物车下单、列表、详情、取消、超时自动关单） |
| `bookmall-payment` | 8051 | 支付单、内部模拟支付、订单状态更新与库存确认 |
| `front` | 5173 | 前端，Vite 托管 |

## 项目亮点

- 微服务职责划分清楚，当前已形成可运行的注册、发现、路由、鉴权和远程调用链路
- 网关统一鉴权：在 Gateway 校验 JWT，并把 userId 透传给下游（`X-User-Id`），下游不再信任前端传的 userId
- OpenFeign 服务间调用：购物车调图书服务校验图书，订单调图书/购物车/库存服务，支付服务调订单服务完成支付
- 统一返回体 + 全局异常处理，接口成功失败格式一致
- 订单越权校验：只能查看/取消自己的订单

## 当前架构

```text
浏览器
  -> Vue Frontend (Vite, 5173)
  -> /api/**
  -> Gateway (8080)  —— 路由 + JWT 鉴权 + 跨域
  -> auth / book / cart / stock / payment / order
  -> MySQL / Nacos
```

鉴权链路：

```text
前端请求（带 Bearer token）
  -> 网关 AuthGlobalFilter：校验 JWT → 放行，并把 userId 放入 X-User-Id 头
  -> 下游服务：从 X-User-Id 拿 userId（前端无法伪造）
```

## 快速启动

### 1. 准备基础环境

- Java 17
- Maven 3.9+
- Node.js 18+
- MySQL（`localhost:3306`）
- Redis（`localhost:6379`）
- Nacos（`localhost:8848`）

### 2. 初始化数据库

执行脚本 [sql/sql.txt](/D:/workspace_idea/BookMall/sql/sql.txt)，会创建数据库 `bookmall` 及全部 9 张表：

- `t_user`（用户）
- `t_category`（分类，平铺大类）
- `t_book`（图书）
- `t_user_address`（收货地址）
- `t_cart_item`（购物车）
- `t_book_stock`（图书库存）
- `t_order`（订单）
- `t_order_item`（订单明细）
- `t_payment`（支付单）

脚本里还内置了 8 个示例分类（文学、计算机、历史等）。

已有环境按顺序执行 `sql/updates/001_cart_address_stock.sql`、`002_stock_order.sql`、`003_payment.sql`、`004_order_expire_stock_confirm.sql` 完成增量升级。

数据库连接、Redis 地址等环境相关配置放在 **Nacos 配置中心**（dataId 为各服务名，如 `book.yaml`），数据库默认 `root` / `123456`。首次运行或 Nacos 数据丢失后，执行 `nacos-config/publish.sh` 重新发布配置。

### 3. 启动顺序

1. 启动 MySQL、Redis（`docker start redis`）、Nacos
2. 启动 `bookmall-auth`（8060）
3. 启动 `bookmall-book`（8070）
4. 启动 `bookmall-cart`（8083）
5. 启动 `bookmall-stock`（8090）
6. 启动 `bookmall-order`（8050）
7. 启动 `bookmall-payment`（8051）
8. 启动 `bookmall-gateway`（8080）

### 4. 前端运行

```bash
cd front
npm install
npm run dev    # 默认 5173，/api 代理到网关 8080
```

## 验证 Redis 缓存与 Sentinel 限流

### Redis 缓存

连续两次请求图书详情，第一次查库、第二次命中缓存：

```bash
curl http://localhost:8080/api/books/1   # 第一次：查库，写入缓存
curl http://localhost:8080/api/books/1   # 第二次：命中缓存
docker exec -it redis redis-cli keys 'book*'   # 能看到 book::1 缓存键
```

修改/删除图书后缓存会自动失效，下次查询会重新查库。

### Sentinel 限流

快速连续请求图书列表（`GET /books`，即 `listBooks`），超过每秒 1 次就触发限流：

```bash
for i in 1 2 3; do curl http://localhost:8080/api/books; echo; done
# 第一次正常，后面返回 429「图书列表请求过于频繁，请稍后再试」
```

> 注意：限流加在 `listBooks`（`GET /books`）上，而前端图书页用的是分页接口 `/books/page`，所以前端页面不会触发限流，演示时用上面的 curl 命令即可。

## 访问方式

- 前端：`http://localhost:5173`
- 网关：`http://localhost:8080`
- 常用接口：
  - `GET http://localhost:8080/api/books/hello`
  - `GET http://localhost:8080/api/books`
  - `GET http://localhost:8080/api/books/page?pageNum=1&pageSize=10`

## 已完成的主要功能

### 前端

- 登录 / 注册页
- 登录后进入首页总览
- 图书中心：分页列表 + 分类下拉筛选 + 关键字搜索 + 立即购买
- 订单中心：订单列表、详情、取消、超时自动关单

### 后端基础能力

- Nacos 服务注册发现
- Gateway 路由转发 + JWT 鉴权过滤器
- OpenFeign 服务间调用
- 统一 `Result` 返回体
- 全局异常处理
- JWT 登录态 + BCrypt 密码加密
- Auth 收货地址管理

## 关键实现

### `bookmall-common`

- `Result<T>`：统一返回体 `{code, message, data}`
- `ErrorCode`：统一错误码
- `BusinessException`：业务异常
- `GlobalExceptionHandler`：全局异常转 `Result`
- `PageResult<T>`：分页返回对象（records/total/pages/current/size）

### `bookmall-gateway`

- `lb://` 路由到各服务，`StripPrefix=1` 去掉 `/api` 前缀
- `AuthGlobalFilter`：全局过滤器，校验 JWT 签名与过期时间，把 `userId` 放入 `X-User-Id` 头透传；登录/注册/`hello` 接口放行
- 全局跨域已配置

### `bookmall-auth`

- `POST /auth/register`：注册，BCrypt 加密密码
- `POST /auth/login`：登录，校验密码后签发 JWT

### `bookmall-book`

- `GET /books`、`GET /books/{id}`：查询列表 / 详情
- `GET /books/page`：分页（支持书名关键字 + 分类精确筛选）
- `POST /books`、`PUT /books/{id}`、`DELETE /books/{id}`：增改删（软删除）
- `GET /books/categories`：平铺分类列表
- 分页使用 MyBatis-Plus `PaginationInnerInterceptor`
- Redis 缓存：`getBookById` 结果缓存到 Redis，增删改时失效缓存
- Sentinel 限流：`listBooks` 配置 QPS=1 的限流，超限返回友好提示

### `bookmall-order`

- `POST /orders`：直接下单（Feign 调图书服务拿价格 → 预占库存 → 落订单 + 明细快照）
- `POST /orders/from-cart`：购物车已选条目下单（Feign 调购物车、图书和库存服务 → 预占库存 → 创建多明细订单）
- `GET /orders`：订单列表
- `GET /orders/{id}`、`PUT /orders/{id}/cancel`：详情 / 取消（取消时释放库存，含越权校验）
- `PUT /orders/{id}/paid`：支付服务调用，把待支付订单更新为已支付并确认库存
- 定时任务：扫描并关闭超过 `expire_time` 的待支付订单，释放预占库存
- userId 从网关透传的 `X-User-Id` 头获取

### `bookmall-stock`

- `GET /stock/{bookId}`：查询可售库存与锁定库存
- `POST /stock/deduct`：下单前原子预占库存，防止并发超卖
- `POST /stock/release`：取消订单或补偿时释放预占库存
- `POST /stock/confirm`：支付成功后把预占库存确认为真实扣减
- 使用 `t_book_stock` 的 `stock / locked_stock / version` 字段维护库存状态

### `bookmall-payment`

- `POST /payment/pay`：内部模拟支付，校验订单后生成支付单，订单状态更新并确认库存
- `GET /payment/order/{orderId}`：查询订单对应的支付单
- 支付前通过 Feign 调订单服务校验订单归属和待支付状态
- 使用 `t_payment` 保存支付单，当前 `payType=mock`

### 配置中心（Nacos Config）

- 各服务的 `application.yml` 只保留本机配置（端口、服务名、Nacos 地址）
- 数据源、JWT 等环境相关配置放在 Nacos，dataId 为 `{服务名}.yaml`（如 `auth.yaml`）
- 通过 `spring.config.import: optional:nacos:xxx.yaml` 拉取，改动 Nacos 配置无需重启即可生效

### 接口文档（Knife4j）

- 六个业务服务已接入 Knife4j，启动后访问：
  - 认证服务：`http://localhost:8060/doc.html`
  - 图书服务：`http://localhost:8070/doc.html`
  - 购物车服务：`http://localhost:8083/doc.html`
  - 库存服务：`http://localhost:8090/doc.html`
  - 订单服务：`http://localhost:8050/doc.html`
  - 支付服务：`http://localhost:8051/doc.html`

## 说明文档

更细的模块说明在 `说明文档/` 目录下：

- [说明文档/BookMall-基础设施搭建说明.md](/D:/workspace_idea/BookMall/说明文档/BookMall-基础设施搭建说明.md)
- [说明文档/BookMall-auth说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-auth说明文档.md)
- [说明文档/BookMall-book说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-book说明文档.md)
- [说明文档/BookMall-cart说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-cart说明文档.md)
- [说明文档/BookMall-stock说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-stock说明文档.md)
- [说明文档/BookMall-payment说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-payment说明文档.md)
- [说明文档/BookMall-gateway说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-gateway说明文档.md)
- [说明文档/BookMall-order说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-order说明文档.md)

## 目前进度

### 已完成

- 项目基础架构搭建（6 业务服务 + 网关 + 公共模块）
- 全部服务接入 Nacos
- Gateway 路由 + JWT 统一鉴权 + 用户身份透传
- 订单服务 OpenFeign 下单 + 越权校验
- 图书服务 CRUD + 分页 + 平铺分类
- 购物车服务加入、查询、修改、删除、清空、结算下单
- 库存服务查询、下单预占、支付确认、取消释放
- 支付服务支付单生成、订单状态更新与库存确认
- 订单超时未支付自动关单
- 前端登录/图书/购物车/订单主链路联通
- 文档体系整理
