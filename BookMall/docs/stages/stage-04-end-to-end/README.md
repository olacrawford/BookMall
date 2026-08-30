# Stage 04 端到端闭环执行手册

## 本阶段目标

把 Stage 02 和 Stage 03 串起来，浏览器里完成两条演示路径：

1. 低金额低风险：自动退款
2. 高金额/高风险：人工审批

## 前置依赖

- 完成 Stage 02、Stage 03
- 原有 BookMall 前端可启动
- 已读 [简历与面试](../../简历与面试.md) 中的演示顺序

## 要做的事

- [ ] 1. 在 `front/src/api/` 增加 `afterSale.js`，接口包括：
      - 创建售后
      - 查询我的售后
      - 查询售后详情
      - 提交消息
      - 查询审批队列
      - 审批通过/驳回
- [ ] 2. 增加前端页面：
      - `AfterSalesView.vue`：提交和列表
      - `AfterSalesDetailView.vue`：工单详情、AI 决策、证据、规则
      - `ApprovalQueueView.vue`：审批队列和操作
- [ ] 3. 在 `front/src/router/index.js` 增加路由，并接入登录态。
- [ ] 4. 在 Gateway 配置增加：
      - `/api/after-sales/**` -> `lb://bookmall-after-sale`
      - `/api/ai/**` -> `lb://bookmall-ai`
      - `/api/mcp/**` -> `lb://bookmall-mcp`
- [ ] 5. 在 `bookmall-after-sale` 增加完整 Controller 调用链：
      - 创建售后后调用 AI 分析
      - AI 分析结果和 Evidence 落库
      - Policy 判定自动/审批/风控
      - 自动退款或创建审批任务
      - 审批通过后触发工作流下一步
      - 每次动作写审计
- [ ] 6. 造两套种子数据：
      - 低金额订单：自动退款
      - 高金额订单：人工审批
- [ ] 7. S级：实现审批恢复工作流：
      - 审批通过 -> 流程从 `APPROVAL` -> `PROCESSING`
      - 审批驳回 -> 流程进入 `REJECTED`
- [ ] 8. S级：实现售后创建幂等：
      - 同一用户对同一订单重复提交，不允许无限创建
- [ ] 9. 手动测试两条路径，记录页面截图或 curl 结果。
- [ ] 10. 更新 `CHANGELOG.md` / `THOUGHT.md` / `PIKE.md`。

## 输出文件

```text
front/src/views/AfterSalesView.vue
front/src/views/AfterSalesDetailView.vue
front/src/views/ApprovalQueueView.vue
front/src/api/afterSale.js
bookmall-gateway 路由配置
bookmall-after-sale 分析/审批/工作流 Controller
docs/stages/stage-04-end-to-end/PIKE.md
```

## S级手写点

- 创建售后后编排 AI -> 落库 -> 规则 -> 执行
- 审批通过后恢复工作流
- 售后创建幂等判断
- 权限校验：用户只能看自己的工单

AI 可代写：

- 前端页面骨架
- 请求封装
- 种子数据
- 测试脚本初稿

## 验收清单

- [ ] 后端编译通过
- [ ] 前端能创建售后单
- [ ] 低金额售后自动完成退款
- [ ] 高金额售后进入审批队列
- [ ] 审批通过后流程继续并完成
- [ ] 审批驳回后状态为 `REJECTED`
- [ ] 同一用户重复提交同一订单不会无限创建
- [ ] 用户 A 无法看到用户 B 的售后单
- [ ] 页面展示 AI 决策、证据、规则版本
- [ ] 审计记录完整链路

## 面试沉淀

简历候选：

```text
打通用户投诉 -> AI 分析 -> 规则判定 -> 自动退款/人工审批 -> 退款 -> 审计的端到端闭环，支持低风险自动处理和人工审批恢复流程。
```

面试问题：

1. 审批通过后，退款流程怎么继续？

   回答重点：审批任务完成后调用工作流推进，不是前端再触发一遍。

2. 为什么页面展示证据？

   回答重点：AI 建议要可解释，运营人员需要看到订单、规则、风险再决定。

3. 用户越权怎么防？

   回答重点：网关透传可信 `X-User-Id`，每次查询必须校验归属。

主动讲失败场景：

```text
用户连续点击提交两次。后端用同一订单的售后幂等判断，第二次直接返回已有售后单，不创建第二个退款。
```

## 完成后下一步

进入 [Stage 05](./../stage-05-reliability/README.md) 补可靠性与最终验收。
