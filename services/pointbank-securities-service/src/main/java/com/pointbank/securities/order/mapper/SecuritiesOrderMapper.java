package com.pointbank.securities.order.mapper;

import com.pointbank.securities.order.domain.OrderStatus;
import com.pointbank.securities.order.domain.SecuritiesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface SecuritiesOrderMapper {
    int insertRequested(SecuritiesOrder order);
    Optional<SecuritiesOrder> findByOrderNo(String orderNo);
    Optional<SecuritiesOrder> findByOrderNoForUpdate(String orderNo);
    Optional<SecuritiesOrder> findByMemberIdAndIdempotencyKey(
            @Param("memberId") Long memberId,
            @Param("idempotencyKey") String idempotencyKey
    );
    int updateStatus(@Param("id") Long id, @Param("status") OrderStatus status);
    int markFundsCompleted(Long id);
    int markCompleted(@Param("id") Long id, @Param("completedAt") LocalDateTime completedAt);
    int markFailed(@Param("id") Long id, @Param("failureReason") String failureReason);
    int markManualReview(@Param("id") Long id, @Param("failureReason") String failureReason);
    int markCanceled(@Param("id") Long id, @Param("failureReason") String failureReason);
    int markReversed(
            @Param("id") Long id,
            @Param("failureReason") String failureReason,
            @Param("completedAt") LocalDateTime completedAt
    );
    List<SecuritiesOrder> findOrdersByMemberIdOrderByCreatedAtDesc(
            @Param("memberId") Long memberId,
            @Param("limit") int limit
    );
}
