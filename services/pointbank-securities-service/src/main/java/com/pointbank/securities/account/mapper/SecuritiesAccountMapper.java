package com.pointbank.securities.account.mapper;

import com.pointbank.securities.account.domain.SecuritiesAccount;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface SecuritiesAccountMapper {
    boolean existsByMemberId(Long memberId);
    boolean existsByAccountNumber(String accountNumber);
    Optional<SecuritiesAccount> findByMemberId(Long memberId);
    int insert(SecuritiesAccount account);
}
