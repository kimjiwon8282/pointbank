package com.pointbank.banking.transaction.service;

import com.pointbank.banking.ledger.LedgerClient;
import com.pointbank.banking.transaction.dto.TransactionHistoryRequest;
import com.pointbank.banking.transaction.dto.TransactionHistorySliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountTransactionQueryService {

    private final LedgerClient ledgerClient;

    public TransactionHistorySliceResponse getHistories(
            Long memberId,
            TransactionHistoryRequest request
    ) {
        return ledgerClient.getHistories(
                memberId,
                request.type(),
                request.from(),
                request.to(),
                request.cursorCreatedAt(),
                request.cursorId(),
                request.size()
        );
    }
}
