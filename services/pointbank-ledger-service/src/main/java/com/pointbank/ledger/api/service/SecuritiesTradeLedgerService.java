package com.pointbank.ledger.api.service;

import com.pointbank.ledger.account.domain.LedgerAccount;
import com.pointbank.ledger.account.domain.LedgerAccountStatus;
import com.pointbank.ledger.account.domain.LedgerAccountType;
import com.pointbank.ledger.account.mapper.LedgerAccountMapper;
import com.pointbank.ledger.api.dto.BuyOrderReversalRequest;
import com.pointbank.ledger.api.dto.BuyOrderReversalResponse;
import com.pointbank.ledger.api.dto.SecuritiesBuyDebitRequest;
import com.pointbank.ledger.api.dto.SecuritiesSellCreditRequest;
import com.pointbank.ledger.api.dto.SecuritiesTradeFundsResponse;
import com.pointbank.ledger.entry.domain.LedgerEntry;
import com.pointbank.ledger.entry.domain.LedgerEntryDirection;
import com.pointbank.ledger.entry.domain.LedgerEntryType;
import com.pointbank.ledger.entry.mapper.LedgerEntryMapper;
import com.pointbank.ledger.global.exception.CustomException;
import com.pointbank.ledger.global.exception.ErrorCode;
import com.pointbank.ledger.transfer.domain.LedgerTransferRequest;
import com.pointbank.ledger.transfer.domain.LedgerTransferStatus;
import com.pointbank.ledger.transfer.domain.LedgerTransferType;
import com.pointbank.ledger.transfer.mapper.LedgerTransferRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SecuritiesTradeLedgerService {
    private static final String BUY_REQUEST_PREFIX = "STOCKBUY-";
    private static final String BUY_REVERSAL_REQUEST_PREFIX = "STOCKREV-";
    private static final String SELL_REQUEST_PREFIX = "STOCKSELL-";

    private final LedgerAccountMapper accountMapper;
    private final LedgerTransferRequestMapper transferRequestMapper;
    private final LedgerEntryMapper entryMapper;

    @Transactional
    public SecuritiesTradeFundsResponse debitBuyFunds(
            String idempotencyKey,
            SecuritiesBuyDebitRequest request
    ) {
        String orderNo = resolveOrderNo(idempotencyKey, request.orderNo());
        String stockCode = normalizeStockCode(request.stockCode());
        long expectedTotal = add(request.orderAmount(), request.fee());
        if (expectedTotal != request.totalAmount()) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        String requestNo = BUY_REQUEST_PREFIX + orderNo;
        String description = buyDescription(stockCode, request.orderAmount(), request.fee(), request.totalAmount());
        LedgerTransferRequest existing = transferRequestMapper.findByRequestNo(requestNo).orElse(null);
        if (existing != null) {
            return completedResponse(
                    resolveExisting(existing, LedgerTransferType.SECURITIES_BUY, request.memberId(),
                            request.totalAmount(), true, LedgerEntryType.STOCK_BUY, description),
                    request.memberId(), LedgerEntryType.STOCK_BUY, stockCode, true);
        }

        LedgerAccount account = findSecuritiesCashAccount(request.memberId());
        LedgerTransferRequest transferRequest = insertOrResolve(
                requestNo, LedgerTransferType.SECURITIES_BUY, account, request.memberId(),
                request.totalAmount(), true, LedgerEntryType.STOCK_BUY, description);
        if (transferRequest.getStatus() == LedgerTransferStatus.COMPLETED) {
            return completedResponse(
                    transferRequest, request.memberId(), LedgerEntryType.STOCK_BUY, stockCode, true);
        }

        LedgerAccount locked = lockAccount(account.getId());
        validateActive(locked);
        long availableBalance = subtract(locked.getBalance(), locked.getReservedBalance());
        if (availableBalance < request.totalAmount()) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        long balanceAfter = subtract(locked.getBalance(), request.totalAmount());
        updateBalance(locked.getId(), balanceAfter);
        insertEntry(transferRequest, locked, LedgerEntryType.STOCK_BUY, LedgerEntryDirection.DEBIT,
                request.totalAmount(), balanceAfter, description);
        complete(transferRequest.getId(), balanceAfter, null);

        return completedResponse(
                reloadRequest(requestNo), request.memberId(), LedgerEntryType.STOCK_BUY, stockCode, true);
    }

    @Transactional
    public BuyOrderReversalResponse reverseBuyFunds(BuyOrderReversalRequest request) {
        String orderNo = resolveOrderNo(null, request.orderNo());
        String stockCode = normalizeStockCode(request.stockCode());
        String originalRequestNo = BUY_REQUEST_PREFIX + orderNo;
        if (!originalRequestNo.equals(request.originalLedgerRequestNo())) {
            throw new CustomException(ErrorCode.BUY_REVERSAL_NOT_ALLOWED);
        }

        LedgerTransferRequest original = transferRequestMapper.findByRequestNo(originalRequestNo)
                .orElseThrow(() -> new CustomException(ErrorCode.BUY_REVERSAL_NOT_ALLOWED));
        validateOriginalBuyForReversal(original, request, stockCode);

        String reversalRequestNo = BUY_REVERSAL_REQUEST_PREFIX + orderNo;
        LedgerTransferRequest existing = transferRequestMapper.findByRequestNo(reversalRequestNo).orElse(null);
        if (existing != null) {
            return completedReversalResponse(
                    resolveExistingReversal(existing, original, request), originalRequestNo, stockCode);
        }

        LedgerAccount account = findSecuritiesCashAccount(request.memberId());
        if (!Objects.equals(account.getId(), original.getSourceAccountId())) {
            throw new CustomException(ErrorCode.BUY_REVERSAL_NOT_ALLOWED);
        }
        LedgerTransferRequest reversal = new LedgerTransferRequest(
                reversalRequestNo,
                LedgerTransferType.SECURITIES_BUY_REVERSAL,
                null,
                account.getId(),
                null,
                request.memberId(),
                request.reversalAmount(),
                LedgerTransferStatus.REQUESTED
        );
        try {
            transferRequestMapper.insertRequested(reversal);
        } catch (DuplicateKeyException exception) {
            LedgerTransferRequest concurrent = transferRequestMapper.findByRequestNo(reversalRequestNo)
                    .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
            return completedReversalResponse(
                    resolveExistingReversal(concurrent, original, request), originalRequestNo, stockCode);
        }

        LedgerAccount locked = lockAccount(account.getId());
        long balanceAfter = add(locked.getBalance(), request.reversalAmount());
        updateBalance(locked.getId(), balanceAfter);
        insertEntry(
                reversal,
                locked,
                LedgerEntryType.STOCK_BUY_REVERSAL,
                LedgerEntryDirection.CREDIT,
                request.reversalAmount(),
                balanceAfter,
                reversalDescription(request, stockCode)
        );
        complete(reversal.getId(), null, balanceAfter);
        return completedReversalResponse(
                reloadRequest(reversalRequestNo), originalRequestNo, stockCode);
    }

    @Transactional
    public SecuritiesTradeFundsResponse creditSellFunds(
            String idempotencyKey,
            SecuritiesSellCreditRequest request
    ) {
        String orderNo = resolveOrderNo(idempotencyKey, request.orderNo());
        String stockCode = normalizeStockCode(request.stockCode());
        long expectedNet = subtract(subtract(request.sellAmount(), request.fee()), request.tax());
        if (expectedNet <= 0L || expectedNet != request.netAmount()) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        String requestNo = SELL_REQUEST_PREFIX + orderNo;
        String description = sellDescription(
                stockCode, request.sellAmount(), request.fee(), request.tax(), request.netAmount());
        LedgerTransferRequest existing = transferRequestMapper.findByRequestNo(requestNo).orElse(null);
        if (existing != null) {
            return completedResponse(
                    resolveExisting(existing, LedgerTransferType.SECURITIES_SELL, request.memberId(),
                            request.netAmount(), false, LedgerEntryType.STOCK_SELL, description),
                    request.memberId(), LedgerEntryType.STOCK_SELL, stockCode, false);
        }

        LedgerAccount account = findSecuritiesCashAccount(request.memberId());
        LedgerTransferRequest transferRequest = insertOrResolve(
                requestNo, LedgerTransferType.SECURITIES_SELL, account, request.memberId(),
                request.netAmount(), false, LedgerEntryType.STOCK_SELL, description);
        if (transferRequest.getStatus() == LedgerTransferStatus.COMPLETED) {
            return completedResponse(
                    transferRequest, request.memberId(), LedgerEntryType.STOCK_SELL, stockCode, false);
        }

        LedgerAccount locked = lockAccount(account.getId());
        validateActive(locked);
        long balanceAfter = add(locked.getBalance(), request.netAmount());
        updateBalance(locked.getId(), balanceAfter);
        insertEntry(transferRequest, locked, LedgerEntryType.STOCK_SELL, LedgerEntryDirection.CREDIT,
                request.netAmount(), balanceAfter, description);
        complete(transferRequest.getId(), null, balanceAfter);

        return completedResponse(
                reloadRequest(requestNo), request.memberId(), LedgerEntryType.STOCK_SELL, stockCode, false);
    }

    private LedgerTransferRequest insertOrResolve(
            String requestNo,
            LedgerTransferType transferType,
            LedgerAccount account,
            Long memberId,
            long amount,
            boolean debit,
            LedgerEntryType entryType,
            String description
    ) {
        LedgerTransferRequest request = new LedgerTransferRequest(
                requestNo,
                transferType,
                debit ? account.getId() : null,
                debit ? null : account.getId(),
                debit ? memberId : null,
                debit ? null : memberId,
                amount,
                LedgerTransferStatus.REQUESTED
        );
        try {
            transferRequestMapper.insertRequested(request);
            return request;
        } catch (DuplicateKeyException exception) {
            LedgerTransferRequest existing = transferRequestMapper.findByRequestNo(requestNo)
                    .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
            return resolveExisting(existing, transferType, memberId, amount, debit, entryType, description);
        }
    }

    private LedgerTransferRequest resolveExisting(
            LedgerTransferRequest existing,
            LedgerTransferType transferType,
            Long memberId,
            long amount,
            boolean debit,
            LedgerEntryType entryType,
            String description
    ) {
        boolean identityMatches = existing.getTransferType() == transferType
                && existing.getAmount() == amount
                && (debit
                ? Objects.equals(existing.getFromMemberId(), memberId)
                    && existing.getSourceAccountId() != null
                    && existing.getTargetAccountId() == null
                    && existing.getToMemberId() == null
                : Objects.equals(existing.getToMemberId(), memberId)
                    && existing.getTargetAccountId() != null
                    && existing.getSourceAccountId() == null
                    && existing.getFromMemberId() == null);
        if (!identityMatches) {
            throw new CustomException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }
        if (existing.getStatus() == LedgerTransferStatus.REQUESTED) {
            throw new CustomException(ErrorCode.FUND_TRANSFER_IN_PROGRESS);
        }
        LedgerEntry entry = entryMapper.findByTransferRequestIdAndEntryType(existing.getId(), entryType.name())
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        if (!Objects.equals(entry.getDescription(), description)) {
            throw new CustomException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }
        return existing;
    }

    private void validateOriginalBuyForReversal(
            LedgerTransferRequest original,
            BuyOrderReversalRequest request,
            String stockCode
    ) {
        boolean matches = original.getTransferType() == LedgerTransferType.SECURITIES_BUY
                && original.getStatus() == LedgerTransferStatus.COMPLETED
                && Objects.equals(original.getFromMemberId(), request.memberId())
                && original.getAmount() == request.reversalAmount()
                && original.getSourceAccountId() != null;
        if (!matches) {
            throw new CustomException(ErrorCode.BUY_REVERSAL_NOT_ALLOWED);
        }
        LedgerEntry originalEntry = entryMapper.findByTransferRequestIdAndEntryType(
                        original.getId(), LedgerEntryType.STOCK_BUY.name())
                .orElseThrow(() -> new CustomException(ErrorCode.BUY_REVERSAL_NOT_ALLOWED));
        if (originalEntry.getDescription() == null
                || !originalEntry.getDescription().startsWith("stockCode=" + stockCode + ";")) {
            throw new CustomException(ErrorCode.BUY_REVERSAL_NOT_ALLOWED);
        }
    }

    private LedgerTransferRequest resolveExistingReversal(
            LedgerTransferRequest existing,
            LedgerTransferRequest original,
            BuyOrderReversalRequest request
    ) {
        boolean matches = existing.getTransferType() == LedgerTransferType.SECURITIES_BUY_REVERSAL
                && existing.getStatus() == LedgerTransferStatus.COMPLETED
                && Objects.equals(existing.getToMemberId(), request.memberId())
                && existing.getAmount() == request.reversalAmount()
                && existing.getSourceAccountId() == null
                && Objects.equals(existing.getTargetAccountId(), original.getSourceAccountId());
        if (!matches) {
            throw new CustomException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }
        entryMapper.findByTransferRequestIdAndEntryType(existing.getId(), LedgerEntryType.STOCK_BUY_REVERSAL.name())
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        return existing;
    }

    private BuyOrderReversalResponse completedReversalResponse(
            LedgerTransferRequest reversal,
            String originalRequestNo,
            String stockCode
    ) {
        if (reversal.getTargetBalanceAfter() == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return new BuyOrderReversalResponse(
                reversal.getRequestNo(),
                originalRequestNo,
                reversal.getToMemberId(),
                stockCode,
                reversal.getAmount(),
                reversal.getTargetBalanceAfter(),
                reversal.getStatus().name()
        );
    }

    private String reversalDescription(BuyOrderReversalRequest request, String stockCode) {
        String description = "originalRequestNo=" + request.originalLedgerRequestNo()
                + ";stockCode=" + stockCode
                + ";reasonCode=" + request.reasonCode()
                + ";reasonMessage=" + request.reasonMessage();
        return description.length() <= 255 ? description : description.substring(0, 255);
    }

    private SecuritiesTradeFundsResponse completedResponse(
            LedgerTransferRequest request,
            Long memberId,
            LedgerEntryType entryType,
            String stockCode,
            boolean debit
    ) {
        Long accountId = debit ? request.getSourceAccountId() : request.getTargetAccountId();
        Long balanceAfter = debit ? request.getSourceBalanceAfter() : request.getTargetBalanceAfter();
        if (accountId == null || balanceAfter == null || request.getStatus() != LedgerTransferStatus.COMPLETED) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return new SecuritiesTradeFundsResponse(
                request.getRequestNo(), memberId, accountId, request.getTransferType().name(),
                entryType.name(), stockCode, request.getAmount(), balanceAfter, request.getStatus().name());
    }

    private LedgerAccount findSecuritiesCashAccount(Long memberId) {
        return accountMapper.findByMemberIdAndType(memberId, LedgerAccountType.SECURITIES_CASH)
                .orElseThrow(() -> new CustomException(ErrorCode.SECURITIES_CASH_ACCOUNT_NOT_FOUND));
    }

    private LedgerAccount lockAccount(Long accountId) {
        return accountMapper.findAllByIdsForUpdate(List.of(accountId)).stream()
                .filter(account -> account.getId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private void validateActive(LedgerAccount account) {
        if (account.getStatus() != LedgerAccountStatus.ACTIVE) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }

    private void updateBalance(Long accountId, long balance) {
        if (accountMapper.updateBalance(accountId, balance) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void insertEntry(
            LedgerTransferRequest transferRequest,
            LedgerAccount account,
            LedgerEntryType entryType,
            LedgerEntryDirection direction,
            long amount,
            long balanceAfter,
            String description
    ) {
        LedgerEntry entry = new LedgerEntry(
                transferRequest.getRequestNo(), transferRequest.getId(), account.getId(), account.getMemberId(),
                account.getAccountType(), entryType, direction, amount, balanceAfter,
                account.getReservedBalance(), description);
        if (entryMapper.insert(entry) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void complete(Long requestId, Long sourceBalanceAfter, Long targetBalanceAfter) {
        if (transferRequestMapper.complete(requestId, sourceBalanceAfter, targetBalanceAfter, LocalDateTime.now()) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private LedgerTransferRequest reloadRequest(String requestNo) {
        return transferRequestMapper.findByRequestNo(requestNo)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private String resolveOrderNo(String idempotencyKey, String orderNoValue) {
        String orderNo = orderNoValue == null ? null : orderNoValue.trim();
        if (orderNo == null || orderNo.isEmpty() || orderNo.length() > 54) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        if (idempotencyKey != null && !idempotencyKey.trim().equals(orderNo)) {
            throw new CustomException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }
        return orderNo;
    }

    private String normalizeStockCode(String stockCodeValue) {
        String stockCode = stockCodeValue == null ? null : stockCodeValue.trim();
        if (stockCode == null || stockCode.isEmpty() || stockCode.length() > 20) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        return stockCode;
    }

    private String buyDescription(String stockCode, long orderAmount, long fee, long totalAmount) {
        return "stockCode=" + stockCode
                + ";orderAmount=" + orderAmount
                + ";fee=" + fee
                + ";totalAmount=" + totalAmount;
    }

    private String sellDescription(String stockCode, long sellAmount, long fee, long tax, long netAmount) {
        return "stockCode=" + stockCode
                + ";sellAmount=" + sellAmount
                + ";fee=" + fee
                + ";tax=" + tax
                + ";netAmount=" + netAmount;
    }

    private long add(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private long subtract(long left, long right) {
        try {
            return Math.subtractExact(left, right);
        } catch (ArithmeticException exception) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }
}
