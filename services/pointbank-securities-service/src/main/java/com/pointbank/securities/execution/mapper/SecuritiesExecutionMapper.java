package com.pointbank.securities.execution.mapper;

import com.pointbank.securities.execution.domain.SecuritiesExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface SecuritiesExecutionMapper {
    int insert(SecuritiesExecution execution);
    Optional<SecuritiesExecution> findByOrderId(Long orderId);
    List<SecuritiesExecution> findExecutionsByMemberIdOrderByExecutedAtDesc(
            @Param("memberId") Long memberId,
            @Param("limit") int limit
    );
    Long sumRealizedProfitByMemberIdAndPeriod(
            @Param("memberId") Long memberId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
