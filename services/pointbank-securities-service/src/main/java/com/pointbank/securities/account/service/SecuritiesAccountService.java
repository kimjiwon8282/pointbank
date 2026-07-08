package com.pointbank.securities.account.service;

import com.pointbank.securities.account.domain.SecuritiesAccount;
import com.pointbank.securities.account.domain.SecuritiesAccountStatus;
import com.pointbank.securities.account.dto.SecuritiesAccountCreateRequest;
import com.pointbank.securities.account.dto.SecuritiesAccountResponse;
import com.pointbank.securities.account.mapper.SecuritiesAccountMapper;
import com.pointbank.securities.account.support.SecuritiesAccountNumberGenerator;
import com.pointbank.securities.global.exception.CustomException;
import com.pointbank.securities.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecuritiesAccountService {

    private static final int ACCOUNT_NUMBER_GENERATION_ATTEMPTS = 5;

    private final SecuritiesAccountMapper securitiesAccountMapper;
    private final SecuritiesAccountNumberGenerator securitiesAccountNumberGenerator;
    private final PasswordEncoder securitiesAccountPasswordEncoder;

    @Transactional
    public SecuritiesAccountResponse create(Long memberId, SecuritiesAccountCreateRequest request) {
        if (securitiesAccountMapper.existsByMemberId(memberId)) {
            throw new CustomException(ErrorCode.SECURITIES_ACCOUNT_ALREADY_EXISTS);
        }

        String passwordHash = securitiesAccountPasswordEncoder.encode(request.accountPassword());
        for (int attempt = 0; attempt < ACCOUNT_NUMBER_GENERATION_ATTEMPTS; attempt++) {
            String accountNumber = securitiesAccountNumberGenerator.generate();
            SecuritiesAccount account = new SecuritiesAccount(
                    memberId,
                    accountNumber,
                    passwordHash,
                    SecuritiesAccountStatus.ACTIVE
            );
            try {
                securitiesAccountMapper.insert(account);
                return SecuritiesAccountResponse.from(account);
            } catch (DuplicateKeyException exception) {
                if (securitiesAccountMapper.existsByMemberId(memberId)) {
                    throw new CustomException(ErrorCode.SECURITIES_ACCOUNT_ALREADY_EXISTS);
                }
                if (!securitiesAccountMapper.existsByAccountNumber(accountNumber)) {
                    throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
                }
            }
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Transactional(readOnly = true)
    public SecuritiesAccountResponse getMine(Long memberId) {
        SecuritiesAccount account = securitiesAccountMapper.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SECURITIES_ACCOUNT_NOT_FOUND));
        return SecuritiesAccountResponse.from(account);
    }
}
