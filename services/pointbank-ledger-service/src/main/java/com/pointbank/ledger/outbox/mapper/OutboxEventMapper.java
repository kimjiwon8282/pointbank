package com.pointbank.ledger.outbox.mapper;

import com.pointbank.ledger.outbox.domain.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OutboxEventMapper {
    int insert(OutboxEvent event);
    List<OutboxEvent> findPendingByEventType(@Param("eventType") String eventType, @Param("limit") int limit);
    int markPublished(@Param("eventId") String eventId);
    int markFailed(@Param("eventId") String eventId);
    int incrementRetryCount(@Param("eventId") String eventId);
    boolean existsByEventTypeAndOrderNo(
            @Param("eventType") String eventType,
            @Param("orderNo") String orderNo
    );
    boolean existsByEventTypeOrderNoAndReasonCode(
            @Param("eventType") String eventType,
            @Param("orderNo") String orderNo,
            @Param("reasonCode") String reasonCode
    );
}
