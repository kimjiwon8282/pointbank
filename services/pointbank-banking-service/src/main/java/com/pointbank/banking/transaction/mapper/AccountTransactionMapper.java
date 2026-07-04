package com.pointbank.banking.transaction.mapper;

import com.pointbank.banking.transaction.domain.AccountTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountTransactionMapper {
    int insert(AccountTransaction transaction);
}
