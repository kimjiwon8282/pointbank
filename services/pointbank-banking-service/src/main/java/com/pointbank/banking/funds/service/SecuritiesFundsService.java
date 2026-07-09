package com.pointbank.banking.funds.service;

import com.pointbank.banking.funds.dto.SecuritiesCashAccountResponse;
import com.pointbank.banking.funds.dto.SecuritiesDepositRequest;
import com.pointbank.banking.funds.dto.SecuritiesDepositResponse;
import com.pointbank.banking.ledger.LedgerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecuritiesFundsService {

    private final LedgerClient ledgerClient;

    public SecuritiesDepositResponse deposit(
            Long memberId,
            String idempotencyKey,
            SecuritiesDepositRequest request
    ) {
        return ledgerClient.depositSecuritiesCash(memberId, idempotencyKey, request);
    }

    public SecuritiesCashAccountResponse getMine(Long memberId) {
        return ledgerClient.getSecuritiesCashAccount(memberId);
    }
}
