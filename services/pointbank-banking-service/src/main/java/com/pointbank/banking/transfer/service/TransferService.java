package com.pointbank.banking.transfer.service;

import com.pointbank.banking.ledger.LedgerClient;
import com.pointbank.banking.transfer.dto.TransferCreateRequest;
import com.pointbank.banking.transfer.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final LedgerClient ledgerClient;

    public TransferResponse transfer(Long memberId, String idempotencyKey, TransferCreateRequest request) {
        return ledgerClient.transfer(memberId, idempotencyKey, request);
    }

    public TransferResponse transfer(Long memberId, TransferCreateRequest request) {
        return ledgerClient.transfer(memberId, java.util.UUID.randomUUID().toString(), request);
    }
}
