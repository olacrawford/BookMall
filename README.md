# BookMall

一个面向学习与展示的微服务图书商城项目，采用前后端分离架构，后端基于 Spring Cloud Alibaba，前端基于 Vue 3，当前已经完成核心购物链路、Redis 热点缓存、Sentinel 限流，以及 Docker Nginx 统一入口。

## 项目亮点

- 微服务拆分清晰：认证、图书、购物车、订单、库存、地址、网关独立演进
- 前后端分离：Vue 3 前端通过 Gateway 与各业务服务联通
- 企业常见基础能力已接入：Nacos、Gateway、OpenFeign、Redis、Sentinel、Nginx
- 核心链路可运行：注册、登录、图书浏览、分类查看、购物车、下单、地址管理
- 面向开源整理：敏感配置已改为环境变量覆盖，便于公开展示与二次开发

## 技术栈

| 分类 | 技术 |
|---|---|
| 后端 | Java 17, Spring Boot 3.2.5 |
| 微服务 | Spring Cloud 2023.0.2, Spring Cloud Alibaba 2023.0.1.0 |
| 注册发现 | Nacos |
| 网关 | Spring Cloud Gateway |
| 服务调用 | OpenFeign |
| 数据访问 | MyBatis-Plus |
| 数据库 | MySQL 8.x |
| 缓存 | Redis |
| 限流 | Sentinel |
| 前端 | Vue 3, Vite, Vue Router, Axios |
| 部署入口 | Docker Nginx |
| 认证 | JWT, BCrypt |

## 仓库结构

```text
BookMall/
├─ BookMall/        后端微服务工程
├─ front/           前端工程
├─ sql/             数据库脚本
├─ 说明文档/         模块说明与增强项文档
└─ docker-compose.nginx.yml
```

## 系统架构

```text
Browser
  -> Nginx (Docker, 80)
  -> Vue Frontend
  -> /api/**
  -> Gateway (8080)
  -> auth / book / cart / order / inventory / address
  -> MySQL / Redis / Nacos
```

## 已完成功能

### 用户侧

- 用户注册
- 用户登录
- 登录态查询
- 收货地址新增、列表、默认地址设置、删除

### 商城侧

- 图书列表
- 图书详情
- 图书搜索
- 分类展示与子分类查看
- 购物车增删改查
- 订单创建、列表、详情、取消
- 库存查询与扣减

### 基础设施与增强项

- Nacos 服务注册与发现
- Gateway 统一路由转发
- OpenFeign 服务间调用
- Redis 热点图书缓存
- Sentinel 高频接口限流
- Nginx 前端统一入口
- 统一返回体与全局异常处理

## 本地运行说明

### 1. 基础环境

建议环境：

- Windows + IDEA 启动 Java 服务
- Docker 启动 MySQL、Redis、Nacos、Nginx
- WSL 用于辅助命令行调试

### 2. 启动基础设施

确保以下组件可用：

- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- Nacos: `localhost:8848`
- Nginx: `localhost:80`

### 3. 导入数据库

执行脚本：

- [sql/sql.txt](/D:/workspace_idea/BookMall/sql/sql.txt)

创建数据库：

- `bookmall`

### 4. 配置环境变量

至少建议配置：

```text
BOOKMALL_DB_PASSWORD=root
BOOKMALL_JWT_SECRET=bookmall-local-demo-secret
```

如果你的服务地址不是默认值，还可以继续覆盖：

- `BOOKMALL_DB_HOST`
- `BOOKMALL_DB_PORT`
- `BOOKMALL_DB_USERNAME`
- `BOOKMALL_NACOS_ADDR`
- `BOOKMALL_REDIS_HOST`
- `BOOKMALL_REDIS_PORT`
- `BOOKMALL_SENTINEL_DASHBOARD`

### 5. 启动后端服务

建议顺序：

1. `bookmall-auth`
2. `bookmall-book`
3. `bookmall-cart`
4. `bookmall-inventory`
5. `bookmall-address`
6. `bookmall-order`
7. `bookmall-gateway`

后端主工程说明见：

- [BookMall/README.md](/D:/workspace_idea/BookMall/BookMall/README.md)

### 6. 启动前端

前端源码目录：

- [front](/D:/workspace_idea/BookMall/front)

开发模式：

```bash
cd front
npm install
npm run dev
```

如果沿用当前项目方式，也可以直接将构建产物发布到 Docker 中的 Nginx。

### 7. 访问入口

- 前端首页: `http://localhost`
- 网关入口: `http://localhost:8080`
- 图书接口示例: `http://localhost/api/books`

## 文档目录

详细说明已经拆分到文档目录，适合继续完善成课程设计或作品集材料：

- [说明文档/BookMall-基础设施搭建说明.md](/D:/workspace_idea/BookMall/说明文档/BookMall-基础设施搭建说明.md)
- [说明文档/BookMall-增强项实施说明.md](/D:/workspace_idea/BookMall/说明文档/BookMall-增强项实施说明.md)
- [说明文档/BookMall-Nginx部署说明.md](/D:/workspace_idea/BookMall/说明文档/BookMall-Nginx部署说明.md)
- [说明文档/BookMall-auth说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-auth说明文档.md)
- [说明文档/BookMall-book说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-book说明文档.md)
- [说明文档/BookMall-cart说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-cart说明文档.md)
- [说明文档/BookMall-gateway说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-gateway说明文档.md)
- [说明文档/BookMall-inventory说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-inventory说明文档.md)
- [说明文档/BookMall-order说明文档.md](/D:/workspace_idea/BookMall/说明文档/BookMall-order说明文档.md)

## 当前规划

已完成：

- 前后端主链路联通
- Redis 缓存增强
- Sentinel 限流增强
- Nginx 统一入口
- README 与模块说明文档整理

待继续：

- RabbitMQ 异步通知
- Seata 分布式事务
- 日志与链路追踪完善
- 更完整的 Docker Compose 与部署脚本
