package com.pointbank.ledger.message.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.ledger.account.domain.LedgerAccount;
import com.pointbank.ledger.account.domain.LedgerAccountStatus;
import com.pointbank.ledger.account.domain.LedgerAccountType;
import com.pointbank.ledger.account.mapper.LedgerAccountMapper;
import com.pointbank.ledger.event.CashAccountCreateRequestedEvent;
import com.pointbank.ledger.event.CashAccountCreatedEvent;
import com.pointbank.ledger.event.LedgerEventType;
import com.pointbank.ledger.global.exception.CustomException;
import com.pointbank.ledger.global.exception.ErrorCode;
import com.pointbank.ledger.message.mapper.ProcessedMessageMapper;
import com.pointbank.ledger.outbox.domain.OutboxEvent;
import com.pointbank.ledger.outbox.domain.OutboxEventStatus;
import com.pointbank.ledger.outbox.mapper.OutboxEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerCommandHandler {

    private final ProcessedMessageMapper processedMessageMapper;
    private final LedgerAccountMapper accountMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(CashAccountCreateRequestedEvent event) {
        if (processedMessageMapper.existsByEventId(event.eventId())) {
            return;
        }

        LedgerAccount account = accountMapper.findByMemberIdAndType(
                        event.memberId(),
                        LedgerAccountType.SECURITIES_CASH
                )
                .orElseGet(() -> createSecuritiesCashAccount(event.memberId()));
        processedMessageMapper.insert(event.eventId(), event.eventType());
        outboxEventMapper.insert(createCashAccountCreatedOutboxEvent(event, account));
    }

    private LedgerAccount createSecuritiesCashAccount(Long memberId) {
        LedgerAccount account = new LedgerAccount(
                memberId,
                LedgerAccountType.SECURITIES_CASH,
                null,
                null,
                0L,
                0L,
                LedgerAccountStatus.ACTIVE
        );
        try {
            accountMapper.insert(account);
            return account;
        } catch (DuplicateKeyException exception) {
            return accountMapper.findByMemberIdAndType(memberId, LedgerAccountType.SECURITIES_CASH)
                    .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    private OutboxEvent createCashAccountCreatedOutboxEvent(
            CashAccountCreateRequestedEvent sourceEvent,
            LedgerAccount account
    ) {
        String eventId = UUID.randomUUID().toString();
        CashAccountCreatedEvent event = new CashAccountCreatedEvent(
                eventId,
                LedgerEventType.CASH_ACCOUNT_CREATED,
                sourceEvent.eventId(),
                sourceEvent.memberId(),
                sourceEvent.securitiesAccountId(),
                account.getId(),
                LocalDateTime.now()
        );
        try {
            return new OutboxEvent(
                    eventId,
                    LedgerEventType.CASH_ACCOUNT_CREATED,
                    "LEDGER_ACCOUNT",
                    account.getId(),
                    objectMapper.writeValueAsString(event),
                    OutboxEventStatus.PENDING
            );
        } catch (JsonProcessingException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
