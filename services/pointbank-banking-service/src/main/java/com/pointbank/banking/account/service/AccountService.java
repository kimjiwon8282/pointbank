package com.pointbank.banking.account.service;

import com.pointbank.banking.account.dto.AccountCreateRequest;
import com.pointbank.banking.account.dto.AccountDepositRequest;
import com.pointbank.banking.account.dto.AccountDepositResponse;
import com.pointbank.banking.account.dto.AccountResponse;
import com.pointbank.banking.ledger.LedgerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final LedgerClient ledgerClient;

    public AccountResponse create(Long memberId, AccountCreateRequest request) {
        return ledgerClient.createAccount(memberId, request);
    }

    public AccountResponse getMine(Long memberId) {
        return ledgerClient.getAccount(memberId);
    }

    public AccountDepositResponse deposit(Long memberId, String idempotencyKey, AccountDepositRequest request) {
        return ledgerClient.deposit(memberId, idempotencyKey, request);
    }

    public AccountDepositResponse deposit(Long memberId, AccountDepositRequest request) {
        return ledgerClient.deposit(memberId, java.util.UUID.randomUUID().toString(), request);
    }
}
