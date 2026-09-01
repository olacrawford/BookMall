# CHANGELOG

## 2026-09-01

### 阶段 5 代码

- `AfterSaleController`：`POST /after-sales` 读取 `Idempotency-Key` 请求头并写入创建请求，修复请求头幂等键被忽略。
- 新增 `AfterSaleAnalysisQueryService`：`GET /after-sales/{id}/analysis` 从 `t_ai_decision`、`t_tool_call_log`、`t_ai_evidence` 返回持久化分析结果，并校验售后单归属。
- 新增 `FlexibleEvidenceDeserializer`：`evidence` 支持数组（契约）和单字符串（旧调用），避免验收脚本 500。
- `AfterSaleCreateRequest`：`evidence` 类型从 `String` 调整为 `List<String>`。
- `AfterSaleServiceImpl`：AI 分析直接接收结构化证据列表，删除字符串拆分逻辑。

### 数据与种子

- 新增 `sql/updates/010_phase5_demo_seed.sql`：固定订单 `10001`（39.80）和 `10002`（199.00），可重复执行。
- 已执行并验证：低金额售后 `1008` 自动退款，高金额售后 `1007` 审批、恢复、退款后完成。

### 测试

- 新增 `AfterSaleCreateRequestTest`：数组 evidence 与旧字符串 evidence 兼容。
- 新增 `AfterSaleAnalysisQueryServiceTest`：持久化分析返回、他人售后单 403。
- 售后模块当前 72 个测试通过，失败 0。
- 全项目 `mvn -f BookMall/pom.xml -q test` 当前 101 个测试通过，失败 0。

### 文档

- 新增阶段 5 目录：`README.md`、`THOUGHT.md`、`PIKE.md`、`CHANGELOG.md`、`EV-009.md`、`EV-010.md`、`EV-014.md`。
- 删除旧的单文件 `phase-5-production-simulation.md`。
- 同步 `00-index.md`、`真正入口.md`、`06-risk-and-verification.md`、`11-resume-first.md`、`08-interview-story.md`、`evidence-index.md`。

### Gateway/JWT 验收

- 修正 `verify-week-one.sh`：低金额和高金额原本共用同一个 `Idempotency-Key`，改为独立幂等键，脚本可完整跑通两条路径。
- 通过 8080 Gateway/JWT 注册/登录后实测：低金额 `1009` 自动退款、高金额 `1010` 审批并退款，均 `COMPLETED`；Outbox `CONSUMED`。

### 已知问题

### 前端售后控制台

- 新增 `front/src/views/AfterSaleView.vue`：我的售后单、创建售后单、详情、AI 分析和审批待办。
- `front/src/api/bookmall.js` 新增 `afterSaleApi`，创建请求带 `Idempotency-Key`；路由和侧边栏接入 `/after-sales`。
- `session.js` 保留登录态 `role`，`APPROVER` 用户可见审批队列。
- 新增 `EV-015.md`：`npm run build` 通过，Vite 代理 `/api/after-sales`、`/api/orders` 返回 200。

- 审批队列中阶段 3 的 1000 条性能种子仍保留，真实队列演示要按 `ticket_id` 定位或先清理性能数据。
- 真实 Provider、真实物流、ES/Milvus、生产级并发未验证；默认仍是 Mock 本地模拟。
