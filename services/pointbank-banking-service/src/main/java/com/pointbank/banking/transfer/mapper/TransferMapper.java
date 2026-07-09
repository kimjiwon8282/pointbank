package com.pointbank.banking.transfer.mapper;

import com.pointbank.banking.transfer.domain.Transfer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface TransferMapper {
    int insert(Transfer transfer);
    int complete(@Param("id") Long id, @Param("completedAt") LocalDateTime completedAt);
    Optional<Transfer> findById(Long id);
    Optional<Transfer> findByTransferNo(String transferNo);
    Optional<Transfer> findByRequestNo(String requestNo);
}
