package com.pointbank.securities.account.mapper;

import com.pointbank.securities.account.domain.SecuritiesAccount;
import com.pointbank.securities.account.domain.SecuritiesAccountStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface SecuritiesAccountMapper {
    boolean existsByMemberId(Long memberId);
    boolean existsByAccountNumber(String accountNumber);
    Optional<SecuritiesAccount> findById(Long id);
    Optional<SecuritiesAccount> findByMemberId(Long memberId);
    int insert(SecuritiesAccount account);
    int updateStatus(@Param("id") Long id, @Param("status") SecuritiesAccountStatus status);
    int deleteById(Long id);
}
