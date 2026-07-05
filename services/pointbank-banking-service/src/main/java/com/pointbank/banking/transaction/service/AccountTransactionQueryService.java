package com.pointbank.banking.transaction.service;

import com.pointbank.banking.account.domain.Account;
import com.pointbank.banking.account.mapper.AccountMapper;
import com.pointbank.banking.global.exception.CustomException;
import com.pointbank.banking.global.exception.ErrorCode;
import com.pointbank.banking.transaction.domain.TransactionQueryType;
import com.pointbank.banking.transaction.dto.TransactionHistoryItemResponse;
import com.pointbank.banking.transaction.dto.TransactionHistoryRequest;
import com.pointbank.banking.transaction.dto.TransactionHistoryRow;
import com.pointbank.banking.transaction.dto.TransactionHistorySliceResponse;
import com.pointbank.banking.transaction.mapper.AccountTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountTransactionQueryService {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final AccountMapper accountMapper;
    private final AccountTransactionMapper accountTransactionMapper;

    @Transactional(readOnly = true)
    public TransactionHistorySliceResponse getHistories(
            Long memberId, TransactionHistoryRequest request) {
        Account account = accountMapper.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        TransactionQueryType queryType = request.type() == null
                ? TransactionQueryType.ALL : request.type();
        int size = request.size() == null ? DEFAULT_SIZE : request.size();
        if (size < 1 || size > MAX_SIZE) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        if ((request.cursorCreatedAt() == null) != (request.cursorId() == null)) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        LocalDate today = LocalDate.now();
        LocalDate from = request.from();
        LocalDate to = request.to();
        if (from == null && to == null) {
            from = today.minusMonths(1);
            to = today;
        } else if (from == null) {
            from = to.minusMonths(1);
        } else if (to == null) {
            to = today;
        }
        if (from.isAfter(to)) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        LocalDateTime toCreatedAtExclusive;
        try {
            toCreatedAtExclusive = to.plusDays(1).atStartOfDay();
        } catch (DateTimeException exception) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        List<TransactionHistoryRow> rows = accountTransactionMapper.findHistories(
                account.getId(), queryType.toTransactionTypes(), from.atStartOfDay(),
                toCreatedAtExclusive, request.cursorCreatedAt(), request.cursorId(), size + 1);
        boolean hasNext = rows.size() > size;
        List<TransactionHistoryItemResponse> items = rows.stream()
                .limit(size)
                .map(TransactionHistoryItemResponse::from)
                .toList();
        return TransactionHistorySliceResponse.of(items, hasNext);
    }
}
