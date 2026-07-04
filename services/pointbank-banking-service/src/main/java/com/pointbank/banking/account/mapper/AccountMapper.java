package com.pointbank.banking.account.mapper;

import com.pointbank.banking.account.domain.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface AccountMapper {
    boolean existsByMemberId(Long memberId);
    Optional<Account> findByMemberId(Long memberId);
    Optional<Account> findByMemberIdForUpdate(Long memberId);
    int insert(Account account);
    int updateBalance(@Param("accountId") Long accountId, @Param("balance") long balance);
}
