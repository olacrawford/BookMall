package com.bookmall.aftersale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.aftersale.entity.AfterSaleOutbox;
import com.bookmall.aftersale.mapper.AfterSaleOutboxMapper;
import com.bookmall.aftersale.mq.AfterSaleOutboxPublisher;
import com.bookmall.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AfterSaleOutboxService {

    private static final Logger log = LoggerFactory.getLogger(AfterSaleOutboxService.class);
    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_DISPATCHED = "DISPATCHED";
    private static final String STATUS_CONSUMED = "CONSUMED";

    private final AfterSaleOutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;
    private final AfterSaleOutboxPublisher outboxPublisher;

    public AfterSaleOutboxService(AfterSaleOutboxMapper outboxMapper,
                                  ObjectMapper objectMapper,
                                  AfterSaleOutboxPublisher outboxPublisher) {
        this.outboxMapper = outboxMapper;
        this.objectMapper = objectMapper;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public String recordRefundExecuted(Long afterSaleId, String refundNo, BigDecimal amount) {
        String eventId = "REFUND_EXECUTED-" + afterSaleId + "-" + refundNo;
        AfterSaleOutbox existing = findByEventId(eventId);
        if (existing != null) {
            return eventId;
        }

        LocalDateTime now = LocalDateTime.now();
        AfterSaleOutbox event = new AfterSaleOutbox();
        event.setEventId(eventId);
        event.setAggregateType("AFTER_SALE");
        event.setAggregateId(afterSaleId);
        event.setEventType("REFUND_EXECUTED");
        event.setPayload(toJson(afterSaleId, refundNo, amount));
        event.setStatus(STATUS_CREATED);
        event.setRetryCount(0);
        event.setNextRetryTime(now);
        event.setCreateTime(now);
        event.setUpdateTime(now);
        try {
            outboxMapper.insert(event);
        } catch (DuplicateKeyException ex) {
            return eventId;
        }
        log.info("outbox event created eventId={} afterSaleId={} eventType=REFUND_EXECUTED", eventId, afterSaleId);
        return eventId;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean consume(String eventId) {
        AfterSaleOutbox event = findByEventId(eventId);
        if (event == null) {
            return false;
        }
        if (STATUS_CONSUMED.equals(event.getStatus())) {
            log.info("outbox event already consumed eventId={}", eventId);
            return true;
        }
        if (!STATUS_DISPATCHED.equals(event.getStatus())) {
            return false;
        }
        event.setStatus(STATUS_CONSUMED);
        event.setUpdateTime(LocalDateTime.now());
        outboxMapper.updateById(event);
        log.info("outbox event consumed eventId={}", eventId);
        return true;
    }

    @Scheduled(fixedDelayString = "${after-sale.outbox.dispatch.delay-ms:10000}",
            initialDelayString = "${after-sale.outbox.dispatch.initial-delay-ms:5000}")
    public void scheduledDispatch() {
        scanAndDispatch(50);
    }

    @Transactional(rollbackFor = Exception.class)
    public int scanAndDispatch(int limit) {
        if (limit <= 0) {
            return 0;
        }
        List<AfterSaleOutbox> pending = outboxMapper.selectList(new LambdaQueryWrapper<AfterSaleOutbox>()
                .eq(AfterSaleOutbox::getStatus, STATUS_CREATED)
                .orderByAsc(AfterSaleOutbox::getCreateTime)
                .last("LIMIT " + limit));
        int dispatched = 0;
        for (AfterSaleOutbox event : pending) {
            outboxPublisher.publish(event);
            event.setStatus(STATUS_DISPATCHED);
            event.setUpdateTime(LocalDateTime.now());
            outboxMapper.updateById(event);
            log.info("outbox event dispatched eventId={}", event.getEventId());
            dispatched++;
        }
        return dispatched;
    }

    private AfterSaleOutbox findByEventId(String eventId) {
        return outboxMapper.selectOne(new LambdaQueryWrapper<AfterSaleOutbox>()
                .eq(AfterSaleOutbox::getEventId, eventId));
    }

    private String toJson(Long afterSaleId, String refundNo, BigDecimal amount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("afterSaleId", afterSaleId);
        payload.put("refundNo", refundNo);
        payload.put("amount", amount.toPlainString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "Outbox payload serialization failed");
        }
    }
}
