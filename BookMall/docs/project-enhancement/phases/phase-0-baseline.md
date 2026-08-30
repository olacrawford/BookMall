# 阶段 0：先把旧系统看明白（第 1 天上午）

## 目标与业务价值

冻结现有交易链路事实，建立可比较的编译、测试、启动和接口基线，避免把新问题误判成增强效果。

## 前置条件

Java 17、Maven、Node、Docker Desktop 可用；不要求 LLM Key。

## 今天照着做

1. 执行 `docker compose -f docker-compose.infra.yml up -d`，把容器状态保存到 `evidence/EV-001-baseline.md`。
2. 执行 `mvn -f BookMall/pom.xml -q test`，复制退出码和失败模块，不改代码“修绿”。
3. 打开 `BookMall/pom.xml`、`sql/sql.txt` 和各服务 `*Controller.java`，填完 [01-baseline.md](../01-baseline.md) 的表格。
4. 用纸或 Mermaid 画出“下单预占库存 -> 模拟支付 -> RabbitMQ 更新订单”的调用顺序。
5. 新建本阶段 `THOUGHT.md`、`PIKE.md`、`CHANGELOG.md`，只写观察到的事实。

## 修改范围

只补文档、运行记录和领域草图，不改业务代码。

## 实施切片

1. 阅读 `01-baseline.md` 并核对模块/端口/表。
2. 启动基础设施：`docker compose -f docker-compose.infra.yml up -d`。
3. 执行 `mvn -f BookMall/pom.xml -q test`，记录实际输出和失败原因。
4. 画订单创建、支付成功、库存确认调用链。
5. 产出 10 个售后规则样例和两套种子订单条件。

## 交付产物

基线记录、状态机初稿、SQL 字段草案、`evidence/EV-001`。

## 验证方案

命令退出码、服务健康地址、测试数量和已知失败必须写入证据；不得只写“已跑通”。

## 风险与回滚

基础设施启动失败时使用现有配置/日志诊断；不修改用户数据，不执行破坏性 SQL。

## 停止条件

未完成基线命令和状态迁移矩阵，不进入编码。

## 面试证据

能说明遗留交易骨架的订单预占/支付事件链路，以及为什么重构后的售后平台必须拥有独立状态、规则版本和审计。

## 必须沉淀

- `THOUGHT.md`：记录遗留资产哪些能迁移、哪些必须丢弃，以及基线中发现的三个真实风险。
- `PIKE.md`：写清“没有基线就不能证明优化”的 Problem/Insight/Decision/Evidence。
- `CHANGELOG.md`：记录仅文档和实验记录的变更。
- 面试卡片：60 秒讲清“为什么不是给旧商城加页面，而是售后领域重构”。

## 状态

`planned`
