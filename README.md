# BookMall

一个面向学习与展示的微服务图书商城项目，采用前后端分离架构，后端基于 Spring Cloud Alibaba，前端基于 Vue 3。当前保留六个核心业务微服务（用户、图书、购物车、库存、支付、订单）加网关与公共模块，核心链路（注册登录、图书浏览、购物车结算下单、库存预占与确认、模拟支付、订单管理与超时关单）已跑通。

## 项目亮点

- 微服务拆分清晰：认证、图书、购物车、库存、支付、订单六个业务服务 + 网关、公共模块独立演进
- 前后端分离：Vue 3 前端通过 Gateway 与各业务服务联通
- 企业常见基础能力已接入：Nacos、Nacos Config、Gateway、OpenFeign、Redis、Sentinel、RabbitMQ
- 网关统一鉴权：JWT 校验 + 用户身份透传（X-User-Id）
- 接口文档：Knife4j 自动生成在线文档
- 核心链路可运行：注册、登录、图书浏览、分类查看、购物车结算下单、支付确认库存、订单超时自动取消

## 技术栈

| 分类 | 技术 |
|---|---|
| 后端 | Java 17, Spring Boot 3.2.5 |
| 微服务 | Spring Cloud 2023.0.2, Spring Cloud Alibaba 2023.0.1.0 |
| 注册发现 | Nacos |
| 配置中心 | Nacos Config |
| 接口文档 | Knife4j |
| 网关 | Spring Cloud Gateway |
| 服务调用 | OpenFeign |
| 消息队列 | RabbitMQ |
| 数据访问 | MyBatis-Plus |
| 数据库 | MySQL 8.x |
| 前端 | Vue 3, Vite, Vue Router, Axios |
| 认证 | JWT, BCrypt |

## 仓库结构

```text
BookMall/
├─ BookMall/        后端微服务工程
├─ front/           前端工程
├─ sql/             数据库脚本（sql.txt + updates/ 增量脚本）
├─ nacos-config/    Nacos 配置中心脚本
└─ 说明文档/         模块说明文档
```

## 系统架构

```text
Browser
  -> Vue Frontend (Vite)
  -> /api/**
  -> Gateway (8080)
  -> auth / book / cart / stock / payment / order
  -> MySQL / Nacos / RabbitMQ
```

## 已完成功能

### 用户侧

- 用户注册
- 用户登录
- 收货地址管理

### 商城侧

- 图书增删改查 + 分页查询
- 分类列表（平铺大类，不细分）
- 购物车页面（加入、数量修改、勾选、删除、清空、结算下单）
- 图书库存查询、下单预占、支付确认、取消订单释放
- 支付单生成、内部模拟支付、订单状态变为已支付并确认库存
- 支付成功事件通过 RabbitMQ 异步补偿，订单更新失败时仍可最终一致
- 直接下单（选书 + 填收货信息）
- 订单列表、详情、取消、超时自动取消（含越权校验）

### 基础设施

- Nacos 服务注册与发现
- Gateway 统一路由转发 + JWT 鉴权过滤器
- OpenFeign 服务间调用
- RabbitMQ 支付成功事件发布与消费
- Redis 缓存图书列表/分页/详情/分类，Sentinel 接口限流
- 购物车并发加购原子更新、OpenFeign 超时配置、订单查询复合索引
- 统一返回体与全局异常处理

## 本地运行说明

### 1. 基础环境

- Windows + IDEA 启动 Java 服务
- Docker 启动 MySQL、Nacos、Redis、RabbitMQ

### 2. 启动基础设施

- MySQL: `localhost:3306`
- Nacos: `localhost:8848`
- Redis: `localhost:6379`
- RabbitMQ: `localhost:5672`（`admin` / `123456`）

### 3. 导入数据库

新环境初始化直接执行 [sql/sql.txt](sql/sql.txt) 即可，脚本已包含用户、图书、购物车、库存、订单、支付等全部 9 张表和默认库存。

已有环境按顺序执行 `sql/updates/001_cart_address_stock.sql`、`002_stock_order.sql`、`003_payment.sql`、`004_order_expire_stock_confirm.sql`、`005_optimization.sql` 完成增量升级。

### 4. 数据库与配置

数据库连接、JWT 密钥、Redis 地址等环境依赖写在 `nacos-config/*.yaml` 里（默认 `localhost:3306`、账号 `root`、密码 `123456`）。各服务 `application.yml` 只维护端口、Nacos 地址和配置导入，本地直连无需额外修改。

### 5. 启动后端服务

建议顺序：

1. `bookmall-auth`（8060）
2. `bookmall-book`（8070）
3. `bookmall-cart`（8083）
4. `bookmall-stock`（8090）
5. `bookmall-order`（8050）
6. `bookmall-payment`（8051）
7. `bookmall-gateway`（8080）

后端主工程说明见 [BookMall/README.md](BookMall/README.md)。

### 6. 启动前端

```bash
cd front
npm install
npm run dev
```

### 7. 访问入口

- 前端首页: `http://localhost:5173`
- 网关入口: `http://localhost:8080`
- 图书接口示例: `http://localhost:5173/api/books`

## 文档目录

- [说明文档/BookMall-基础设施搭建说明.md](说明文档/BookMall-基础设施搭建说明.md)
- [说明文档/BookMall-auth说明文档.md](说明文档/BookMall-auth说明文档.md)
- [说明文档/BookMall-book说明文档.md](说明文档/BookMall-book说明文档.md)
- [说明文档/BookMall-cart说明文档.md](说明文档/BookMall-cart说明文档.md)
- [说明文档/BookMall-stock说明文档.md](说明文档/BookMall-stock说明文档.md)
- [说明文档/BookMall-payment说明文档.md](说明文档/BookMall-payment说明文档.md)
- [说明文档/BookMall-order说明文档.md](说明文档/BookMall-order说明文档.md)
- [说明文档/BookMall-gateway说明文档.md](说明文档/BookMall-gateway说明文档.md)

