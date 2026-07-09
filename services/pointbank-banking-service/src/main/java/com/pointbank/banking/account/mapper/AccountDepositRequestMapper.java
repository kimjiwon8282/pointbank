package com.pointbank.banking.account.mapper;

import com.pointbank.banking.account.domain.AccountDepositRequestRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface AccountDepositRequestMapper {
    int insertRequested(AccountDepositRequestRecord request);

    Optional<AccountDepositRequestRecord> findByRequestNo(String requestNo);

    int complete(
            @Param("id") Long id,
            @Param("accountId") Long accountId,
            @Param("balanceAfter") long balanceAfter,
            @Param("completedAt") LocalDateTime completedAt
    );
}
