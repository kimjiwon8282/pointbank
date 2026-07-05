package com.pointbank.banking.transfer.service;

import com.pointbank.banking.account.domain.Account;
import com.pointbank.banking.account.domain.AccountStatus;
import com.pointbank.banking.account.mapper.AccountMapper;
import com.pointbank.banking.global.exception.CustomException;
import com.pointbank.banking.global.exception.ErrorCode;
import com.pointbank.banking.transaction.domain.AccountTransaction;
import com.pointbank.banking.transaction.domain.AccountTransactionType;
import com.pointbank.banking.transaction.mapper.AccountTransactionMapper;
import com.pointbank.banking.transfer.domain.Transfer;
import com.pointbank.banking.transfer.dto.TransferCreateRequest;
import com.pointbank.banking.transfer.dto.TransferResponse;
import com.pointbank.banking.transfer.mapper.TransferMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferService {
    private static final int TRANSFER_NO_GENERATION_ATTEMPTS = 5;
    private static final String TRANSFER_OUT_DESCRIPTION = "포인트 송금 출금";
    private static final String TRANSFER_IN_DESCRIPTION = "포인트 송금 입금";

    private final AccountMapper accountMapper;
    private final AccountTransactionMapper accountTransactionMapper;
    private final TransferMapper transferMapper;
    private final TransferNoGenerator transferNoGenerator;
    private final PasswordEncoder accountPasswordEncoder;

    @Transactional
    public TransferResponse transfer(Long memberId, TransferCreateRequest request) {
        Account fromAccount = accountMapper.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        Account toAccount = accountMapper.findByAccountNumber(request.toAccountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.RECEIVER_ACCOUNT_NOT_FOUND));
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new CustomException(ErrorCode.CANNOT_TRANSFER_TO_SELF);
        }

        List<Account> lockedAccounts = accountMapper.findAllByIdsForUpdate(
                List.of(fromAccount.getId(), toAccount.getId()));
        Account lockedFromAccount = findLockedAccount(lockedAccounts, fromAccount.getId());
        Account lockedToAccount = findLockedAccount(lockedAccounts, toAccount.getId());

        validateActive(lockedFromAccount);
        validateActive(lockedToAccount);
        if (!accountPasswordEncoder.matches(
                request.accountPassword(), lockedFromAccount.getAccountPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_ACCOUNT_PASSWORD);
        }

        long amount = request.amount();
        if (lockedFromAccount.getBalance() < amount) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        long fromBalanceAfter;
        long toBalanceAfter;
        try {
            fromBalanceAfter = Math.subtractExact(lockedFromAccount.getBalance(), amount);
            toBalanceAfter = Math.addExact(lockedToAccount.getBalance(), amount);
        } catch (ArithmeticException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        LocalDateTime completedAt = LocalDateTime.now();
        Transfer transfer = insertCompletedTransfer(
                lockedFromAccount, lockedToAccount, amount, completedAt);

        updateBalance(lockedFromAccount.getId(), fromBalanceAfter);
        updateBalance(lockedToAccount.getId(), toBalanceAfter);
        insertTransaction(new AccountTransaction(
                lockedFromAccount.getId(), lockedFromAccount.getMemberId(), transfer.getId(),
                AccountTransactionType.TRANSFER_OUT, amount, fromBalanceAfter,
                TRANSFER_OUT_DESCRIPTION));
        insertTransaction(new AccountTransaction(
                lockedToAccount.getId(), lockedToAccount.getMemberId(), transfer.getId(),
                AccountTransactionType.TRANSFER_IN, amount, toBalanceAfter,
                TRANSFER_IN_DESCRIPTION));

        return new TransferResponse(
                transfer.getTransferNo(), lockedFromAccount.getAccountNumber(),
                lockedToAccount.getAccountNumber(), amount, fromBalanceAfter,
                transfer.getStatus(), transfer.getCompletedAt());
    }

    private Account findLockedAccount(List<Account> accounts, Long accountId) {
        return accounts.stream()
                .filter(account -> account.getId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private void validateActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }

    private Transfer insertCompletedTransfer(
            Account fromAccount, Account toAccount, long amount, LocalDateTime completedAt) {
        for (int attempt = 0; attempt < TRANSFER_NO_GENERATION_ATTEMPTS; attempt++) {
            Transfer transfer = Transfer.completed(
                    transferNoGenerator.generate(), fromAccount.getId(), toAccount.getId(),
                    fromAccount.getMemberId(), toAccount.getMemberId(), amount, completedAt);
            try {
                if (transferMapper.insert(transfer) != 1) {
                    throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
                }
                return transfer;
            } catch (DuplicateKeyException exception) {
                // A new number is generated before any balance is changed.
            }
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private void updateBalance(Long accountId, long balance) {
        if (accountMapper.updateBalance(accountId, balance) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void insertTransaction(AccountTransaction transaction) {
        if (accountTransactionMapper.insert(transaction) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
