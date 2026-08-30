# 证据记录模板

复制下面模板创建 `EV-*` 文件：

```markdown
### EV-XXX: 标题
- 状态: observed | implemented | verified | planned | simulation
- 结论:
- 代码来源: `path/to/file:line`
- 环境: OS / Java / Docker / 配置 profile
- 前置数据:
- 验证命令或操作:
- 预期结果:
- 实际结果:
- 关联请求/trace_id:
- 限制与后续:
```

## AI 决策记录补充

```markdown
- dataset_version:
- prompt_version:
- provider/model:
- tool_calls:
- retrieved_chunks:
- permission_filter_result:
- input_tokens/output_tokens:
- latency_ms/cost:
- human_feedback:
```

## 故障演练补充

```markdown
- fault_injection:
- affected_step:
- expected_recovery:
- actual_recovery:
- retry_count/dead_letter:
- user_visible_state:
- rollback_or_compensation:
```
