# BookMall

一个面向学习与展示的微服务图书商城项目，采用前后端分离架构，后端基于 Spring Cloud Alibaba，前端基于 Vue 3。当前保留三个核心业务微服务（用户、图书、订单）加网关与公共模块，核心链路（注册登录、图书浏览、直接下单、订单管理）已跑通。

## 项目亮点

- 微服务拆分清晰：认证、图书、订单三个业务服务 + 网关、公共模块独立演进
- 前后端分离：Vue 3 前端通过 Gateway 与各业务服务联通
- 企业常见基础能力已接入：Nacos、Nacos Config、Gateway、OpenFeign、Redis、Sentinel
- 网关统一鉴权：JWT 校验 + 用户身份透传（X-User-Id）
- 接口文档：Knife4j 自动生成在线文档
- 核心链路可运行：注册、登录、图书浏览、分类查看、直接下单、订单管理

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
| 数据访问 | MyBatis-Plus |
| 数据库 | MySQL 8.x |
| 前端 | Vue 3, Vite, Vue Router, Axios |
| 认证 | JWT, BCrypt |

## 仓库结构

```text
BookMall/
├─ BookMall/        后端微服务工程
├─ front/           前端工程
├─ sql/             数据库脚本
├─ nacos-config/    Nacos 配置中心脚本
└─ 说明文档/         模块说明文档
```

## 系统架构

```text
Browser
  -> Vue Frontend (Vite)
  -> /api/**
  -> Gateway (8080)
  -> auth / book / order
  -> MySQL / Nacos
```

## 已完成功能

### 用户侧

- 用户注册
- 用户登录

### 商城侧

- 图书增删改查 + 分页查询
- 分类列表（平铺大类，不细分）
- 直接下单（选书 + 填收货信息）
- 订单列表、详情、取消（含越权校验）

### 基础设施

- Nacos 服务注册与发现
- Gateway 统一路由转发 + JWT 鉴权过滤器
- OpenFeign 服务间调用
- 统一返回体与全局异常处理

## 本地运行说明

### 1. 基础环境

- Windows + IDEA 启动 Java 服务
- Docker 启动 MySQL、Nacos

### 2. 启动基础设施

- MySQL: `localhost:3306`
- Nacos: `localhost:8848`

### 3. 导入数据库

执行脚本 [sql/sql.txt](sql/sql.txt)，会创建数据库 `bookmall` 及核心表。

### 4. 数据库与配置

数据库连接、Nacos 地址、JWT 密钥等已直接写在各服务的 `application.yml` 里（默认 `localhost:3306`、账号 `root`、密码 `123456`），本地直连无需额外配置。

### 5. 启动后端服务

建议顺序：

1. `bookmall-auth`（8060）
2. `bookmall-book`（8070）
3. `bookmall-order`（8050）
4. `bookmall-gateway`（8080）

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
- [说明文档/BookMall-order说明文档.md](说明文档/BookMall-order说明文档.md)
- [说明文档/BookMall-gateway说明文档.md](说明文档/BookMall-gateway说明文档.md)
