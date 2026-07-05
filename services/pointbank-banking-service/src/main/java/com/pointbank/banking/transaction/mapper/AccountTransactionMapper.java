package com.pointbank.banking.transaction.mapper;

import com.pointbank.banking.transaction.domain.AccountTransaction;
import com.pointbank.banking.transaction.domain.AccountTransactionType;
import com.pointbank.banking.transaction.dto.TransactionHistoryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AccountTransactionMapper {
    int insert(AccountTransaction transaction);

    List<TransactionHistoryRow> findHistories(
            @Param("accountId") Long accountId,
            @Param("types") List<AccountTransactionType> types,
            @Param("fromCreatedAt") LocalDateTime fromCreatedAt,
            @Param("toCreatedAtExclusive") LocalDateTime toCreatedAtExclusive,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );
}
