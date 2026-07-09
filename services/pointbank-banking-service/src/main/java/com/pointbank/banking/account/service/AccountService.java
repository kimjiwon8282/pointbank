package com.pointbank.banking.account.service;

import com.pointbank.banking.account.domain.Account;
import com.pointbank.banking.account.domain.AccountDepositRequestRecord;
import com.pointbank.banking.account.domain.AccountDepositStatus;
import com.pointbank.banking.account.domain.AccountStatus;
import com.pointbank.banking.account.dto.AccountCreateRequest;
import com.pointbank.banking.account.dto.AccountDepositRequest;
import com.pointbank.banking.account.dto.AccountDepositResponse;
import com.pointbank.banking.account.dto.AccountResponse;
import com.pointbank.banking.account.mapper.AccountDepositRequestMapper;
import com.pointbank.banking.account.mapper.AccountMapper;
import com.pointbank.banking.global.exception.CustomException;
import com.pointbank.banking.global.exception.ErrorCode;
import com.pointbank.banking.transaction.domain.AccountTransaction;
import com.pointbank.banking.transaction.domain.AccountTransactionType;
import com.pointbank.banking.transaction.mapper.AccountTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final int ACCOUNT_NUMBER_GENERATION_ATTEMPTS = 5;
    private static final String DEPOSIT_REQUEST_NO_PREFIX = "ACCDEP-";
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 56;
    private static final String DEVELOPMENT_DEPOSIT_DESCRIPTION = "개발용 포인트 충전";

    private final AccountMapper accountMapper;
    private final AccountNumberGenerator accountNumberGenerator;
    private final PasswordEncoder accountPasswordEncoder;
    private final AccountTransactionMapper accountTransactionMapper;
    private final AccountDepositRequestMapper accountDepositRequestMapper;

    @Transactional
    public AccountResponse create(Long memberId, AccountCreateRequest request) {
        if (accountMapper.existsByMemberId(memberId)) {
            throw new CustomException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }

        String passwordHash = accountPasswordEncoder.encode(request.accountPassword());
        for (int attempt = 0; attempt < ACCOUNT_NUMBER_GENERATION_ATTEMPTS; attempt++) {
            Account account = new Account(
                    memberId,
                    accountNumberGenerator.generate(),
                    passwordHash,
                    0L,
                    AccountStatus.ACTIVE
            );
            try {
                accountMapper.insert(account);
                return AccountResponse.from(account);
            } catch (DuplicateKeyException exception) {
                if (accountMapper.existsByMemberId(memberId)) {
                    throw new CustomException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
                }
            }
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Transactional(readOnly = true)
    public AccountResponse getMine(Long memberId) {
        Account account = accountMapper.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountDepositResponse deposit(Long memberId, AccountDepositRequest request) {
        return deposit(memberId, UUID.randomUUID().toString(), request);
    }

    @Transactional
    public AccountDepositResponse deposit(Long memberId, String idempotencyKey, AccountDepositRequest request) {
        String requestNo = toDepositRequestNo(idempotencyKey);
        AccountDepositRequestRecord depositRequest = createDepositRequestOrResolveExisting(
                requestNo,
                memberId,
                request.amount()
        );
        if (depositRequest.getStatus() == AccountDepositStatus.COMPLETED) {
            return completedDepositResponse(depositRequest);
        }

        Account account = accountMapper.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        long balanceAfter;
        try {
            balanceAfter = Math.addExact(account.getBalance(), request.amount());
        } catch (ArithmeticException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        if (accountMapper.updateBalance(account.getId(), balanceAfter) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        AccountTransaction transaction = new AccountTransaction(
                account.getId(),
                memberId,
                requestNo,
                AccountTransactionType.DEPOSIT,
                request.amount(),
                balanceAfter,
                DEVELOPMENT_DEPOSIT_DESCRIPTION
        );
        if (accountTransactionMapper.insert(transaction) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        LocalDateTime completedAt = LocalDateTime.now();
        if (accountDepositRequestMapper.complete(
                depositRequest.getId(),
                account.getId(),
                balanceAfter,
                completedAt
        ) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        AccountDepositRequestRecord completed = accountDepositRequestMapper.findByRequestNo(requestNo)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        return responseFrom(completed, account);
    }

    private String toDepositRequestNo(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.trim().length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        return DEPOSIT_REQUEST_NO_PREFIX + idempotencyKey.trim();
    }

    private AccountDepositRequestRecord createDepositRequestOrResolveExisting(
            String requestNo,
            Long memberId,
            long amount
    ) {
        return accountDepositRequestMapper.findByRequestNo(requestNo)
                .map(existing -> resolveExisting(existing, memberId, amount))
                .orElseGet(() -> insertRequested(requestNo, memberId, amount));
    }

    private AccountDepositRequestRecord insertRequested(String requestNo, Long memberId, long amount) {
        AccountDepositRequestRecord request = new AccountDepositRequestRecord(
                requestNo,
                memberId,
                amount,
                AccountDepositStatus.REQUESTED
        );
        try {
            accountDepositRequestMapper.insertRequested(request);
            return request;
        } catch (DuplicateKeyException exception) {
            return accountDepositRequestMapper.findByRequestNo(requestNo)
                    .map(existing -> resolveExisting(existing, memberId, amount))
                    .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    private AccountDepositRequestRecord resolveExisting(
            AccountDepositRequestRecord existing,
            Long memberId,
            long amount
    ) {
        if (!Objects.equals(existing.getMemberId(), memberId)
                || existing.getAmount() != amount) {
            throw new CustomException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }
        if (existing.getStatus() == AccountDepositStatus.REQUESTED) {
            throw new CustomException(ErrorCode.FUND_TRANSFER_IN_PROGRESS);
        }
        return existing;
    }

    private AccountDepositResponse completedDepositResponse(AccountDepositRequestRecord depositRequest) {
        Account account = accountMapper.findById(depositRequest.getAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        return responseFrom(depositRequest, account);
    }

    private AccountDepositResponse responseFrom(AccountDepositRequestRecord depositRequest, Account account) {
        return new AccountDepositResponse(
                account.getId(),
                account.getAccountNumber(),
                depositRequest.getAmount(),
                depositRequest.getBalanceAfter() == null ? 0L : depositRequest.getBalanceAfter(),
                AccountTransactionType.DEPOSIT
        );
    }
}
