package com.pointbank.banking.transfer.mapper;

import com.pointbank.banking.transfer.domain.Transfer;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface TransferMapper {
    int insert(Transfer transfer);
    Optional<Transfer> findById(Long id);
    Optional<Transfer> findByTransferNo(String transferNo);
}
