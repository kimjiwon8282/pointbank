package com.pointbank.ledger.transfer.mapper;

import com.pointbank.ledger.transfer.domain.LedgerTransferRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface LedgerTransferRequestMapper {
    int insertRequested(LedgerTransferRequest request);
    Optional<LedgerTransferRequest> findByRequestNo(String requestNo);
    Optional<LedgerTransferRequest> findByRequestNoForUpdate(String requestNo);
    int complete(
            @Param("id") Long id,
            @Param("sourceBalanceAfter") Long sourceBalanceAfter,
            @Param("targetBalanceAfter") Long targetBalanceAfter,
            @Param("completedAt") LocalDateTime completedAt
    );
}
