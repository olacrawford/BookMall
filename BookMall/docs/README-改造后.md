# 电商售后智能运营平台

>
> 本文档描述将遗留交易骨架重构为电商售后平台后的目标形态。仓库历史模块名仅作为迁移线索保留，最终产品不以图书商城作为对外定位；后续按 `docs/阶段实施方案.md` 分阶段落地。

## 项目定位

这不是一个“AI 客服聊天框”，而是一套能实际处理售后工单的系统：

```text
用户投诉 / 订单异常
        ↓
AI 接入层：意图识别、实体抽取、证据收集
        ↓
售后工单
        ↓
业务规则 + 风险评估
        ↓
AI 决策建议
        ↓
Workflow：自动处理 / 人工审批 / 转人工
        ↓
售后执行：退款、重发、补偿、库存/订单/支付状态联动
        ↓
审计 + Trace + 结果复盘
```

项目对遗留交易骨架进行领域级重构：仅迁移用户身份、订单金额/归属、支付状态、库存预占等通用能力，重新建立售后工单、流程编排、规则版本、审批、审计、AI 编排、MCP 领域工具和售后规则知识库。商品目录、购物车等旧语义不作为最终业务主线。

核心设计主张是：

> LLM 可以提出决策，但不能拥有最终业务权限。AI 负责“理解、判断、规划”，确定性系统负责“执行、状态、权限、一致性”。

## 为什么这个形态不烂大街

| 常见 AI 项目 | 本项目形态 |
| --- | --- |
| MCP 工具箱 | 售后领域 MCP，工具只暴露受控业务能力 |
| AI 客服 | AI 售后工单中心，有状态、有流程、有审计 |
| 上传 PDF 问答 | 售后规则知识库，按规则版本和条款证据回答 |
| 多 Agent 对话 | 决策 Agent + Policy Engine + Workflow 分离 |
| Chat 演示 | 工单生命周期、审批、退款、补偿、审计闭环 |
| Prompt 展示 | 可解释、可回滚、可评估的业务规则系统 |

## 技术栈

### 现有复用

| 分类 | 技术 |
| --- | --- |
| 语言 | Java 17 |
| 基础框架 | Spring Boot 3.2.5 |
| 微服务 | Spring Cloud 2023.0.2 |
| 微服务套件 | Spring Cloud Alibaba 2023.0.1.0 |
| 注册/配置 | Nacos |
| 网关 | Spring Cloud Gateway + JWT 鉴权 |
| 远程调用 | OpenFeign |
| ORM | MyBatis-Plus + MySQL 8.x |
| 缓存 | Redis |
| 消息队列 | RabbitMQ |
| 限流 | Sentinel |
| 前端 | Vue 3 + Vite |

### 改造新增

| 能力 | 选型 |
| --- | --- |
| 售后工单/工作流/审批 | 自研领域服务 `bookmall-after-sale` |
| LLM 接入 | Spring AI 或直连 OpenAI-compatible HTTP，预留 `MockLlmClient` |
| RAG | 第一周用 MySQL 规则文档/分块表 + 关键词召回；后续接向量库 |
| MCP | 自研轻量 MCP 领域工具层，暴露订单/物流/售后/风控工具 |
| 审计链路 | `audit_log` + `X-Request-Id` + `trace_id` |
| 异步一致性 | RabbitMQ 事件 + Outbox + 幂等消费者 |
| 可观测性 | 后续接入 Prometheus / OpenTelemetry / 日志聚合 |

第一周先用 Mock LLM、Mock 物流、MySQL + Redis 跑通闭环；真实 LLM、向量库、监控和压测放到后续可选任务。

## 目标架构

```text
浏览器 / 运营端
        ↓
     API Gateway (8080)
        ↓
┌──────────────────────────────┐
│        AI 决策域              │
│  bookmall-ai (8094)          │
│  Agent + Intent + RAG + LLM  │
│  bookmall-mcp (8095)         │
│  领域工具定义与调用网关         │
└──────────────┬───────────────┘
               ↓
┌──────────────────────────────┐
│       售后业务域               │
│  bookmall-after-sale (8093)  │
│  Ticket / Policy / Risk      │
│  Workflow / Approval / Audit │
└──────────────┬───────────────┘
               ↓
┌──────────────────────────────┐
│      交易能力适配层           │
│  order / payment / stock     │
│  identity / catalog adapters  │
└──────────────────────────────┘
```

## 核心业务链路

```text
用户提交售后
  -> 创建 Ticket
  -> AI 意图识别 + 实体抽取
  -> Agent 收集证据：订单、物流、用户历史、商品规则
  -> RAG 召回售后规则条款
  -> Policy + Risk 计算
  -> 生成 AI 决策建议
  -> 低风险自动执行 / 中风险人工审批 / 高风险风控介入
  -> 售后执行：退款、重发、补偿、取消处理
  -> 更新订单、支付、库存；记录审计与 Trace
```

## 新增模块

| 模块 | 端口 | 职责 |
| --- | ---: | --- |
| `bookmall-after-sale` | 8093 | 售后工单、状态机、流程实例、审批任务、退款/补偿、审计 |
| `bookmall-ai` | 8094 | AI 编排、意图/实体、工具选择、RAG、LLM 适配 |
| `bookmall-mcp` | 8095 | MCP 工具注册、鉴权、限流、调用审计、结果标准契约 |
| `front-after-sale` | 5173 扩展 | 用户投诉、工单详情、AI 决策可视、审批工作台、审计查询 |

### 复用模块

| 模块 | 端口 | 复用方式 |
| --- | ---: | --- |
| `bookmall-gateway` | 8080 | 新增 `/after-sales/**`、`/ai/**`、运营端路由 |
| `bookmall-auth` | 8060 | 登录、注册、用户身份 |
| `bookmall-order` | 8050 | 订单查询、退款后订单状态联动 |
| `bookmall-payment` | 8051 | 支付记录、退款回冲 |
| `bookmall-stock` | 8090 | 售后入库/赔付重发相关库存处理 |
| `bookmall-book` | 8070 | 仅作为遗留商品目录适配器 |
| `bookmall-cart` | 8083 | 非核心迁移样本，最终售后主线不依赖 |

## 新增领域模型

### 第一周 MVP 表

| 表 | 用途 |
| --- | --- |
| `t_after_sale_order` | 售后单主表，关联订单 |
| `t_after_sale_ticket` | 客服工单，保存用户诉求和 AI 分析结果 |
| `t_ticket_message` | 用户/AI/运营人员消息 |
| `t_workflow_instance` | 流程实例，保存流程版本和当前节点 |
| `t_workflow_step` | 流程步骤，保存执行记录、重试次数、检查点 |
| `t_approval_task` | 人工审批任务 |
| `t_refund_record` | 退款记录，幂等键约束 |
| `t_compensation_record` | 补偿/重发记录 |
| `t_risk_record` | 风险评分与命中规则 |
| `t_policy_version` | 规则版本，绑定生效时间 |
| `t_policy_rule` | 规则明细，如金额阈值、审批策略 |
| `t_audit_log` | 关键操作审计 |
| `t_rag_document` | 售后规则文档 |
| `t_rag_chunk` | 规则文本分块 |
| `t_after_sale_outbox` | 领域事件 Outbox |

### 一个月增强表

| 表 | 用途 |
| --- | --- |
| `t_logistics_record` | Mock 物流轨迹/签收/驿站证据 |
| `t_user_risk_profile` | 用户风险画像 |
| `t_llm_inference_log` | LLM 输入输出、成本、Token、耗时 |
| `t_rag_benchmark_case` | RAG 评估用例与评分 |

## 核心设计原则

1. **AI 决策与业务执行隔离**
   AI 只输出结构化的 `Decision`，如 `{action, targetType, targetId, amount, reason, evidenceIds, policyVersion}`，不能直接调用退款/改状态能力。

2. **售后状态机**
   所有状态迁移集中定义，禁止任意跳转。例如：
   `CREATED -> UNDER_REVIEW -> APPROVAL -> PROCESSING -> COMPLETED`，允许 `CANCELED`、`REJECTED` 等终止态，状态迁移必须校验业务上下文。

3. **人工审批策略**
   退款/赔付金额、风险等级、商品类目共同决定审批门槛：
   - 低金额低风险：自动处理
   - 高危/高金额：强制人工审批
   - 疑似欺诈：进入风控

4. **规则版本化**
   每个售后单创建时绑定 `policy_version`，规则变更不回溯历史工单。

5. **幂等与最终一致**
   退款、补偿、订单状态更新、库存释放都使用唯一业务幂等键；异步链路使用 Outbox + MQ + 幂等消费。

6. **证据优先**
   AI 建议必须附带证据：订单快照、物流证据、规则条款、用户历史。运营人员可以看到“为什么这么判”。

7. **MCP 只暴露受控能力**
   Agent 通过 MCP 调用订单查询、物流查询、售后查询、风控查询等领域工具，不直接访问数据库，也不允许 Agent 工具直接执行资金操作。

## 关键 API 规划

### 用户端

- `POST /api/after-sales`：提交售后诉求
- `GET /api/after-sales/{id}`：售后单详情
- `POST /api/after-sales/{id}/messages`：追加说明/证据
- `GET /api/after-sales`：我的售后单

### 运营端

- `GET /api/after-sales/queue`：工单队列
- `GET /api/after-sales/{id}/analysis`：AI 分析与证据
- `POST /api/approval-tasks/{id}/approve`：审批通过
- `POST /api/approval-tasks/{id}/reject`：审批驳回
- `GET /api/audits`：审计查询

### AI/MCP 内部能力

- `POST /api/ai/analyze`：给定工单上下文，输出结构化 Decision
- `POST /api/mcp/tool/invoke`：按契约调用领域工具
- `GET /api/mcp/tools`：工具清单

## 第一周闭环目标

```text
用户提交“未收到但物流显示已签收”
  -> 创建售后单
  -> AI 识别“物流异常 + 退款诉求”
  -> Agent 查询订单、物流、用户历史
  -> RAG 召回赔付规则
  -> Policy + Risk 建议审批路径
  -> 自动退款 或 生成审批任务
  -> 退款成功/审批通过后更新订单与审计
  -> 前端可查看工单、AI 判断、证据和审批结果
```

一周结束后可以写出 3-4 个简历点，至少覆盖：

1. 设计并实现“AI 决策 + 业务执行”隔离的企业售后流程，AI 只产出结构化建议，退款/改状态由业务规则和 Workflow 执行。
2. 落地售后单状态机、规则版本、风险分级和人工审批，覆盖工单生命周期与权限边界。
3. 实现 MCP 领域工具层与 RAG 规则召回，AI 建议可溯源、可审计。
4. 实现退款/审批/订单状态联动的幂等与事件一致性，支撑闭环可重复执行。

## 一个月增强方向

- 接入真实 LLM Provider，内置 Mock 模式保证无 Key 可演示
- 引入向量库和 RAG 评测集
- 增加运营工作台完整视图：工单队列、证据面板、审批看板、审计报表
- 增加 OpenTelemetry 链路追踪、指标、告警
- 增加并发工单压力测试、故障恢复演练
- 沉淀系统设计文档、岗位定向面试问答手册

## 快速启动（改造完成后）

1. 启动基础设施：

```bash
docker compose -f docker-compose.infra.yml up -d
```

2. 初始化数据库：

```bash
mysql -uroot -p123456 < sql/sql.txt
mysql -uroot -p123456 < sql/updates/006_after_sale_ai.sql
```

3. 编译：

```bash
mvn -f BookMall/pom.xml -q clean package
```

4. 启动顺序：

```text
auth -> book -> cart -> stock -> order -> payment -> after-sale -> ai -> mcp -> gateway -> front
```

5. 访问：

```text
前端：http://localhost:5173
网关：http://localhost:8080
Knife4j：
  after-sale http://localhost:8093/doc.html
  ai        http://localhost:8094/doc.html
  mcp       http://localhost:8095/doc.html
```
