package com.pointbank.securities.message.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProcessedMessageMapper {
    boolean existsByEventId(String eventId);
    int insert(@Param("eventId") String eventId, @Param("messageType") String messageType);
}
