package com.pointbank.ledger.account.mapper;

import com.pointbank.ledger.account.domain.LedgerAccount;
import com.pointbank.ledger.account.domain.LedgerAccountType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface LedgerAccountMapper {
    boolean existsByMemberIdAndType(@Param("memberId") Long memberId, @Param("accountType") LedgerAccountType accountType);
    Optional<LedgerAccount> findByMemberIdAndType(@Param("memberId") Long memberId, @Param("accountType") LedgerAccountType accountType);
    Optional<LedgerAccount> findByAccountNumberAndType(@Param("accountNumber") String accountNumber, @Param("accountType") LedgerAccountType accountType);
    Optional<LedgerAccount> findById(Long id);
    List<LedgerAccount> findAllByIdsForUpdate(@Param("accountIds") List<Long> accountIds);
    int insert(LedgerAccount account);
    int updateBalance(@Param("accountId") Long accountId, @Param("balance") long balance);
}
