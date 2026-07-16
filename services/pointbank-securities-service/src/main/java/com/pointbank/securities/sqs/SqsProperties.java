package com.pointbank.securities.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pointbank.sqs")
public record SqsProperties(
        String endpoint,
        String region,
        String ledgerCommandQueueUrl,
        String ledgerCommandDlqUrl,
        String ledgerOrderCommandQueueUrl,
        String ledgerOrderCommandDlqUrl,
        String securitiesResultQueueUrl,
        String securitiesResultDlqUrl,
        String securitiesOrderResultQueueUrl,
        String securitiesOrderResultDlqUrl
) {
}
