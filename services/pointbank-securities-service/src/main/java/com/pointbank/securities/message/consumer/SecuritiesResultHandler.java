package com.pointbank.securities.message.consumer;

import com.pointbank.securities.account.domain.SecuritiesAccount;
import com.pointbank.securities.account.domain.SecuritiesAccountStatus;
import com.pointbank.securities.account.mapper.SecuritiesAccountMapper;
import com.pointbank.securities.event.CashAccountCreatedEvent;
import com.pointbank.securities.global.exception.CustomException;
import com.pointbank.securities.global.exception.ErrorCode;
import com.pointbank.securities.message.mapper.ProcessedMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecuritiesResultHandler {

    private final ProcessedMessageMapper processedMessageMapper;
    private final SecuritiesAccountMapper securitiesAccountMapper;

    @Transactional
    public void handle(CashAccountCreatedEvent event) {
        if (processedMessageMapper.existsByEventId(event.eventId())) {
            return;
        }

        SecuritiesAccount account = securitiesAccountMapper.findById(event.securitiesAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.SECURITIES_ACCOUNT_NOT_FOUND));
        if (account.getStatus() == SecuritiesAccountStatus.PENDING_CASH_ACCOUNT) {
            securitiesAccountMapper.updateStatus(account.getId(), SecuritiesAccountStatus.ACTIVE);
        }
        processedMessageMapper.insert(event.eventId(), event.eventType());
    }
}
