package com.pointbank.banking.funds.mapper;

import com.pointbank.banking.funds.domain.SecuritiesCashAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface SecuritiesCashAccountMapper {
    Optional<SecuritiesCashAccount> findByMemberId(Long memberId);
    Optional<SecuritiesCashAccount> findByMemberIdForUpdate(Long memberId);
    int insert(SecuritiesCashAccount account);
    int updateCashBalance(@Param("id") Long id, @Param("cashBalance") long cashBalance);
}
