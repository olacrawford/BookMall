# 遗留交易骨架基线与迁移证据

状态均为 `observed`，除非另有说明。没有运行时结果的地方不推断为可用。

## 仓库与入口

本节记录的是重构前的遗留资产，不等于最终项目的业务模型。代码目录中的 `book/order/payment` 等名称是历史实现痕迹；对外产品统一称为“电商售后智能运营平台”。

| 范围 | 事实 | 证据 |
|---|---|---|
| 后端聚合工程 | Maven 多模块，Java 17；Spring Boot 3.2.5、Spring Cloud 2023.0.2、Alibaba 2023.0.1.0；作为重构底座 | `BookMall/pom.xml:16-31` |
| 服务模块 | auth 8060、book 8070、cart 8083、stock 8090、order 8050、payment 8051、gateway 8080，加 common | `BookMall/pom.xml:12-21`；各模块 `*Application.java` |
| 前端 | Vue 3 + Vite，保留少量旧交易页面作为迁移参考 | `front/src/views/`；`front/package.json` |
| 配置 | 端口和 Nacos 导入在服务 `application.yml`，数据库/Redis/MQ 在 `nacos-config/*.yaml` | `BookMall/*/src/main/resources/application.yml`、`nacos-config/` |

## 服务能力

| 模块 | 主要入口/调用方 | 存储与外部依赖 | 已观测能力和缺口 | 证据 |
|---|---|---|---|---|
| auth | `AuthController`、`AddressController`；网关路由 | MySQL、JWT、BCrypt | 注册、登录、地址；无运营角色模型 | `bookmall-auth/.../controller/` |
| 商品目录 | 历史 `BookController` | MySQL、Redis、Sentinel | 仅迁移商品查询、价格和类目等通用能力；旧图书语义不进入最终产品 | `bookmall-book/.../BookController.java`、`RedisConfig.java` |
| cart | `CartController`；order 通过 Feign 调用 | MySQL、Feign、LoadBalancer | 加购、勾选、数量、删除；用户维度唯一约束 | `bookmall-cart/.../CartServiceImpl.java`、`sql/updates/001...sql` |
| stock | `StockController`；order/payment 调用，RabbitMQ 消费 | MySQL、RabbitMQ | 预占、确认、释放；需补售后入库/重发语义 | `bookmall-stock/.../StockServiceImpl.java`、`OrderStockConsumer.java` |
| order | `OrderController`；book/cart/stock Feign，RabbitMQ 发布/消费 | MySQL、RabbitMQ、定时任务 | 下单、订单查询、支付确认、超时取消；已有补偿但没有售后域 | `bookmall-order/.../OrderServiceImpl.java:68-197` |
| payment | `PaymentController`；order Feign，发布支付成功事件 | MySQL、RabbitMQ | 内部模拟支付、按订单幂等；没有退款记录/退款状态 | `bookmall-payment/.../PaymentServiceImpl.java:38-63` |
| gateway | `AuthGlobalFilter`；浏览器入口 | JWT、Nacos | JWT 校验、透传可信 `X-User-Id`；无售后/运营路由 | `bookmall-gateway/.../AuthGlobalFilter.java` |

## 数据基线

当前初始化/增量脚本覆盖用户、地址、商品目录、购物车、库存、订单、订单明细和支付等遗留表。重构只迁移订单金额/归属、支付状态、库存预占等通用约束；商品名称、图书分类等领域细节将由售后领域重新命名和建模。

| 业务事实 | 证据 |
|---|---|
| 下单先远程预占库存，本地订单异常时发布释放事件 | `bookmall-order/.../OrderServiceImpl.java:80-94,190-196` |
| 订单明细保存标题和价格快照 | `OrderServiceImpl.java:161-171` |
| 支付以订单服务返回金额为准，已支付记录直接返回 | `bookmall-payment/.../PaymentServiceImpl.java:40-63` |
| 订单过期扫描和状态索引已存在 | `sql/updates/004_order_expire_stock_confirm.sql:7-14` |
| RabbitMQ、Redis、Nacos、MySQL 有 Docker 基础设施 | `docker-compose.infra.yml:7-81` |

## 测试与部署

目前存在 auth、商品目录、cart、gateway、order、payment、stock 7 组遗留测试文件，未发现售后/AI 测试。根工程建议命令是 `mvn -f BookMall/pom.xml -q test`；基础设施命令是 `docker compose -f docker-compose.infra.yml up -d`。测试是否在本机通过需要在实施阶段实际记录，当前不宣称 `verified`。

## 已观测问题

1. 业务对象只有交易域，没有售后单、工单、规则版本、审批、审计和流程步骤。
2. 支付有“支付成功”事件，但没有退款/补偿业务事件和统一幂等契约。
3. 网关只有用户身份透传，没有运营角色和售后资源级授权。
4. 没有统一 `trace_id`，无法把 AI、工具、业务和审计串起来。
5. 现有文档中的 Stage 01-08 全部标为待开始；它们是规划，不是实现证据。
6. 重构目标不是给旧商城“加一个售后页面”，而是用售后领域重新组织业务、数据和流程；旧模块只作为迁移适配器和回归样本。
