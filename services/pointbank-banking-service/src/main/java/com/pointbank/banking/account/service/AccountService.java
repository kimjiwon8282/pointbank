package com.pointbank.banking.account.service;

import com.pointbank.banking.account.domain.Account;
import com.pointbank.banking.account.domain.AccountStatus;
import com.pointbank.banking.account.dto.AccountCreateRequest;
import com.pointbank.banking.account.dto.AccountDepositRequest;
import com.pointbank.banking.account.dto.AccountDepositResponse;
import com.pointbank.banking.account.dto.AccountResponse;
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

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final int ACCOUNT_NUMBER_GENERATION_ATTEMPTS = 5;
    private static final String DEVELOPMENT_DEPOSIT_DESCRIPTION = "개발용 포인트 충전";

    private final AccountMapper accountMapper;
    private final AccountNumberGenerator accountNumberGenerator;
    private final PasswordEncoder accountPasswordEncoder;
    private final AccountTransactionMapper accountTransactionMapper;

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
                AccountTransactionType.DEPOSIT,
                request.amount(),
                balanceAfter,
                DEVELOPMENT_DEPOSIT_DESCRIPTION
        );
        if (accountTransactionMapper.insert(transaction) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return new AccountDepositResponse(
                account.getId(),
                account.getAccountNumber(),
                request.amount(),
                balanceAfter,
                AccountTransactionType.DEPOSIT
        );
    }
}
