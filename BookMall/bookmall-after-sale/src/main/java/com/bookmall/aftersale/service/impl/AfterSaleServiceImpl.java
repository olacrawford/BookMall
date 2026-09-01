package com.bookmall.aftersale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.aftersale.client.LogisticsQueryGateway;
import com.bookmall.aftersale.client.LogisticsQueryResult;
import com.bookmall.aftersale.client.OrderClient;
import com.bookmall.aftersale.client.dto.OrderSnapshot;
import com.bookmall.aftersale.ai.model.AiAnalysisResponse;
import com.bookmall.aftersale.ai.model.TicketContext;
import com.bookmall.aftersale.ai.service.AfterSaleAiAnalysisService;
import com.bookmall.aftersale.dto.AfterSaleCreateRequest;
import com.bookmall.aftersale.dto.ApprovalRequest;
import com.bookmall.aftersale.dto.PolicyDecisionRequest;
import com.bookmall.aftersale.dto.RefundRequest;
import com.bookmall.aftersale.entity.AfterSaleOrder;
import com.bookmall.aftersale.entity.AfterSaleTicket;
import com.bookmall.aftersale.entity.ApprovalTask;
import com.bookmall.aftersale.entity.AuditLog;
import com.bookmall.aftersale.entity.RefundRecord;
import com.bookmall.aftersale.entity.WorkflowInstance;
import com.bookmall.aftersale.entity.WorkflowStep;
import com.bookmall.aftersale.mapper.AfterSaleOrderMapper;
import com.bookmall.aftersale.mapper.AfterSaleTicketMapper;
import com.bookmall.aftersale.mapper.ApprovalTaskMapper;
import com.bookmall.aftersale.mapper.AuditLogMapper;
import com.bookmall.aftersale.mapper.RefundRecordMapper;
import com.bookmall.aftersale.mapper.WorkflowInstanceMapper;
import com.bookmall.aftersale.mapper.WorkflowStepMapper;
import com.bookmall.aftersale.service.AfterSaleAccessGuard;
import com.bookmall.aftersale.service.AfterSaleOutboxService;
import com.bookmall.aftersale.service.AfterSaleService;
import com.bookmall.aftersale.service.AfterSaleStatusMachine;
import com.bookmall.aftersale.service.LogisticsRecoveryService;
import com.bookmall.aftersale.service.PolicyService;
import com.bookmall.aftersale.vo.AfterSaleDetailVO;
import com.bookmall.aftersale.vo.AfterSaleVO;
import com.bookmall.aftersale.vo.ApprovalQueueVO;
import com.bookmall.aftersale.vo.ApprovalTaskVO;
import com.bookmall.aftersale.vo.PolicyDecision;
import com.bookmall.aftersale.vo.RefundVO;
import com.bookmall.common.exception.BusinessException;
import com.bookmall.common.result.Result;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AfterSaleServiceImpl implements AfterSaleService {

    private final AfterSaleOrderMapper afterSaleOrderMapper;
    private final AfterSaleTicketMapper afterSaleTicketMapper;
    private final WorkflowInstanceMapper workflowInstanceMapper;
    private final WorkflowStepMapper workflowStepMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final ApprovalTaskMapper approvalTaskMapper;
    private final AuditLogMapper auditLogMapper;
    private final AfterSaleStatusMachine statusMachine;
    private final PolicyService policyService;
    private final OrderClient orderClient;
    private final AfterSaleAccessGuard accessGuard;
    private final AfterSaleOutboxService outboxService;
    private final LogisticsRecoveryService logisticsRecoveryService;
    private final LogisticsQueryGateway logisticsQueryGateway;
    private final AfterSaleAiAnalysisService aiAnalysisService;

    public AfterSaleServiceImpl(AfterSaleOrderMapper afterSaleOrderMapper,
                                AfterSaleTicketMapper afterSaleTicketMapper,
                                WorkflowInstanceMapper workflowInstanceMapper,
                                WorkflowStepMapper workflowStepMapper,
                                RefundRecordMapper refundRecordMapper,
                                ApprovalTaskMapper approvalTaskMapper,
                                AuditLogMapper auditLogMapper,
                                AfterSaleStatusMachine statusMachine,
                                PolicyService policyService,
                                OrderClient orderClient,
                                AfterSaleAccessGuard accessGuard,
                                AfterSaleOutboxService outboxService,
                                LogisticsRecoveryService logisticsRecoveryService,
                                LogisticsQueryGateway logisticsQueryGateway,
                                AfterSaleAiAnalysisService aiAnalysisService) {
        this.afterSaleOrderMapper = afterSaleOrderMapper;
        this.afterSaleTicketMapper = afterSaleTicketMapper;
        this.workflowInstanceMapper = workflowInstanceMapper;
        this.workflowStepMapper = workflowStepMapper;
        this.refundRecordMapper = refundRecordMapper;
        this.approvalTaskMapper = approvalTaskMapper;
        this.auditLogMapper = auditLogMapper;
        this.statusMachine = statusMachine;
        this.policyService = policyService;
        this.orderClient = orderClient;
        this.accessGuard = accessGuard;
        this.outboxService = outboxService;
        this.logisticsRecoveryService = logisticsRecoveryService;
        this.logisticsQueryGateway = logisticsQueryGateway;
        this.aiAnalysisService = aiAnalysisService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSaleDetailVO createAfterSale(Long userId, AfterSaleCreateRequest request) {
        requireUser(userId);
        LocalDateTime now = LocalDateTime.now();
        String traceId = UUID.randomUUID().toString();
        String idempotencyKey = hasText(request.getIdempotencyKey()) ? request.getIdempotencyKey() : "AS-ID-" + UUID.randomUUID();

        AfterSaleOrder existing = findByKey(idempotencyKey);
        if (existing != null) {
            return detailFor(existing, userId, null);
        }

        OrderSnapshot snapshot = requireOrderSnapshot(userId, request.getOrderId());
        BigDecimal amount = snapshot.getTotalAmount() != null
                ? snapshot.getTotalAmount()
                : (request.getAmount() == null ? BigDecimal.ZERO : request.getAmount());

        PolicyDecisionRequest policyRequest = new PolicyDecisionRequest();
        policyRequest.setAmount(amount);
        policyRequest.setRiskLevel(hasText(request.getRiskLevel()) ? request.getRiskLevel() : "LOW");
        PolicyDecision decision = policyService.evaluate(policyRequest);

        AfterSaleOrder order = new AfterSaleOrder();
        order.setAfterSaleNo(nextNo("AS"));
        order.setOrderId(request.getOrderId());
        order.setOrderNo(hasText(request.getOrderNo()) ? request.getOrderNo() : snapshot.getOrderNo());
        order.setUserId(userId);
        order.setType(request.getType());
        order.setStatus(AfterSaleStatusMachine.CREATED);
        order.setAmount(amount);
        order.setPolicyVersion("v1");
        order.setIdempotencyKey(idempotencyKey);
        order.setVersion(0);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        try {
            afterSaleOrderMapper.insert(order);
        } catch (DuplicateKeyException ex) {
            AfterSaleOrder duplicate = findByKey(idempotencyKey);
            if (duplicate == null) {
                throw ex;
            }
            return detailFor(duplicate, userId, decision.getAction());
        }

        order.setStatus(statusMachine.transition(order.getStatus(), AfterSaleStatusMachine.UNDER_REVIEW));
        order.setUpdateTime(now);
        afterSaleOrderMapper.updateById(order);

        AfterSaleTicket ticket = new AfterSaleTicket();
        ticket.setTicketNo(nextNo("TK"));
        ticket.setAfterSaleId(order.getId());
        ticket.setUserId(userId);
        ticket.setDescription(request.getDescription());
        ticket.setDecisionStatus("PENDING");
        ticket.setTraceId(traceId);
        ticket.setCreateTime(now);
        ticket.setUpdateTime(now);
        afterSaleTicketMapper.insert(ticket);

        runAiAnalysis(order, ticket, snapshot, request.getDescription(), request.getEvidence(), traceId, now);

        WorkflowInstance workflow = new WorkflowInstance();
        workflow.setWorkflowNo(nextNo("WF"));
        workflow.setTicketId(ticket.getId());
        workflow.setWorkflowType(request.getType());
        workflow.setStatus("RUNNING");
        workflow.setCurrentStep("UNDER_REVIEW");
        workflow.setWorkflowVersion("v1");
        workflow.setVersion(0);
        workflow.setCreateTime(now);
        workflow.setUpdateTime(now);
        workflowInstanceMapper.insert(workflow);

        insertStep(workflow.getId(), "CREATE_AFTER_SALE", "COMPLETED", 1, now);
        insertStep(workflow.getId(), "LOGISTICS_CHECK", "RUNNING", 0, now);
        LogisticsQueryResult logisticsResult = logisticsQueryGateway.query(order.getId(), order.getOrderId());
        if (!logisticsResult.available()) {
            logisticsRecoveryService.degradeToHuman(order, workflow, now);
            ticket.setDecisionStatus("WAITING_HUMAN");
            ticket.setUpdateTime(now);
            afterSaleTicketMapper.updateById(ticket);
            writeAudit(traceId, ticket.getId(), order.getId(), "SYSTEM", null, "LOGISTICS_TIMEOUT",
                    order.getStatus(), AfterSaleStatusMachine.WAITING_HUMAN, now);
            return toDetailVO(order, ticket, workflow, "WAITING_HUMAN");
        }
        logisticsRecoveryService.completeLogisticsCheck(workflow.getId(), now);
        insertStep(workflow.getId(), "POLICY_CHECK", "COMPLETED", 1, now);

        String beforeStatus = order.getStatus();
        String afterStatus;
        if (PolicyDecision.AUTO_REFUND.equals(decision.getAction())) {
            order.setStatus(statusMachine.transition(order.getStatus(), AfterSaleStatusMachine.AUTO_HANDLED));
            order.setUpdateTime(now);
            afterSaleOrderMapper.updateById(order);
            completeRefund(order, workflow, userId, "AUTO-" + order.getAfterSaleNo(), now);
            ticket.setDecisionStatus("APPROVED");
            ticket.setUpdateTime(now);
            afterSaleTicketMapper.updateById(ticket);
            afterStatus = order.getStatus();
        } else if (PolicyDecision.REQUIRE_APPROVAL.equals(decision.getAction())) {
            afterStatus = statusMachine.transition(order.getStatus(), AfterSaleStatusMachine.WAITING_APPROVAL);
            order.setStatus(afterStatus);
            order.setUpdateTime(now);
            afterSaleOrderMapper.updateById(order);
            workflow.setCurrentStep("WAITING_APPROVAL");
            workflow.setUpdateTime(now);
            workflowInstanceMapper.updateById(workflow);
            createApprovalTask(workflow.getId(), ticket.getId(), now);
            insertStep(workflow.getId(), "APPROVAL_WAIT", "PENDING", 0, now);
        } else {
            afterStatus = statusMachine.transition(order.getStatus(), AfterSaleStatusMachine.RISK_REVIEW);
            order.setStatus(afterStatus);
            order.setUpdateTime(now);
            afterSaleOrderMapper.updateById(order);
            workflow.setCurrentStep("RISK_REVIEW");
            workflow.setUpdateTime(now);
            workflowInstanceMapper.updateById(workflow);
            createApprovalTask(workflow.getId(), ticket.getId(), now);
            insertStep(workflow.getId(), "RISK_REVIEW", "PENDING", 0, now);
        }

        writeAudit(traceId, ticket.getId(), order.getId(), "USER", userId, "CREATE", beforeStatus, afterStatus, now);
        return toDetailVO(order, ticket, workflow, decision.getAction());
    }

    @Override
    public AfterSaleDetailVO getAfterSale(Long userId, Long id) {
        requireUser(userId);
        return detailFor(ownedOrder(userId, id), userId, null);
    }

    private AiAnalysisResponse runAiAnalysis(AfterSaleOrder order, AfterSaleTicket ticket, OrderSnapshot snapshot,
                                              String description, List<String> evidence, String traceId, LocalDateTime now) {
        try {
            TicketContext aiContext = new TicketContext();
            aiContext.setTraceId(traceId);
            aiContext.setTicketId(ticket.getId());
            aiContext.setUserId(order.getUserId());
            aiContext.setOrderId(order.getOrderId());
            aiContext.setDescription(description);
            aiContext.setOrderSnapshot(snapshot);
            aiContext.setPolicyVersion(order.getPolicyVersion() == null ? "v1" : order.getPolicyVersion());
            aiContext.setUserEvidence(evidence == null ? List.of() : evidence);
            AiAnalysisResponse response = aiAnalysisService.analyze(aiContext);
            ticket.setDecisionStatus("AI_REVIEWED");
            ticket.setUpdateTime(now);
            afterSaleTicketMapper.updateById(ticket);
            return response;
        } catch (Exception ex) {
            ticket.setDecisionStatus("WAITING_HUMAN");
            ticket.setUpdateTime(now);
            afterSaleTicketMapper.updateById(ticket);
            writeAudit(traceId, ticket.getId(), order.getId(), "AI", null, "AI_ANALYZE_FAILED",
                    order.getStatus(), AfterSaleStatusMachine.WAITING_HUMAN, now);
            return null;
        }
    }

    @Override
    public List<AfterSaleVO> listAfterSales(Long userId) {
        requireUser(userId);
        return afterSaleOrderMapper.selectList(new LambdaQueryWrapper<AfterSaleOrder>()
                        .eq(AfterSaleOrder::getUserId, userId)
                        .orderByDesc(AfterSaleOrder::getCreateTime))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO refund(Long userId, Long afterSaleId, RefundRequest request) {
        requireUser(userId);
        AfterSaleOrder order = ownedOrder(userId, afterSaleId);
        RefundRecord existing = refundByAfterSaleId(afterSaleId);
        if (existing != null) {
            return toRefundVO(existing);
        }

        String status = order.getStatus();
        if (AfterSaleStatusMachine.WAITING_APPROVAL.equals(status)
                || AfterSaleStatusMachine.RISK_REVIEW.equals(status)) {
            throw new BusinessException(409, "当前售后单需先完成人工审批");
        }
        if (!AfterSaleStatusMachine.PROCESSING.equals(status)
                && !AfterSaleStatusMachine.AUTO_HANDLED.equals(status)) {
            throw new BusinessException(409, "当前售后单状态不可退款");
        }

        LocalDateTime now = LocalDateTime.now();
        AfterSaleTicket ticket = ticketByAfterSaleId(afterSaleId);
        WorkflowInstance workflow = workflowByTicketId(ticket == null ? null : ticket.getId());
        RefundRecord refund = getOrCreateRefund(order, userId, request.getRefundKey(), now);
        completeRefundState(order, workflow, now);
        outboxService.recordRefundExecuted(afterSaleId, refund.getRefundNo(), refund.getAmount());
        writeAudit(UUID.randomUUID().toString(), ticket == null ? null : ticket.getId(), afterSaleId, "USER", userId, "REFUND", status, order.getStatus(), now);
        return toRefundVO(refund);
    }

    @Override
    public ApprovalQueueVO listApprovalTasks(String status) {
        List<ApprovalTaskVO> items = approvalTaskMapper.selectList(new LambdaQueryWrapper<ApprovalTask>()
                        .eq(hasText(status), ApprovalTask::getStatus, status)
                        .orderByAsc(ApprovalTask::getCreateTime))
                .stream()
                .map(this::toApprovalTaskVO)
                .toList();
        return new ApprovalQueueVO(items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long operatorId, Long taskId, ApprovalRequest request) {
        requireUser(operatorId);
        ApprovalTask task = approvalTask(taskId);
        if ("APPROVED".equals(task.getStatus())) {
            return;
        }
        if (!"WAITING".equals(task.getStatus())) {
            throw new BusinessException(409, "审批任务已处理");
        }

        AfterSaleOrder order = orderForApprovalTask(task);
        String beforeStatus = order.getStatus();
        if (!AfterSaleStatusMachine.WAITING_APPROVAL.equals(beforeStatus)
                && !AfterSaleStatusMachine.RISK_REVIEW.equals(beforeStatus)) {
            throw new BusinessException(409, "当前售后单不在待审批状态");
        }

        LocalDateTime now = LocalDateTime.now();
        String afterStatus = statusMachine.transition(beforeStatus, AfterSaleStatusMachine.PROCESSING);
        order.setStatus(afterStatus);
        order.setUpdateTime(now);
        afterSaleOrderMapper.updateById(order);

        task.setStatus("APPROVED");
        task.setDecision("APPROVED");
        task.setComment(request == null ? null : request.getComment());
        task.setDecidedAt(now);
        task.setUpdateTime(now);
        approvalTaskMapper.updateById(task);

        AfterSaleTicket ticket = afterSaleTicketMapper.selectById(task.getTicketId());
        WorkflowInstance workflow = workflowByTicketId(task.getTicketId());
        if (workflow != null) {
            workflow.setCurrentStep("PROCESSING");
            workflow.setUpdateTime(now);
            workflowInstanceMapper.updateById(workflow);
            insertStep(workflow.getId(), "APPROVAL_APPROVED", "COMPLETED", 1, now);
        }
        writeAudit(UUID.randomUUID().toString(), ticket == null ? null : ticket.getId(), order.getId(), "OPERATOR", operatorId, "APPROVE", beforeStatus, afterStatus, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long operatorId, Long taskId, ApprovalRequest request) {
        requireUser(operatorId);
        ApprovalTask task = approvalTask(taskId);
        if ("REJECTED".equals(task.getStatus())) {
            return;
        }
        if (!"WAITING".equals(task.getStatus())) {
            throw new BusinessException(409, "审批任务已处理");
        }

        AfterSaleOrder order = orderForApprovalTask(task);
        String beforeStatus = order.getStatus();
        if (!AfterSaleStatusMachine.WAITING_APPROVAL.equals(beforeStatus)
                && !AfterSaleStatusMachine.RISK_REVIEW.equals(beforeStatus)) {
            throw new BusinessException(409, "当前售后单不在待审批状态");
        }

        LocalDateTime now = LocalDateTime.now();
        String afterStatus = statusMachine.transition(beforeStatus, AfterSaleStatusMachine.REJECTED);
        order.setStatus(afterStatus);
        order.setUpdateTime(now);
        afterSaleOrderMapper.updateById(order);

        task.setStatus("REJECTED");
        task.setDecision("REJECTED");
        task.setComment(request == null ? null : request.getComment());
        task.setDecidedAt(now);
        task.setUpdateTime(now);
        approvalTaskMapper.updateById(task);

        AfterSaleTicket ticket = afterSaleTicketMapper.selectById(task.getTicketId());
        WorkflowInstance workflow = workflowByTicketId(task.getTicketId());
        if (workflow != null) {
            workflow.setStatus("COMPLETED");
            workflow.setCurrentStep("REJECTED");
            workflow.setUpdateTime(now);
            workflowInstanceMapper.updateById(workflow);
            insertStep(workflow.getId(), "APPROVAL_REJECTED", "COMPLETED", 1, now);
        }
        writeAudit(UUID.randomUUID().toString(), ticket == null ? null : ticket.getId(), order.getId(), "OPERATOR", operatorId, "REJECT", beforeStatus, afterStatus, now);
    }

    private OrderSnapshot requireOrderSnapshot(Long userId, Long orderId) {
        if (orderId == null) {
            throw new BusinessException(400, "orderId 不能为空");
        }
        Result<OrderSnapshot> result;
        try {
            result = orderClient.getOrderDetail(orderId, userId);
        } catch (Exception ex) {
            throw new BusinessException(503, "订单服务暂不可用");
        }
        if (result == null || !Integer.valueOf(200).equals(result.getCode()) || result.getData() == null) {
            String message = result == null || result.getMessage() == null ? "订单不存在" : result.getMessage();
            throw new BusinessException(result == null ? 503 : result.getCode(), message);
        }
        if (result.getData().getUserId() == null || !result.getData().getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该订单");
        }
        return result.getData();
    }

    private AfterSaleDetailVO detailFor(AfterSaleOrder order, Long userId, String policyAction) {
        ownedOrder(userId, order.getId());
        AfterSaleTicket ticket = ticketByAfterSaleId(order.getId());
        return toDetailVO(order, ticket, workflowByTicketId(ticket == null ? null : ticket.getId()), policyAction);
    }

    private AfterSaleOrder orderForApprovalTask(ApprovalTask task) {
        AfterSaleTicket ticket = afterSaleTicketMapper.selectById(task.getTicketId());
        if (ticket == null) {
            throw new BusinessException(404, "审批任务关联的售后单不存在");
        }
        AfterSaleOrder order = afterSaleOrderMapper.selectById(ticket.getAfterSaleId());
        if (order == null) {
            throw new BusinessException(404, "审批任务关联的售后单不存在");
        }
        return order;
    }

    private ApprovalTask approvalTask(Long taskId) {
        ApprovalTask task = approvalTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "审批任务不存在");
        }
        return task;
    }

    private AfterSaleTicket ticketByAfterSaleId(Long afterSaleId) {
        return afterSaleTicketMapper.selectOne(new LambdaQueryWrapper<AfterSaleTicket>()
                .eq(AfterSaleTicket::getAfterSaleId, afterSaleId));
    }

    private WorkflowInstance workflowByTicketId(Long ticketId) {
        if (ticketId == null) {
            return null;
        }
        return workflowInstanceMapper.selectOne(new LambdaQueryWrapper<WorkflowInstance>()
                .eq(WorkflowInstance::getTicketId, ticketId));
    }

    private AfterSaleOrder findByKey(String idempotencyKey) {
        return afterSaleOrderMapper.selectOne(new LambdaQueryWrapper<AfterSaleOrder>()
                .eq(AfterSaleOrder::getIdempotencyKey, idempotencyKey));
    }

    private RefundRecord refundByAfterSaleId(Long afterSaleId) {
        return refundRecordMapper.selectOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getAfterSaleId, afterSaleId)
                .last("LIMIT 1"));
    }

    private RefundRecord refundByKey(String refundKey) {
        return refundRecordMapper.selectOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getIdempotencyKey, refundKey));
    }

    private void completeRefund(AfterSaleOrder order, WorkflowInstance workflow, Long userId, String refundKey, LocalDateTime now) {
        RefundRecord refund = getOrCreateRefund(order, userId, refundKey, now);
        completeRefundState(order, workflow, now);
        outboxService.recordRefundExecuted(order.getId(), refund.getRefundNo(), refund.getAmount());
    }

    private void completeRefundState(AfterSaleOrder order, WorkflowInstance workflow, LocalDateTime now) {
        String current = order.getStatus();
        if (AfterSaleStatusMachine.AUTO_HANDLED.equals(current)) {
            current = statusMachine.transition(current, AfterSaleStatusMachine.PROCESSING);
            order.setStatus(current);
            order.setUpdateTime(now);
            afterSaleOrderMapper.updateById(order);
        }
        if (AfterSaleStatusMachine.PROCESSING.equals(current)) {
            current = statusMachine.transition(current, AfterSaleStatusMachine.COMPLETED);
            order.setStatus(current);
            order.setUpdateTime(now);
            afterSaleOrderMapper.updateById(order);
        } else if (!AfterSaleStatusMachine.COMPLETED.equals(current)) {
            throw new BusinessException(409, "当前售后单状态不可退款");
        }
        if (workflow != null) {
            workflow.setStatus("COMPLETED");
            workflow.setCurrentStep("COMPLETED");
            workflow.setUpdateTime(now);
            workflowInstanceMapper.updateById(workflow);
            insertStep(workflow.getId(), "REFUND_COMPLETED", "COMPLETED", 1, now);
        }
    }

    private RefundRecord getOrCreateRefund(AfterSaleOrder order, Long userId, String refundKey, LocalDateTime now) {
        RefundRecord existing = refundByAfterSaleId(order.getId());
        if (existing != null) {
            return existing;
        }
        String effectiveKey = hasText(refundKey) ? refundKey : "RF-ID-" + order.getId();
        existing = refundByKey(effectiveKey);
        if (existing != null) {
            return existing;
        }

        RefundRecord refund = new RefundRecord();
        refund.setRefundNo(nextNo("RF"));
        refund.setAfterSaleId(order.getId());
        refund.setOrderId(order.getOrderId());
        refund.setUserId(userId);
        refund.setAmount(order.getAmount() == null ? BigDecimal.ZERO : order.getAmount());
        refund.setStatus("SUCCESS");
        refund.setIdempotencyKey(effectiveKey);
        refund.setProviderRef("MOCK");
        refund.setCreateTime(now);
        refund.setUpdateTime(now);
        try {
            refundRecordMapper.insert(refund);
        } catch (DuplicateKeyException ex) {
            RefundRecord duplicate = refundByKey(effectiveKey);
            if (duplicate == null) {
                throw ex;
            }
            return duplicate;
        }
        return refund;
    }

    private void createApprovalTask(Long workflowId, Long ticketId, LocalDateTime now) {
        ApprovalTask task = new ApprovalTask();
        task.setTaskNo(nextNo("AP"));
        task.setWorkflowId(workflowId);
        task.setTicketId(ticketId);
        task.setStatus("WAITING");
        task.setVersion(0);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        approvalTaskMapper.insert(task);
    }

    private void insertStep(Long workflowId, String stepKey, String status, int attemptCount, LocalDateTime now) {
        WorkflowStep step = new WorkflowStep();
        step.setWorkflowId(workflowId);
        step.setStepKey(stepKey);
        step.setStatus(status);
        step.setAttemptCount(attemptCount);
        step.setVersion(0);
        step.setCreateTime(now);
        step.setUpdateTime(now);
        workflowStepMapper.insert(step);
    }

    private void writeAudit(String traceId, Long ticketId, Long afterSaleId,
                            String operatorType, Long operatorId, String action,
                            String beforeStatus, String afterStatus, LocalDateTime now) {
        AuditLog audit = new AuditLog();
        audit.setTraceId(traceId);
        audit.setTicketId(ticketId);
        audit.setAfterSaleId(afterSaleId);
        audit.setOperatorType(operatorType);
        audit.setOperatorId(operatorId);
        audit.setAction(action);
        audit.setBeforeStatus(beforeStatus);
        audit.setAfterStatus(afterStatus);
        audit.setCreateTime(now);
        auditLogMapper.insert(audit);
    }

    private AfterSaleOrder ownedOrder(Long userId, Long id) {
        AfterSaleOrder order = afterSaleOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "售后单不存在");
        }
        accessGuard.requireOwner(order, userId);
        return order;
    }

    private ApprovalTaskVO toApprovalTaskVO(ApprovalTask task) {
        ApprovalTaskVO vo = new ApprovalTaskVO();
        vo.setId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setStatus(task.getStatus());
        vo.setDecision(task.getDecision());
        vo.setComment(task.getComment());
        vo.setCreateTime(task.getCreateTime());
        vo.setDecidedAt(task.getDecidedAt());
        AfterSaleTicket ticket = afterSaleTicketMapper.selectById(task.getTicketId());
        if (ticket != null) {
            AfterSaleOrder order = afterSaleOrderMapper.selectById(ticket.getAfterSaleId());
            if (order != null) {
                vo.setAfterSaleId(order.getId());
                vo.setOrderId(order.getOrderId());
                vo.setUserId(order.getUserId());
            }
        }
        return vo;
    }

    private AfterSaleVO toVO(AfterSaleOrder order) {
        AfterSaleVO vo = new AfterSaleVO();
        vo.setId(order.getId());
        vo.setAfterSaleId(order.getId());
        vo.setAfterSaleNo(order.getAfterSaleNo());
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setType(order.getType());
        vo.setStatus(order.getStatus());
        vo.setAmount(order.getAmount());
        vo.setPolicyVersion(order.getPolicyVersion());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    private AfterSaleDetailVO toDetailVO(AfterSaleOrder order, AfterSaleTicket ticket, WorkflowInstance workflow, String policyAction) {
        AfterSaleDetailVO vo = new AfterSaleDetailVO();
        vo.setId(order.getId());
        vo.setAfterSaleId(order.getId());
        vo.setAfterSaleNo(order.getAfterSaleNo());
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setType(order.getType());
        vo.setStatus(order.getStatus());
        vo.setAmount(order.getAmount());
        vo.setPolicyVersion(order.getPolicyVersion());
        vo.setPolicyAction(policyAction);
        vo.setCreateTime(order.getCreateTime());
        if (ticket != null) {
            vo.setTicketId(ticket.getId());
            vo.setTicketNo(ticket.getTicketNo());
            vo.setDescription(ticket.getDescription());
            vo.setDecisionStatus(ticket.getDecisionStatus());
        }
        if (workflow != null) {
            vo.setWorkflowId(workflow.getId());
            vo.setWorkflowStatus(workflow.getStatus());
        }
        return vo;
    }

    private RefundVO toRefundVO(RefundRecord refund) {
        RefundVO vo = new RefundVO();
        vo.setId(refund.getId());
        vo.setRefundNo(refund.getRefundNo());
        vo.setAfterSaleId(refund.getAfterSaleId());
        vo.setOrderId(refund.getOrderId());
        vo.setAmount(refund.getAmount());
        vo.setStatus(refund.getStatus());
        vo.setIdempotencyKey(refund.getIdempotencyKey());
        vo.setCreateTime(refund.getCreateTime());
        return vo;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }


    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
    }

    private String nextNo(String prefix) {
        return prefix + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
    }
}
