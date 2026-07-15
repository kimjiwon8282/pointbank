package com.pointbank.quote.quote.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuoteBulkRequest(
        @NotEmpty @Size(max = 100)
        List<@Valid @NotBlank @Size(max = 20) String> stockCodes
) {
}
