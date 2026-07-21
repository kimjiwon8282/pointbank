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

# 메인 큐: RedrivePolicy(중첩 JSON) 때문에 shorthand --attributes 파서가 깨진다
# ("Expected: '=', received: '\"'"). → --attributes 를 JSON 맵(file://)으로 전달하고
# RedrivePolicy 는 JSON 문자열로 인코딩한다.
cat > /tmp/ledger-command-attrs.json <<EOF
{
  "FifoQueue": "true",
  "VisibilityTimeout": "60",
  "ContentBasedDeduplication": "false",
  "RedrivePolicy": "{\"deadLetterTargetArn\":\"$LEDGER_COMMAND_DLQ_ARN\",\"maxReceiveCount\":\"5\"}"
}
EOF
awslocal sqs create-queue \
  --queue-name ledger-command-queue.fifo \
  --attributes file:///tmp/ledger-command-attrs.json

cat > /tmp/securities-result-attrs.json <<EOF
{
  "FifoQueue": "true",
  "VisibilityTimeout": "60",
  "ContentBasedDeduplication": "false",
  "RedrivePolicy": "{\"deadLetterTargetArn\":\"$SECURITIES_RESULT_DLQ_ARN\",\"maxReceiveCount\":\"5\"}"
}
EOF
awslocal sqs create-queue \
  --queue-name securities-result-queue.fifo \
  --attributes file:///tmp/securities-result-attrs.json
