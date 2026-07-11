package com.pointbank.securities.outbox.mapper;

import com.pointbank.securities.outbox.domain.OutboxEvent;
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
    int deletePendingOrFailedByAggregate(
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") Long aggregateId
    );
}
