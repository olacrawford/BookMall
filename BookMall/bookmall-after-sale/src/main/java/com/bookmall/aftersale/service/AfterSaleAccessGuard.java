package com.bookmall.aftersale.service;

import com.bookmall.aftersale.entity.AfterSaleOrder;
import com.bookmall.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AfterSaleAccessGuard {

    private final Set<Long> approverIds;
    private final Set<String> approverRoles;

    @Autowired
    public AfterSaleAccessGuard(@Value("${after-sale.approver-ids:1}") String approverIds,
                                @Value("${after-sale.approver-roles:APPROVER}") String approverRoles) {
        this.approverIds = parseApproverIds(approverIds);
        this.approverRoles = parseRoles(approverRoles);
    }

    AfterSaleAccessGuard(Set<Long> approverIds) {
        this.approverIds = approverIds;
        this.approverRoles = Set.of("APPROVER");
    }

    AfterSaleAccessGuard(Set<Long> approverIds, Set<String> approverRoles) {
        this.approverIds = approverIds;
        this.approverRoles = approverRoles;
    }

    public void requireOwner(AfterSaleOrder order, Long userId) {
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(403, "无权访问该售后单");
        }
    }

    public void requireApprover(Long operatorId) {
        requireApprover(operatorId, null);
    }

    public void requireApprover(Long operatorId, String roleHeader) {
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
        if (roleHeader != null && !roleHeader.isBlank()) {
            Set<String> roles = parseRoles(roleHeader);
            if (roles.stream().anyMatch(approverRoles::contains)) {
                return;
            }
            throw new BusinessException(403, "当前角色不能操作审批任务");
        }
        if (!approverIds.contains(operatorId)) {
            throw new BusinessException(403, "普通用户不能操作审批任务");
        }
    }

    private Set<Long> parseApproverIds(String value) {
        Set<Long> ids = new HashSet<>();
        if (value == null || value.isBlank()) {
            return ids;
        }
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .forEach(ids::add);
        return ids;
    }

    private Set<String> parseRoles(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
