package com.pointbank.ledger.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pointbank.sqs")
public record SqsProperties(
        String endpoint,
        String region,
        String ledgerCommandQueueUrl,
        String ledgerCommandDlqUrl,
        String securitiesResultQueueUrl,
        String securitiesResultDlqUrl
) {
}
