package com.pointbank.ledger.entry.mapper;

import com.pointbank.ledger.entry.domain.LedgerEntry;
import com.pointbank.ledger.entry.dto.LedgerTransactionHistoryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface LedgerEntryMapper {
    int insert(LedgerEntry entry);

    Optional<LedgerEntry> findByTransferRequestIdAndEntryType(
            @Param("transferRequestId") Long transferRequestId,
            @Param("entryType") String entryType
    );

    List<LedgerTransactionHistoryRow> findBankingHistories(
            @Param("ledgerAccountId") Long ledgerAccountId,
            @Param("types") List<String> types,
            @Param("fromCreatedAt") LocalDateTime fromCreatedAt,
            @Param("toCreatedAtExclusive") LocalDateTime toCreatedAtExclusive,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );
}
