package com.bookmall.aftersale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.aftersale.dto.AfterSaleCreateRequest;
import com.bookmall.aftersale.dto.RefundRequest;
import com.bookmall.aftersale.entity.AfterSaleOrder;
import com.bookmall.aftersale.entity.AfterSaleTicket;
import com.bookmall.aftersale.entity.AuditLog;
import com.bookmall.aftersale.entity.RefundRecord;
import com.bookmall.aftersale.entity.WorkflowInstance;
import com.bookmall.aftersale.entity.WorkflowStep;
import com.bookmall.aftersale.mapper.AfterSaleOrderMapper;
import com.bookmall.aftersale.mapper.AfterSaleTicketMapper;
import com.bookmall.aftersale.mapper.AuditLogMapper;
import com.bookmall.aftersale.mapper.RefundRecordMapper;
import com.bookmall.aftersale.mapper.WorkflowInstanceMapper;
import com.bookmall.aftersale.mapper.WorkflowStepMapper;
import com.bookmall.aftersale.service.AfterSaleService;
import com.bookmall.aftersale.service.AfterSaleStatusMachine;
import com.bookmall.aftersale.vo.AfterSaleDetailVO;
import com.bookmall.aftersale.vo.AfterSaleVO;
import com.bookmall.aftersale.vo.RefundVO;
import com.bookmall.common.exception.BusinessException;
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
    private final AuditLogMapper auditLogMapper;
    private final AfterSaleStatusMachine statusMachine;

    public AfterSaleServiceImpl(AfterSaleOrderMapper afterSaleOrderMapper,
                                AfterSaleTicketMapper afterSaleTicketMapper,
                                WorkflowInstanceMapper workflowInstanceMapper,
                                WorkflowStepMapper workflowStepMapper,
                                RefundRecordMapper refundRecordMapper,
                                AuditLogMapper auditLogMapper,
                                AfterSaleStatusMachine statusMachine) {
        this.afterSaleOrderMapper = afterSaleOrderMapper;
        this.afterSaleTicketMapper = afterSaleTicketMapper;
        this.workflowInstanceMapper = workflowInstanceMapper;
        this.workflowStepMapper = workflowStepMapper;
        this.refundRecordMapper = refundRecordMapper;
        this.auditLogMapper = auditLogMapper;
        this.statusMachine = statusMachine;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSaleDetailVO createAfterSale(Long userId, AfterSaleCreateRequest request) {
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }

        LocalDateTime now = LocalDateTime.now();
        String traceId = UUID.randomUUID().toString();
        BigDecimal amount = request.getAmount() == null ? BigDecimal.ZERO : request.getAmount();
        String orderNo = hasText(request.getOrderNo()) ? request.getOrderNo() : "OD" + request.getOrderId();

        AfterSaleOrder order = new AfterSaleOrder();
        order.setAfterSaleNo(nextNo("AS"));
        order.setOrderId(request.getOrderId());
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setType(request.getType());
        order.setStatus(AfterSaleStatusMachine.CREATED);
        order.setAmount(amount);
        order.setPolicyVersion("v1");
        order.setIdempotencyKey(hasText(request.getIdempotencyKey()) ? request.getIdempotencyKey() : nextNo("AS-ID"));
        order.setVersion(0);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        afterSaleOrderMapper.insert(order);

        String reviewStatus = statusMachine.transition(order.getStatus(), AfterSaleStatusMachine.UNDER_REVIEW);
        order.setStatus(reviewStatus);
        order.setUpdateTime(LocalDateTime.now());
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

        WorkflowInstance workflow = new WorkflowInstance();
        workflow.setWorkflowNo(nextNo("WF"));
        workflow.setTicketId(ticket.getId());
        workflow.setWorkflowType(request.getType());
        workflow.setStatus("RUNNING");
        workflow.setCurrentStep("CREATE_AFTER_SALE");
        workflow.setWorkflowVersion("v1");
        workflow.setVersion(0);
        workflow.setCreateTime(now);
        workflow.setUpdateTime(now);
        workflowInstanceMapper.insert(workflow);

        insertStep(workflow.getId(), "CREATE_AFTER_SALE", "COMPLETED", 1, now);
        insertStep(workflow.getId(), "POLICY_CHECK", "PENDING", 0, now);

        writeAudit(traceId, ticket.getId(), order.getId(), "USER", userId,
                "CREATE", null, reviewStatus, now);

        return toDetailVO(order, ticket, workflow);
    }

    @Override
    public AfterSaleDetailVO getAfterSale(Long userId, Long id) {
        AfterSaleOrder order = ownedOrder(userId, id);
        AfterSaleTicket ticket = ticketByAfterSaleId(id);
        WorkflowInstance workflow = ticket == null ? null : workflowByTicketId(ticket.getId());
        return toDetailVO(order, ticket, workflow);
    }

    @Override
    public List<AfterSaleVO> listAfterSales(Long userId) {
        List<AfterSaleOrder> orders = afterSaleOrderMapper.selectList(
                new LambdaQueryWrapper<AfterSaleOrder>()
                        .eq(AfterSaleOrder::getUserId, userId)
                        .orderByDesc(AfterSaleOrder::getCreateTime));
        return orders.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO refund(Long userId, Long afterSaleId, RefundRequest request) {
        AfterSaleOrder order = ownedOrder(userId, afterSaleId);

        RefundRecord existing = refundRecordMapper.selectOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getAfterSaleId, afterSaleId)
                .eq(RefundRecord::getIdempotencyKey, request.getRefundKey()));
        if (existing != null) {
            return toRefundVO(existing);
        }

        LocalDateTime now = LocalDateTime.now();
        RefundRecord refund = new RefundRecord();
        refund.setRefundNo(nextNo("RF"));
        refund.setAfterSaleId(afterSaleId);
        refund.setOrderId(order.getOrderId());
        refund.setUserId(userId);
        refund.setAmount(order.getAmount() == null ? BigDecimal.ZERO : order.getAmount());
        refund.setStatus("SUCCESS");
        refund.setIdempotencyKey(request.getRefundKey());
        refund.setProviderRef("MOCK");
        refund.setCreateTime(now);
        refund.setUpdateTime(now);
        refundRecordMapper.insert(refund);

        String beforeStatus = order.getStatus();
        String afterStatus = statusMachine.transition(beforeStatus, AfterSaleStatusMachine.AUTO_HANDLED);
        order.setStatus(afterStatus);
        order.setUpdateTime(now);
        afterSaleOrderMapper.updateById(order);

        AfterSaleTicket ticket = ticketByAfterSaleId(afterSaleId);
        writeAudit(UUID.randomUUID().toString(),
                ticket == null ? null : ticket.getId(),
                afterSaleId, "USER", userId, "REFUND", beforeStatus, afterStatus, now);

        return toRefundVO(refund);
    }

    private AfterSaleOrder ownedOrder(Long userId, Long id) {
        AfterSaleOrder order = afterSaleOrderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "售后单不存在");
        }
        return order;
    }

    private AfterSaleTicket ticketByAfterSaleId(Long afterSaleId) {
        return afterSaleTicketMapper.selectOne(new LambdaQueryWrapper<AfterSaleTicket>()
                .eq(AfterSaleTicket::getAfterSaleId, afterSaleId));
    }

    private WorkflowInstance workflowByTicketId(Long ticketId) {
        return workflowInstanceMapper.selectOne(new LambdaQueryWrapper<WorkflowInstance>()
                .eq(WorkflowInstance::getTicketId, ticketId));
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

    private AfterSaleVO toVO(AfterSaleOrder order) {
        AfterSaleVO vo = new AfterSaleVO();
        vo.setId(order.getId());
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

    private AfterSaleDetailVO toDetailVO(AfterSaleOrder order, AfterSaleTicket ticket, WorkflowInstance workflow) {
        AfterSaleDetailVO vo = new AfterSaleDetailVO();
        vo.setId(order.getId());
        vo.setAfterSaleNo(order.getAfterSaleNo());
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setType(order.getType());
        vo.setStatus(order.getStatus());
        vo.setAmount(order.getAmount());
        vo.setPolicyVersion(order.getPolicyVersion());
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

    private String nextNo(String prefix) {
        return prefix + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
    }
}
