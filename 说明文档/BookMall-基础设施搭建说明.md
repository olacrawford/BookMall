# BookMall 基础设施搭建说明

## 1. 当前开发环境

当前项目运行在以下环境中：

- Windows 启动 Java 微服务
- WSL 使用 Docker Desktop 启动基础设施
- MySQL、Nacos、Redis 运行在 Docker 容器中
- Java 服务通过 `localhost` 访问 Docker 映射出来的端口

当前基础设施端口：

| 组件 | 地址 |
|---|---|
| MySQL | `localhost:3306` |
| Nacos | `localhost:8848` |
| Redis | `localhost:6379` |
| RabbitMQ | `localhost:5672`（账号 `admin` / `123456`） |

## 2. 当前后端模块

当前实际存在的后端模块：

| 模块 | 端口 | 当前职责 |
|---|---:|---|
| `bookmall-common` | - | 公共返回体、错误码、异常、分页 |
| `bookmall-gateway` | 8080 | 路由、跨域、JWT 鉴权 |
| `bookmall-auth` | 8060 | 注册、登录、地址管理 |
| `bookmall-book` | 8070 | 图书、分类、Redis 缓存、Sentinel |
| `bookmall-cart` | 8083 | 购物车条目、OpenFeign 图书校验 |
| `bookmall-stock` | 8090 | 库存查询、下单预占、取消释放 |
| `bookmall-order` | 8050 | 直接/购物车下单、订单管理、OpenFeign |
| `bookmall-payment` | 8051 | 支付单、内部模拟支付、发布支付成功事件 |

## 3. 当前基础设施能力

### 3.1 Nacos

当前已接入：

- 服务注册与发现
- Nacos Config 配置中心
- Gateway 通过 `lb://auth`、`lb://book`、`lb://cart`、`lb://stock`、`lb://order`、`lb://payment` 路由

配置脚本位于 `nacos-config/`：

- `auth.yaml`
- `book.yaml`
- `cart.yaml`
- `stock.yaml`
- `order.yaml`
- `payment.yaml`
- `gateway.yaml`

更新配置后执行：

```bash
cd nacos-config
bash publish.sh
```

### 3.2 MySQL

数据库脚本：

- `sql/sql.txt`：初始化数据库和基础表
- `sql/updates/`：增量脚本

当前已创建的表：

- `t_user`
- `t_category`
- `t_book`
- `t_order`
- `t_order_item`
- `t_user_address`
- `t_cart_item`
- `t_book_stock`
- `t_payment`

### 3.3 Redis

当前 Redis 用于 `bookmall-book`：

- Spring Cache 缓存图书详情
- 新增、修改、删除图书时清理缓存

### 3.4 Sentinel

当前 Sentinel 接在 `bookmall-book`：

- 资源名：`listBooks`
- QPS 阈值：每秒 1 次
- 超限返回 429

Windows 下如果 Sentinel 无法写 `C:\logs\csp`，启动 `bookmall-book` 时指定项目内日志目录：

```text
mvn -o -s D:\workspace_idea\BookMall\.m2\settings.xml -f D:\workspace_idea\BookMall\BookMall\pom.xml -pl bookmall-book spring-boot:run '-Dspring-boot.run.jvmArguments=-Dcsp.sentinel.log.dir=D:/workspace_idea/BookMall/logs/sentinel'
```

### 3.5 RabbitMQ

当前 RabbitMQ 用于支付和订单状态事件：

- 交换机：`bookmall.pay.success.exchange`（Topic）
- 队列：`bookmall.order.pay.success.queue`
- 路由键：`pay.success`
- 发布端：`bookmall-payment`
- 消费端：`bookmall-order`

订单侧还会发布库存确认/释放事件：

- 交换机：`bookmall.order.stock.exchange`（Topic）
- 队列：`bookmall.stock.order.paid.queue`
- 队列：`bookmall.stock.order.release.queue`
- 生产端：`bookmall-order`
- 消费端：`bookmall-stock`

RabbitMQ 连接配置位于 `nacos-config/payment.yaml`、`nacos-config/order.yaml` 和 `nacos-config/stock.yaml` 的 `spring.rabbitmq`。

启动 RabbitMQ：

```bash
docker start rabbitmq
```

支付服务和订单服务都会声明同名交换机、队列和绑定，RabbitMQ 声明是幂等的，不依赖两个服务的严格启动顺序。

### 3.6 OpenFeign

当前 `bookmall-order` 和 `bookmall-cart` 使用 OpenFeign：

- 服务名：`book`
- 调用：`GET /books/{id}`
- 分别返回订单模块、购物车模块自己的 `BookSnapshot`
- 订单服务还通过服务名 `cart` 调用 `GET /cart/selected`，读取购物车已选条目
- 订单服务通过服务名 `stock` 调用 `POST /stock/deduct`，完成下单前的库存预占
- 支付服务通过服务名 `order` 调用 `GET /orders/{id}`，完成支付前订单校验

## 4. 启动顺序

后端服务建议按以下顺序启动：

1. `bookmall-auth`（8060）
2. `bookmall-book`（8070）
3. `bookmall-cart`（8083）
4. `bookmall-stock`（8090）
5. `bookmall-order`（8050）
6. `bookmall-payment`（8051）
7. `bookmall-gateway`（8080）

常用 Maven 命令：

```text
mvn -o -s D:\workspace_idea\BookMall\.m2\settings.xml -f D:\workspace_idea\BookMall\BookMall\pom.xml -DskipTests install
mvn -o -s D:\workspace_idea\BookMall\.m2\settings.xml -f D:\workspace_idea\BookMall\BookMall\pom.xml -pl bookmall-auth spring-boot:run
```

先执行 `install` 安装公共模块，再把 `bookmall-auth` 替换为其他模块名即可启动对应服务。不要对 `spring-boot:run` 使用 `-am`，否则会尝试在父工程上找启动类。

## 5. 当前状态

本节只记录当前已实现的基础设施能力。
