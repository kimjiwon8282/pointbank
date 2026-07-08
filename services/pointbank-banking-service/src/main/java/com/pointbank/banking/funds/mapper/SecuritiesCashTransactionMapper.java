package com.pointbank.banking.funds.mapper;

import com.pointbank.banking.funds.domain.SecuritiesCashTransaction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface SecuritiesCashTransactionMapper {
    int insert(SecuritiesCashTransaction transaction);
    Optional<SecuritiesCashTransaction> findByRequestNo(String requestNo);
}
