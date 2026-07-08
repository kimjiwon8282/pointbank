package com.pointbank.banking.funds.service;

import com.pointbank.banking.account.domain.Account;
import com.pointbank.banking.account.domain.AccountStatus;
import com.pointbank.banking.account.mapper.AccountMapper;
import com.pointbank.banking.funds.domain.FundAccountType;
import com.pointbank.banking.funds.domain.FundTransferRequest;
import com.pointbank.banking.funds.domain.FundTransferStatus;
import com.pointbank.banking.funds.domain.SecuritiesCashAccount;
import com.pointbank.banking.funds.domain.SecuritiesCashAccountStatus;
import com.pointbank.banking.funds.domain.SecuritiesCashTransaction;
import com.pointbank.banking.funds.domain.SecuritiesCashTransactionType;
import com.pointbank.banking.funds.dto.SecuritiesCashAccountResponse;
import com.pointbank.banking.funds.dto.SecuritiesDepositRequest;
import com.pointbank.banking.funds.dto.SecuritiesDepositResponse;
import com.pointbank.banking.funds.mapper.FundTransferRequestMapper;
import com.pointbank.banking.funds.mapper.SecuritiesCashAccountMapper;
import com.pointbank.banking.funds.mapper.SecuritiesCashTransactionMapper;
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

@Service
@RequiredArgsConstructor
public class SecuritiesFundsService {

    private static final String REQUEST_NO_PREFIX = "SECDEP-";
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 56;
    private static final String DEPOSIT_DESCRIPTION = "증권 예수금 충전";
    private static final FundAccountType SOURCE_TYPE = FundAccountType.BANKING_ACCOUNT;
    private static final FundAccountType TARGET_TYPE = FundAccountType.SECURITIES_CASH;

    private final AccountMapper accountMapper;
    private final AccountTransactionMapper accountTransactionMapper;
    private final SecuritiesCashAccountMapper securitiesCashAccountMapper;
    private final SecuritiesCashTransactionMapper securitiesCashTransactionMapper;
    private final FundTransferRequestMapper fundTransferRequestMapper;
    private final PasswordEncoder accountPasswordEncoder;

    @Transactional
    public SecuritiesDepositResponse deposit(
            Long memberId,
            String idempotencyKey,
            SecuritiesDepositRequest request
    ) {
        String requestNo = toRequestNo(idempotencyKey);
        FundTransferRequest transferRequest = createTransferRequestOrResolveExisting(
                requestNo,
                memberId,
                request.amount()
        );
        if (transferRequest.getStatus() == FundTransferStatus.COMPLETED) {
            return completedResponse(transferRequest);
        }

        Account bankingAccount = accountMapper.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        SecuritiesCashAccount cashAccount = securitiesCashAccountMapper.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SECURITIES_CASH_ACCOUNT_NOT_FOUND));

        if (bankingAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new CustomException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        if (!accountPasswordEncoder.matches(request.accountPassword(), bankingAccount.getAccountPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_ACCOUNT_PASSWORD);
        }
        if (bankingAccount.getBalance() < request.amount()) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        if (cashAccount.getStatus() != SecuritiesCashAccountStatus.ACTIVE) {
            throw new CustomException(ErrorCode.SECURITIES_CASH_ACCOUNT_NOT_FOUND);
        }

        long bankingBalanceAfter;
        long cashBalanceAfter;
        try {
            bankingBalanceAfter = Math.subtractExact(bankingAccount.getBalance(), request.amount());
            cashBalanceAfter = Math.addExact(cashAccount.getCashBalance(), request.amount());
        } catch (ArithmeticException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        if (accountMapper.updateBalance(bankingAccount.getId(), bankingBalanceAfter) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (securitiesCashAccountMapper.updateCashBalance(cashAccount.getId(), cashBalanceAfter) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        AccountTransaction accountTransaction = new AccountTransaction(
                bankingAccount.getId(),
                memberId,
                requestNo,
                AccountTransactionType.SECURITIES_DEPOSIT_OUT,
                request.amount(),
                bankingBalanceAfter,
                DEPOSIT_DESCRIPTION
        );
        if (accountTransactionMapper.insert(accountTransaction) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        SecuritiesCashTransaction cashTransaction = new SecuritiesCashTransaction(
                cashAccount.getId(),
                memberId,
                requestNo,
                SecuritiesCashTransactionType.DEPOSIT_FROM_BANKING,
                request.amount(),
                cashBalanceAfter,
                cashAccount.getReservedCash(),
                DEPOSIT_DESCRIPTION
        );
        try {
            if (securitiesCashTransactionMapper.insert(cashTransaction) != 1) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        } catch (DuplicateKeyException exception) {
            throw new CustomException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }

        LocalDateTime completedAt = LocalDateTime.now();
        if (fundTransferRequestMapper.complete(
                transferRequest.getId(),
                bankingAccount.getId(),
                cashAccount.getId(),
                bankingBalanceAfter,
                completedAt
        ) != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        FundTransferRequest completed = fundTransferRequestMapper.findByRequestNo(requestNo)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        return SecuritiesDepositResponse.of(
                completed,
                bankingBalanceAfter,
                cashBalanceAfter,
                cashAccount.getReservedCash()
        );
    }

    @Transactional(readOnly = true)
    public SecuritiesCashAccountResponse getMine(Long memberId) {
        SecuritiesCashAccount account = securitiesCashAccountMapper.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SECURITIES_CASH_ACCOUNT_NOT_FOUND));
        return SecuritiesCashAccountResponse.from(account);
    }

    private String toRequestNo(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.trim().length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
        return REQUEST_NO_PREFIX + idempotencyKey.trim();
    }

    private FundTransferRequest createTransferRequestOrResolveExisting(
            String requestNo,
            Long memberId,
            long amount
    ) {
        return fundTransferRequestMapper.findByRequestNo(requestNo)
                .map(existing -> resolveExisting(existing, memberId, amount))
                .orElseGet(() -> insertRequested(requestNo, memberId, amount));
    }

    private FundTransferRequest insertRequested(String requestNo, Long memberId, long amount) {
        FundTransferRequest request = new FundTransferRequest(
                requestNo,
                memberId,
                SOURCE_TYPE,
                TARGET_TYPE,
                amount,
                FundTransferStatus.REQUESTED
        );
        try {
            fundTransferRequestMapper.insertRequested(request);
            return request;
        } catch (DuplicateKeyException exception) {
            return fundTransferRequestMapper.findByRequestNo(requestNo)
                    .map(existing -> resolveExisting(existing, memberId, amount))
                    .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    private FundTransferRequest resolveExisting(
            FundTransferRequest existing,
            Long memberId,
            long amount
    ) {
        if (!Objects.equals(existing.getMemberId(), memberId)
                || existing.getAmount() != amount
                || existing.getSourceType() != SOURCE_TYPE
                || existing.getTargetType() != TARGET_TYPE) {
            throw new CustomException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }
        if (existing.getStatus() == FundTransferStatus.REQUESTED) {
            throw new CustomException(ErrorCode.FUND_TRANSFER_IN_PROGRESS);
        }
        return existing;
    }

    private SecuritiesDepositResponse completedResponse(FundTransferRequest transferRequest) {
        SecuritiesCashTransaction transaction = securitiesCashTransactionMapper
                .findByRequestNo(transferRequest.getRequestNo())
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        return SecuritiesDepositResponse.of(
                transferRequest,
                transferRequest.getBankingBalanceAfter() == null ? 0L : transferRequest.getBankingBalanceAfter(),
                transaction.getCashBalanceAfter(),
                transaction.getReservedCashAfter()
        );
    }
}
