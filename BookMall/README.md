# BookMall 项目说明文档

> 一个基于 Spring Cloud Alibaba 的微服务图书商城项目，当前已经完成前后端主链路联通，并落地了 Redis 热点缓存、Sentinel 限流与 Docker Nginx 统一入口，适合作为微服务课程设计、毕业设计或个人作品集项目。

## 项目定位

BookMall 是一个围绕图书商城业务拆分的前后端分离项目，目标不是只完成接口演示，而是尽量贴近真实企业项目中的分层、服务拆分和基础设施接入方式。

它目前更适合用于：

- 微服务项目学习与练手
- Java 后端作品集展示
- Spring Cloud Alibaba 技术栈实践
- 前后端联调与部署演示

## 项目概览

### 技术栈

| 分类 | 技术 |
|---|---|
| 语言 | Java 17 |
| 后端框架 | Spring Boot 3.2.5 |
| 微服务 | Spring Cloud 2023.0.2 |
| 微服务套件 | Spring Cloud Alibaba 2023.0.1.0 |
| 注册发现 | Nacos |
| 网关 | Spring Cloud Gateway |
| 远程调用 | OpenFeign |
| 负载均衡 | Spring Cloud LoadBalancer |
| ORM | MyBatis-Plus 3.5.14 |
| 数据库 | MySQL 8.x |
| 缓存 | Redis |
| 限流 | Sentinel |
| 前端 | Vue 3 + Vite + Vue Router + Axios |
| 反向代理 | Nginx |
| 认证 | JWT + BCrypt |

### 当前模块

| 模块 | 端口 | 职责 |
|---|---:|---|
| `bookmall-common` | - | 公共返回体、错误码、异常处理 |
| `bookmall-gateway` | 8080 | 统一入口、路由转发、跨域 |
| `bookmall-auth` | 8081 | 注册、登录、用户信息 |
| `bookmall-book` | 8082 | 图书、分类、缓存、限流 |
| `bookmall-cart` | 8083 | 购物车 |
| `bookmall-order` | 8084 | 订单 |
| `bookmall-inventory` | 8085 | 库存 |
| `bookmall-address` | 8086 | 收货地址 |
| `front` | 80 | 前端静态页面，Nginx 托管 |

## 项目亮点

- 微服务职责划分清楚，便于继续扩展消息队列、分布式事务和链路追踪
- 图书服务已经接入 Redis 热点缓存，能演示缓存命中与缓存失效
- 图书服务已经接入 Sentinel，可直接演示接口限流效果
- 网关与 Nginx 形成统一入口，前后端联调路径清晰
- 配置已经做开源化整理，适合直接发布到 GitHub 展示

## 当前架构

```text
浏览器
  -> Docker Nginx (80)
  -> 前端静态页面
  -> /api/**
  -> Gateway (8080)
  -> auth / book / cart / order / inventory / address
  -> MySQL / Redis / Nacos
```

## 开源说明

这个仓库是面向开源发布整理过的版本：

- 已移除源码配置中的固定数据库密码
- 已移除源码配置中的固定 JWT 密钥
- 数据源、Nacos、Redis、Sentinel 等参数改为环境变量可覆盖
- 本仓库默认值只用于本地演示，不建议直接用于生产环境

你在公开仓库中需要重点关注：

- 不要提交真实数据库账号密码
- 不要提交生产 JWT 密钥
- 不要提交 `target/`、`node_modules/`、`dist/` 等构建产物
- 不要提交只适用于你本机的绝对路径配置

## 环境变量

后端当前支持以下环境变量覆盖：

| 变量名 | 默认值 | 说明 |
|---|---|---|
| `BOOKMALL_DB_HOST` | `localhost` | MySQL 主机 |
| `BOOKMALL_DB_PORT` | `3306` | MySQL 端口 |
| `BOOKMALL_DB_NAME` | `bookmall` | 数据库名 |
| `BOOKMALL_DB_USERNAME` | `root` | 数据库用户名 |
| `BOOKMALL_DB_PASSWORD` | `change-me` | 数据库密码 |
| `BOOKMALL_NACOS_ADDR` | `localhost:8848` | Nacos 地址 |
| `BOOKMALL_NACOS_NAMESPACE` | `public` | Nacos 命名空间 |
| `BOOKMALL_NACOS_GROUP` | `DEFAULT_GROUP` | Nacos 分组 |
| `BOOKMALL_REDIS_HOST` | `localhost` | Redis 主机 |
| `BOOKMALL_REDIS_PORT` | `6379` | Redis 端口 |
| `BOOKMALL_REDIS_DB` | `0` | Redis 数据库 |
| `BOOKMALL_SENTINEL_DASHBOARD` | `localhost:8858` | Sentinel 控制台 |
| `BOOKMALL_SENTINEL_PORT` | `8720` | Sentinel 本地端口 |
| `BOOKMALL_JWT_SECRET` | `change-me-in-production` | JWT 密钥 |
| `BOOKMALL_JWT_EXPIRE_SECONDS` | `86400` | JWT 过期秒数 |

## 快速启动

### 1. 准备基础环境

如果你要在本地运行，可以先准备：

- MySQL
- Redis
- Nacos
- Nginx
- Java 17
- Maven 3.9+
- Node.js 18+

当前项目默认运行方式：

- Windows + IDEA 跑 Java 微服务
- Docker 跑 MySQL / Redis / Nacos / Nginx
- WSL 负责辅助命令执行

### 2. 初始化数据库

数据库名：`bookmall`

执行脚本：

- [sql/sql.txt](/D:/workspace_idea/BookMall/sql/sql.txt)

主要表：

- `t_user`
- `t_book`
- `t_category`
- `t_cart`
- `t_order`
- `t_order_item`
- `t_inventory`
- `t_address`

### 3. 配置本地环境变量

建议先在启动命令或 IDE 环境变量中设置：

```text
BOOKMALL_DB_PASSWORD=root
BOOKMALL_JWT_SECRET=bookmall-local-demo-secret
```

如果你的本地环境就是默认的 Docker MySQL / Redis / Nacos，也可以继续使用：

- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- Nacos: `localhost:8848`

### 4. 启动顺序

1. 启动 Docker：MySQL、Redis、Nacos、Nginx
2. 设置本地环境变量，至少补齐数据库密码和 JWT 密钥
3. 启动 `bookmall-auth`
4. 启动 `bookmall-book`
5. 启动 `bookmall-cart`
6. 启动 `bookmall-inventory`
7. 启动 `bookmall-address`
8. 启动 `bookmall-order`
9. 启动 `bookmall-gateway`

### 5. 前端运行

前端目录：

- [front](/D:/workspace_idea/BookMall/front)

开发模式：

```bash
cd front
npm install
npm run dev
```

如果沿用当前部署方式，可将打包后的静态资源放到 Docker Nginx 中统一托管。

## 访问方式

### 前端

- `http://localhost`

### 网关

- `http://localhost:8080`

### 常用接口

- `GET http://localhost/api/auth/hello`
- `GET http://localhost/api/books/hello`
- `GET http://localhost/api/books`
- `GET http://localhost/api/books/1`

## 已完成的主要功能

### 前端

- 登录 / 注册页
- 登录后进入首页总览
- 图书中心、购物车、订单、地址管理
- 分类展示改成更接近线上网站的格式
- 前端已由现有 Docker Nginx 托管

### 后端基础能力

- Nacos 服务注册发现
- Gateway 路由转发
- OpenFeign 服务间调用
- 统一 `Result` 返回体
- 全局异常处理
- JWT 登录态

### 增强项

- Redis 热点图书缓存
- Sentinel 热点接口限流
- Nginx 前端统一入口

## 关键实现

### `bookmall-common`

- `Result<T>`：统一返回体
- `ErrorCode`：统一错误码
- `BusinessException`：业务异常
- `GlobalExceptionHandler`：统一异常转 `Result`

### `bookmall-gateway`

- 使用 `lb://` 路由到各服务
- `/api/auth/**`、`/api/books/**` 等统一转发
- 全局跨域已配置

### `bookmall-book`

- 图书列表、详情、搜索、分页、增删改查
- 分类树查询
- Redis 缓存：`book:list`、`book:detail:{id}`、`book:category:tree`
- Sentinel 限流：列表、详情、搜索、分页
- 写操作后自动清理缓存

### `bookmall-order`

- 已接入 OpenFeign
- 通过 `BookClient`、`InventoryClient`、`AddressClient` 编排下单
- 使用快照 DTO 传输必要字段

## 验证方式

### Redis

```bash
docker exec -it redis redis-cli
keys book:*
```

### 网关

```bash
curl http://localhost:8080/api/books/hello
```

### Nginx 前端入口

```bash
curl http://localhost/api/books/hello
```

## 说明文档

更细的模块说明在 `说明文档/` 目录下：

- [说明文档/BookMall-基础设施搭建说明.md](/D:/workspace_idea/BookMall/说明文档/BookMall-基础设施搭建说明.md)
- [说明文档/BookMall-增强项实施说明.md](/D:/workspace_idea/BookMall/说明文档/BookMall-增强项实施说明.md)
- [说明文档/BookMall-Nginx部署说明.md](/D:/workspace_idea/BookMall/说明文档/BookMall-Nginx部署说明.md)
- [说明文档/BookMall-auth说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-auth说明文档.md)
- [说明文档/BookMall-book说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-book说明文档.md)
- [说明文档/BookMall-cart说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-cart说明文档.md)
- [说明文档/BookMall-gateway说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-gateway说明文档.md)
- [说明文档/BookMall-inventory说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-inventory说明文档.md)
- [说明文档/BookMall-order说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-order说明文档.md)

## 目前进度

### 已完成

- 项目基础架构搭建
- 全部服务接入 Nacos
- Gateway 路由改为服务发现
- 订单服务改用 OpenFeign
- Book 服务 Redis 缓存
- Book 服务 Sentinel 限流
- 前端 Nginx 统一入口
- 文档体系整理

### 待继续

- RabbitMQ 异步通知
- Seata 分布式事务
- 日志和链路追踪进一步完善
- 更完整的企业级部署脚本
