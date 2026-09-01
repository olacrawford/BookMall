# CHANGELOG

## 2026-09-01

- 新增 `bookmall-after-sale` 的核心售后服务实现。
- 新增订单快照 Feign Client，用于售后创建时校验订单归属和金额。
- 新增状态机状态：`WAITING_APPROVAL`、`RISK_REVIEW`、`AUTO_HANDLED`、`PROCESSING`、`COMPLETED`、`REJECTED`、`CANCELED`、`FAILED`。
- 新增审批接口：`GET /approval-tasks`、`POST /approval-tasks/{id}/approve`、`POST /approval-tasks/{id}/reject`。
- 新增售后健康检查接口。
- 网关新增售后和审批任务路由。
- 更新售后创建请求、详情返回值和售后列表返回值。
- 补充状态机测试，覆盖新的合法/非法迁移。
- 验证结果：`mvn -f BookMall/pom.xml -pl bookmall-after-sale -am test` 通过。
