#!/bin/sh
set -eu

REGION="${AWS_DEFAULT_REGION:-ap-northeast-2}"

create_dlq() {
  awslocal sqs create-queue \
    --region "$REGION" \
    --queue-name "$1" \
    --attributes FifoQueue=true,VisibilityTimeout=60,ContentBasedDeduplication=false \
    --query 'QueueUrl' \
    --output text
}

get_queue_arn() {
  awslocal sqs get-queue-attributes \
    --region "$REGION" \
    --queue-url "$1" \
    --attribute-names QueueArn \
    --query 'Attributes.QueueArn' \
    --output text
}

create_source_queue() {
  ATTRIBUTES=$(printf '%s' "{\"FifoQueue\":\"true\",\"VisibilityTimeout\":\"60\",\"ContentBasedDeduplication\":\"false\",\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$2\\\",\\\"maxReceiveCount\\\":\\\"5\\\"}\"}")
  awslocal sqs create-queue \
    --region "$REGION" \
    --queue-name "$1" \
    --attributes "$ATTRIBUTES"
}

LEDGER_COMMAND_DLQ_URL=$(create_dlq ledger-command-dlq.fifo)
SECURITIES_RESULT_DLQ_URL=$(create_dlq securities-result-dlq.fifo)
LEDGER_ORDER_COMMAND_DLQ_URL=$(create_dlq ledger-order-command-dlq.fifo)
SECURITIES_ORDER_RESULT_DLQ_URL=$(create_dlq securities-order-result-dlq.fifo)

LEDGER_COMMAND_DLQ_ARN=$(get_queue_arn "$LEDGER_COMMAND_DLQ_URL")
SECURITIES_RESULT_DLQ_ARN=$(get_queue_arn "$SECURITIES_RESULT_DLQ_URL")
LEDGER_ORDER_COMMAND_DLQ_ARN=$(get_queue_arn "$LEDGER_ORDER_COMMAND_DLQ_URL")
SECURITIES_ORDER_RESULT_DLQ_ARN=$(get_queue_arn "$SECURITIES_ORDER_RESULT_DLQ_URL")

create_source_queue ledger-command-queue.fifo "$LEDGER_COMMAND_DLQ_ARN"
create_source_queue securities-result-queue.fifo "$SECURITIES_RESULT_DLQ_ARN"
create_source_queue ledger-order-command-queue.fifo "$LEDGER_ORDER_COMMAND_DLQ_ARN"
create_source_queue securities-order-result-queue.fifo "$SECURITIES_ORDER_RESULT_DLQ_ARN"
