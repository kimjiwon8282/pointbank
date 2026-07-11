#!/bin/sh
set -e

LEDGER_COMMAND_DLQ_URL=$(awslocal sqs create-queue \
  --queue-name ledger-command-dlq.fifo \
  --attributes FifoQueue=true,VisibilityTimeout=60,ContentBasedDeduplication=false \
  --query 'QueueUrl' \
  --output text)

SECURITIES_RESULT_DLQ_URL=$(awslocal sqs create-queue \
  --queue-name securities-result-dlq.fifo \
  --attributes FifoQueue=true,VisibilityTimeout=60,ContentBasedDeduplication=false \
  --query 'QueueUrl' \
  --output text)

LEDGER_COMMAND_DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$LEDGER_COMMAND_DLQ_URL" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)

SECURITIES_RESULT_DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$SECURITIES_RESULT_DLQ_URL" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)

awslocal sqs create-queue \
  --queue-name ledger-command-queue.fifo \
  --attributes "FifoQueue=true,VisibilityTimeout=60,ContentBasedDeduplication=false,RedrivePolicy={\"deadLetterTargetArn\":\"$LEDGER_COMMAND_DLQ_ARN\",\"maxReceiveCount\":\"5\"}"

awslocal sqs create-queue \
  --queue-name securities-result-queue.fifo \
  --attributes "FifoQueue=true,VisibilityTimeout=60,ContentBasedDeduplication=false,RedrivePolicy={\"deadLetterTargetArn\":\"$SECURITIES_RESULT_DLQ_ARN\",\"maxReceiveCount\":\"5\"}"
