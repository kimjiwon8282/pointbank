package com.pointbank.ledger.api.service;

import com.pointbank.ledger.account.domain.LedgerAccount;
import com.pointbank.ledger.account.domain.LedgerAccountStatus;
import com.pointbank.ledger.account.domain.LedgerAccountType;
import com.pointbank.ledger.account.mapper.LedgerAccountMapper;
import com.pointbank.ledger.account.service.LedgerAccountNumberGenerator;
import com.pointbank.ledger.api.dto.AccountDepositResponse;
import com.pointbank.ledger.api.dto.BankingAccountCreateRequest;
import com.pointbank.ledger.api.dto.BankingAccountResponse;
import com.pointbank.ledger.api.dto.SecuritiesCashAccountCleanupResponse;
import com.pointbank.ledger.api.dto.SecuritiesCashAccountResponse;
import com.pointbank.ledger.api.dto.SecuritiesCashDepositResponse;
import com.pointbank.ledger.api.dto.TransactionHistoryItemResponse;
import com.pointbank.ledger.api.dto.TransactionHistoryRequest;
import com.pointbank.ledger.api.dto.TransactionHistorySliceResponse;
import com.pointbank.ledger.api.dto.TransferResponse;
import com.pointbank.ledger.entry.domain.LedgerEntry;
import com.pointbank.ledger.entry.domain.LedgerEntryDirection;
import com.pointbank.ledger.entry.domain.LedgerEntryType;
import com.pointbank.ledger.entry.dto.LedgerTransactionHistoryRow;
import com.pointbank.ledger.entry.mapper.LedgerEntryMapper;
import com.pointbank.ledger.global.exception.CustomException;
import com.pointbank.ledger.global.exception.ErrorCode;
import com.pointbank.ledger.transfer.domain.LedgerTransferRequest;
import com.pointbank.ledger.transfer.domain.LedgerTransferStatus;
import com.pointbank.ledger.transfer.domain.LedgerTransferType;
import com.pointbank.ledger.transfer.mapper.LedgerTransferRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LedgerInternalService {
    private static final int ACCOUNT_NUMBER_GENERATION_ATTEMPTS = 5;
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 56;
    private static final int TRANSFER_IDEMPOTENCY_KEY_MAX_LENGTH = 60;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final String DEPOSIT_DESCRIPTION = "개발용 포인트 충전";
    private static final String TRANSFER_OUT_DESCRIPTION = "포인트 송금 출금";
    private static final String TRANSFER_IN_DESCRIPTION = "포인트 송금 입금";
    private static final String SECURITIES_DEPOSIT_DESCRIPTION = "증권 예수금 충전";

    private final LedgerAccountMapper accountMapper;
    private final LedgerTransferRequestMapper transferRequestMapper;
    private final LedgerEntryMapper entryMapper;
    private final LedgerAccountNumberGenerator accountNumberGenerator;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public BankingAccountResponse createBankingAccount(BankingAccountCreateRequest request) {
        if (accountMapper.existsByMemberIdAndType(request.memberId(), LedgerAccountType.BANKING)) {
            throw new CustomException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }
        String passwordHash = passwordEncoder.encode(request.accountPassword());
        for (int attempt = 0; attempt < ACCOUNT_NUMBER_GENERATION_ATTEMPTS; attempt++) {
            LedgerAccount account = new LedgerAccount(
                    request.memberId(),
                    LedgerAccountType.BANKING,
                    accountNumberGenerator.generate(),
                    passwordHash,
                    0L,
                    0L,
                    LedgerAccountStatus.ACTIVE
            );
            try {
                accountMapper.insert(account);
                return bankingAccountResponse(account);
            } catch (DuplicateKeyException exception) {
                if (accountMapper.existsByMemberIdAndType(request.memberId(), LedgerAccountType.BANKING)) {
                    throw new CustomException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
                }
            }
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Transactional(readOnly = true)
    public BankingAccountResponse getBankingAccount(Long memberId) {
        LedgerAccount account = accountMapper.findByMemberIdAndType(memberId, LedgerAccountType.BANKING)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        return bankingAccountResponse(account);
    }

    @Transactional
    public SecuritiesCashAccountResponse createSecuritiesCashAccount(Long memberId) {
        return accountMapper.findByMemberIdAndType(memberId, LedgerAccountType.SECURITIES_CASH)
                .map(this::securitiesCashAccountResponse)
                .orElseGet(() -> {
                    LedgerAccount account = new LedgerAccount(
                            memberId,
                            LedgerAccountType.SECURITIES_CASH,
                            null,
                            null,
                            0L,
                            0L,
                            LedgerAccountStatus.ACTIVE
                    );
                    accountMapper.insert(account);
                    return securitiesCashAccountResponse(account);
                });
    }

    @Transactional(readOnly = true)
    public SecuritiesCashAccountResponse getSecuritiesCashAccount(Long memberId) {
        LedgerAccount account = accountMapper.findByMemberIdAndType(memberId, LedgerAccountType.SECURITIES_CASH)
                .orElseThrow(() -> new CustomException(ErrorCode.SECURITIES_CASH_ACCOUNT_NOT_FOUND));
        return securitiesCashAccountResponse(account);
    }

    @Transactional
    public SecuritiesCashAccountCleanupResponse cleanupSecuritiesCashAccount(Long memberId) {
        LedgerAccount account = accountMapper.findByMemberIdAndType(memberId, LedgerAccountType.SECURITIES_CASH)
                .orElse(null);
        if (account == null) {
            return new SecuritiesCashAccountCleanupResponse(memberId, false, "SECURITIES_CASH account not found");
        }
        if (account.getBalance() != 0L || account.getReservedBalance() != 0L) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        if (entryMapper.existsByLedgerAccountId(account.getId())) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        accountMapper.deleteById(account.getId());
        return new SecuritiesCashAccountCleanupResponse(memberId, true, "SECURITIES_CASH account deleted");
    }

    @Transactional
    public AccountDepositResponse deposit(Long memberId, String idempotencyKey, long amount) {
        String requestNo = toRequestNo("ACCDEP-", idempotencyKey, IDEMPOTENCY_KEY_MAX_LENGTH);
        LedgerAccount account = accountMapper.findByMemberIdAndType(memberId, LedgerAccountType.BANKING)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        LedgerTransferRequest transferRequest = createOrResolve(
                requestNo, LedgerTransferType.ACCOUNT_DEPOSIT, account.getId(), null,
                memberId, null, amount);
        if (transferRequest.getStatus() == LedgerTransferStatus.COMPLETED) {
            return completedDepositResponse(transferRequest);
        }

        LedgerAccount locked = lockOne(account.getId());
        validateActive(locked);
        long balanceAfter = add(locked.getBalance(), amount);
        updateBalance(locked.getId(), balanceAfter);
        insertEntry(requestNo, transferRequest.getId(), locked, LedgerEntryType.DEPOSIT,
                LedgerEntryDirection.CREDIT, amount, balanceAfter, locked.getReservedBalance(), DEPOSIT_DESCRIPTION);
        complete(transferRequest.getId(), balanceAfter, null);
        LedgerTransferRequest completed = reloadRequest(requestNo);
        return new AccountDepositResponse(requestNo, memberId, locked.getId(), amount,
                balanceAfter, completed.getStatus().name(), completed.getCompletedAt());
    }

    @Transactional
    public TransferResponse transfer(Long memberId, String idempotencyKey, String toAccountNumber, long amount, String accountPassword) {
        String requestNo = toRequestNo("TRF-", idempotencyKey, TRANSFER_IDEMPOTENCY_KEY_MAX_LENGTH);
        LedgerAccount fromAccount = accountMapper.findByMemberIdAndType(memberId, LedgerAccountType.BANKING)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        LedgerAccount toAccount = accountMapper.findByAccountNumberAndType(toAccountNumber, LedgerAccountType.BANKING)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new CustomException(ErrorCode.CANNOT_TRANSFER_TO_SELF);
        }
        LedgerTransferRequest transferRequest = createOrResolve(
                requestNo, LedgerTransferType.BANK_TO_BANK, fromAccount.getId(), toAccount.getId(),
                fromAccount.getMemberId(), toAccount.getMemberId(), amount);
        if (transferRequest.getStatus() == LedgerTransferStatus.COMPLETED) {
            return completedTransferResponse(transferRequest);
        }

        List<LedgerAccount> locked = accountMapper.findAllByIdsForUpdate(List.of(fromAccount.getId(), toAccount.getId()));
        LedgerAccount lockedFrom = findLocked(locked, fromAccount.getId());
        LedgerAccount lockedTo = findLocked(locked, toAccount.getId());
        validateActive(lockedFrom);
        validateActive(lockedTo);
        if (!passwordEncoder.matches(accountPassword, lockedFrom.getAccountPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_ACCOUNT_PASSWORD);
        }
        if (lockedFrom.getBalance() < amount) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        long fromAfter = subtract(lockedFrom.getBalance(), amount);
        long toAfter = add(lockedTo.getBalance(), amount);
        updateBalance(lockedFrom.getId(), fromAfter);
        updateBalance(lockedTo.getId(), toAfter);
        insertEntry(requestNo, transferRequest.getId(), lockedFrom, LedgerEntryType.TRANSFER_OUT,
                LedgerEntryDirection.DEBIT, amount, fromAfter, lockedFrom.getReservedBalance(), TRANSFER_OUT_DESCRIPTION);
        insertEntry(requestNo, transferRequest.getId(), lockedTo, LedgerEntryType.TRANSFER_IN,
                LedgerEntryDirection.CREDIT, amount, toAfter, lockedTo.getReservedBalance(), TRANSFER_IN_DESCRIPTION);
        complete(transferRequest.getId(), fromAfter, toAfter);
        return transferResponse(reloadRequest(requestNo), lockedFrom, lockedTo, fromAfter, toAfter);
    }

    @Transactional
    public SecuritiesCashDepositResponse depositSecuritiesCash(Long memberId, String idempotencyKey, long amount, String accountPassword) {
        String requestNo = toRequestNo("SECDEP-", idempotencyKey, IDEMPOTENCY_KEY_MAX_LENGTH);
        LedgerAccount banking = accountMapper.findByMemberIdAndType(memberId, LedgerAccountType.BANKING)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        LedgerAccount cash = accountMapper.findByMemberIdAndType(memberId, LedgerAccountType.SECURITIES_CASH)
                .orElseThrow(() -> new CustomException(ErrorCode.SECURITIES_CASH_ACCOUNT_NOT_FOUND));
        LedgerTransferRequest transferRequest = createOrResolve(
                requestNo, LedgerTransferType.BANK_TO_SECURITIES_CASH, banking.getId(), cash.getId(),
                memberId, memberId, amount);
        if (transferRequest.getStatus() == LedgerTransferStatus.COMPLETED) {
            return completedSecuritiesDepositResponse(transferRequest);
        }

        List<LedgerAccount> locked = accountMapper.findAllByIdsForUpdate(List.of(banking.getId(), cash.getId()));
        LedgerAccount lockedBanking = findLocked(locked, banking.getId());
        LedgerAccount lockedCash = findLocked(locked, cash.getId());
        validateActive(lockedBanking);
        validateActive(lockedCash);
        if (!passwordEncoder.matches(accountPassword, lockedBanking.getAccountPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_ACCOUNT_PASSWORD);
        }
        if (lockedBanking.getBalance() < amount) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        long bankingAfter = subtract(lockedBanking.getBalance(), amount);
        long cashAfter = add(lockedCash.getBalance(), amount);
        updateBalance(lockedBanking.getId(), bankingAfter);
        updateBalance(lockedCash.getId(), cashAfter);
        insertEntry(requestNo, transferRequest.getId(), lockedBanking, LedgerEntryType.SECURITIES_DEPOSIT_OUT,
                LedgerEntryDirection.DEBIT, amount, bankingAfter, lockedBanking.getReservedBalance(), SECURITIES_DEPOSIT_DESCRIPTION);
        insertEntry(requestNo, transferRequest.getId(), lockedCash, LedgerEntryType.SECURITIES_DEPOSIT_IN,
                LedgerEntryDirection.CREDIT, amount, cashAfter, lockedCash.getReservedBalance(), SECURITIES_DEPOSIT_DESCRIPTION);
        complete(transferRequest.getId(), bankingAfter, cashAfter);
        LedgerTransferRequest completed = reloadRequest(requestNo);
        return securitiesDepositResponse(completed, lockedCash.getReservedBalance());
    }

    @Transactional(readOnly = true)
    public TransactionHistorySliceResponse getBankingHistories(TransactionHistoryRequest request) {
        LedgerAccount account = accountMapper.findByMemberIdAndType(request.memberId(), LedgerAccountType.BANKING)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        int size = request.size() == null ? DEFAULT_SIZE : request.size();
        if (size < 1 || size > MAX_SIZE || (request.cursorCreatedAt() == null) != (request.cursorId() == null)) {
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
        LocalDateTime toExclusive;
        try {
            toExclusive = to.plusDays(1).atStartOfDay();
        } catch (DateTimeException exception) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        List<String> types = historyTypes(request.type());
        List<LedgerTransactionHistoryRow> rows = entryMapper.findBankingHistories(
                account.getId(), types, from.atStartOfDay(), toExclusive,
                request.cursorCreatedAt(), request.cursorId(), size + 1);
        boolean hasNext = rows.size() > size;
        List<TransactionHistoryItemResponse> items = rows.stream().limit(size).map(this::historyItem).toList();
        TransactionHistoryItemResponse last = hasNext ? items.getLast() : null;
        return new TransactionHistorySliceResponse(items, hasNext,
                last == null ? null : last.createdAt(),
                last == null ? null : last.transactionId());
    }

    private LedgerTransferRequest createOrResolve(
            String requestNo,
            LedgerTransferType transferType,
            Long sourceAccountId,
            Long targetAccountId,
            Long fromMemberId,
            Long toMemberId,
            long amount
    ) {
        return transferRequestMapper.findByRequestNo(requestNo)
                .map(existing -> resolveExisting(existing, transferType, sourceAccountId, targetAccountId, fromMemberId, toMemberId, amount))
                .orElseGet(() -> insertRequested(requestNo, transferType, sourceAccountId, targetAccountId, fromMemberId, toMemberId, amount));
    }

    private LedgerTransferRequest insertRequested(
            String requestNo,
            LedgerTransferType transferType,
            Long sourceAccountId,
            Long targetAccountId,
            Long fromMemberId,
            Long toMemberId,
            long amount
    ) {
        LedgerTransferRequest request = new LedgerTransferRequest(
                requestNo, transferType, sourceAccountId, targetAccountId,
                fromMemberId, toMemberId, amount, LedgerTransferStatus.REQUESTED);
        try {
            transferRequestMapper.insertRequested(request);
            return request;
        } catch (DuplicateKeyException exception) {
            return transferRequestMapper.findByRequestNo(requestNo)
                    .map(existing -> resolveExisting(existing, transferType, sourceAccountId, targetAccountId, fromMemberId, toMemberId, amount))
                    .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    private LedgerTransferRequest resolveExisting(
            LedgerTransferRequest existing,
            LedgerTransferType transferType,
            Long sourceAccountId,
            Long targetAccountId,
            Long fromMemberId,
            Long toMemberId,
            long amount
    ) {
        if (existing.getTransferType() != transferType
                || !Objects.equals(existing.getSourceAccountId(), sourceAccountId)
                || !Objects.equals(existing.getTargetAccountId(), targetAccountId)
                || !Objects.equals(existing.getFromMemberId(), fromMemberId)
                || !Objects.equals(existing.getToMemberId(), toMemberId)
                || existing.getAmount() != amount) {
            throw new CustomException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }
        if (existing.getStatus() == LedgerTransferStatus.REQUESTED) {
            throw new CustomException(ErrorCode.FUND_TRANSFER_IN_PROGRESS);
        }
        return existing;
    }

    private String toRequestNo(String prefix, String idempotencyKey, int maxLength) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.trim().length() > maxLength) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        return prefix + idempotencyKey.trim();
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

    private LedgerAccount lockOne(Long accountId) {
        return findLocked(accountMapper.findAllByIdsForUpdate(List.of(accountId)), accountId);
    }

    private LedgerAccount findLocked(List<LedgerAccount> accounts, Long accountId) {
        return accounts.stream()
                .filter(account -> account.getId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private void validateActive(LedgerAccount account) {
        if (account.getStatus() != LedgerAccountStatus.ACTIVE) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }

    private long add(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private long subtract(long left, long right) {
        try {
            return Math.subtractExact(left, right);
        } catch (ArithmeticException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void updateBalance(Long accountId, long balance) {
        if (accountMapper.updateBalance(accountId, balance) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void insertEntry(
            String requestNo,
            Long transferRequestId,
            LedgerAccount account,
            LedgerEntryType entryType,
            LedgerEntryDirection direction,
            long amount,
            long balanceAfter,
            long reservedBalanceAfter,
            String description
    ) {
        LedgerEntry entry = new LedgerEntry(
                requestNo, transferRequestId, account.getId(), account.getMemberId(),
                account.getAccountType(), entryType, direction, amount,
                balanceAfter, reservedBalanceAfter, description);
        if (entryMapper.insert(entry) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private AccountDepositResponse completedDepositResponse(LedgerTransferRequest request) {
        return new AccountDepositResponse(
                request.getRequestNo(), request.getFromMemberId(), request.getSourceAccountId(),
                request.getAmount(), request.getSourceBalanceAfter() == null ? 0L : request.getSourceBalanceAfter(),
                request.getStatus().name(), request.getCompletedAt());
    }

    private TransferResponse completedTransferResponse(LedgerTransferRequest request) {
        LedgerAccount source = accountMapper.findById(request.getSourceAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        LedgerAccount target = accountMapper.findById(request.getTargetAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        return transferResponse(request, source, target,
                request.getSourceBalanceAfter() == null ? 0L : request.getSourceBalanceAfter(),
                request.getTargetBalanceAfter() == null ? 0L : request.getTargetBalanceAfter());
    }

    private SecuritiesCashDepositResponse completedSecuritiesDepositResponse(LedgerTransferRequest request) {
        LedgerAccount cash = accountMapper.findById(request.getTargetAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        return securitiesDepositResponse(request, cash.getReservedBalance());
    }

    private TransferResponse transferResponse(
            LedgerTransferRequest request,
            LedgerAccount source,
            LedgerAccount target,
            long sourceBalanceAfter,
            long targetBalanceAfter
    ) {
        return new TransferResponse(
                request.getId(), request.getRequestNo(), request.getRequestNo(),
                source.getId(), target.getId(), source.getAccountNumber(), target.getAccountNumber(),
                request.getAmount(), sourceBalanceAfter, targetBalanceAfter,
                request.getStatus().name(), request.getCompletedAt());
    }

    private SecuritiesCashDepositResponse securitiesDepositResponse(LedgerTransferRequest request, long reservedCash) {
        long cashBalance = request.getTargetBalanceAfter() == null ? 0L : request.getTargetBalanceAfter();
        return new SecuritiesCashDepositResponse(
                request.getId(), request.getRequestNo(), request.getFromMemberId(),
                request.getSourceAccountId(), request.getTargetAccountId(), request.getAmount(),
                request.getSourceBalanceAfter() == null ? 0L : request.getSourceBalanceAfter(),
                cashBalance, reservedCash, cashBalance - reservedCash,
                request.getStatus().name(), request.getCompletedAt());
    }

    private BankingAccountResponse bankingAccountResponse(LedgerAccount account) {
        return new BankingAccountResponse(
                account.getId(), account.getMemberId(), account.getAccountNumber(),
                account.getBalance(), account.getStatus().name(), account.getCreatedAt());
    }

    private SecuritiesCashAccountResponse securitiesCashAccountResponse(LedgerAccount account) {
        return new SecuritiesCashAccountResponse(
                account.getId(), account.getMemberId(), account.getBalance(),
                account.getReservedBalance(), account.getBalance() - account.getReservedBalance(),
                account.getStatus().name());
    }

    private List<String> historyTypes(String type) {
        String queryType = type == null ? "ALL" : type;
        return switch (queryType) {
            case "IN" -> List.of("DEPOSIT", "TRANSFER_IN");
            case "OUT" -> List.of("TRANSFER_OUT", "SECURITIES_DEPOSIT_OUT");
            case "ALL" -> List.of("DEPOSIT", "TRANSFER_IN", "TRANSFER_OUT", "SECURITIES_DEPOSIT_OUT");
            default -> throw new CustomException(ErrorCode.BAD_REQUEST);
        };
    }

    private TransactionHistoryItemResponse historyItem(LedgerTransactionHistoryRow row) {
        String direction = switch (row.getTransactionType()) {
            case "DEPOSIT", "TRANSFER_IN" -> "IN";
            default -> "OUT";
        };
        String counterparty = switch (row.getTransactionType()) {
            case "TRANSFER_OUT" -> row.getToAccountNumber();
            case "TRANSFER_IN" -> row.getFromAccountNumber();
            default -> null;
        };
        long signedAmount = "IN".equals(direction) ? row.getAmount() : -row.getAmount();
        String title = row.getDescription() == null || row.getDescription().isBlank()
                ? row.getTransactionType() : row.getDescription();
        return new TransactionHistoryItemResponse(
                row.getTransactionId(), row.getTransactionType(), direction, title,
                row.getAmount(), signedAmount, row.getBalanceAfter(),
                counterparty, row.getTransferNo(), row.getCreatedAt());
    }
}
