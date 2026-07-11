package com.pointbank.securities.account.service;

import com.pointbank.securities.account.domain.SecuritiesAccount;
import com.pointbank.securities.account.domain.SecuritiesAccountStatus;
import com.pointbank.securities.account.mapper.SecuritiesAccountMapper;
import com.pointbank.securities.event.CashAccountCreateRequestedEvent;
import com.pointbank.securities.event.CashAccountCreatedEvent;
import com.pointbank.securities.global.exception.CustomException;
import com.pointbank.securities.global.exception.ErrorCode;
import com.pointbank.securities.infrastructure.ledger.LedgerClient;
import com.pointbank.securities.outbox.mapper.OutboxEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PendingSecuritiesAccountCleanupService {

    private final SecuritiesAccountMapper securitiesAccountMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final LedgerClient ledgerClient;

    @Transactional
    public void cleanupCreateRequestedDlq(CashAccountCreateRequestedEvent event) {
        securitiesAccountMapper.findById(event.securitiesAccountId())
                .ifPresent(this::cleanupIfPending);
    }

    @Transactional
    public void cleanupCreatedResultDlq(CashAccountCreatedEvent event) {
        SecuritiesAccount account = securitiesAccountMapper.findById(event.securitiesAccountId())
                .orElse(null);
        if (account == null) {
            ledgerClient.cleanupSecuritiesCashAccount(event.memberId());
            return;
        }
        cleanupIfPending(account);
    }

    private void cleanupIfPending(SecuritiesAccount account) {
        if (account.getStatus() == SecuritiesAccountStatus.ACTIVE
                || account.getStatus() == SecuritiesAccountStatus.SUSPENDED) {
            return;
        }
        if (account.getStatus() != SecuritiesAccountStatus.PENDING_CASH_ACCOUNT) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        ledgerClient.cleanupSecuritiesCashAccount(account.getMemberId());
        outboxEventMapper.deletePendingOrFailedByAggregate("SECURITIES_ACCOUNT", account.getId());
        securitiesAccountMapper.deleteById(account.getId());
    }
}
