package com.pointbank.banking.funds.mapper;

import com.pointbank.banking.funds.domain.FundTransferRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface FundTransferRequestMapper {
    int insertRequested(FundTransferRequest request);
    Optional<FundTransferRequest> findByRequestNo(String requestNo);
    int complete(
            @Param("id") Long id,
            @Param("sourceAccountId") Long sourceAccountId,
            @Param("targetAccountId") Long targetAccountId,
            @Param("bankingBalanceAfter") long bankingBalanceAfter,
            @Param("completedAt") LocalDateTime completedAt
    );
}
