# Securities Ledger Cash Account Integration

## Stage 1: Synchronous Call

Stage 1 used a synchronous HTTP call from Securities Service to Ledger Service.

- Securities account creation stored `securities_accounts` as `PENDING_CASH_ACCOUNT`.
- Securities Service then called `POST /internal/ledger/securities/cash/accounts`.
- Ledger Service created or returned the member's `SECURITIES_CASH` ledger account.
- Securities Service marked the securities account as `ACTIVE` only after Ledger succeeded.

The limitation was that Securities DB and Ledger DB are separate databases, so this was not a single atomic transaction. A process crash or network failure could leave partial state.

## Stage 2: SQS + Outbox

Stage 2 replaces the direct call with SQS + Outbox based eventual consistency.

- Securities account creation stores `securities_accounts` as `PENDING_CASH_ACCOUNT`.
- The same Securities DB transaction stores a `CASH_ACCOUNT_CREATE_REQUESTED` row in `outbox_events`.
- Securities Outbox Publisher sends pending events to `ledger-command-queue.fifo`.
- Ledger Command Consumer creates or reuses the member's `SECURITIES_CASH` account.
- The same Ledger DB transaction stores a `CASH_ACCOUNT_CREATED` row in Ledger `outbox_events`.
- Ledger Outbox Publisher sends the result to `securities-result-queue.fifo`.
- Securities Result Consumer marks the securities account as `ACTIVE` after receiving `CASH_ACCOUNT_CREATED`.

This intentionally does not try to wrap Securities DB and Ledger DB in one transaction. The system accepts temporary `PENDING_CASH_ACCOUNT` state and converges to `ACTIVE` after Ledger completion.

## Remaining Work

- Add outbox `FAILED` follow-up handling.
- Define order-domain DLQ replay and correction policy before adding order flows.
- Add alerting for DLQ cleanup failures and outbox `FAILED` rows.
- Add focused tests for publishers, consumers, idempotency, and status transitions.

## Stage 3: DLQ and Automatic Account-Opening Cleanup

Stage 3 adds DLQ isolation and automatic cleanup for the securities account opening flow.

The product decision is that securities account creation should feel like one operation to the user. If the Ledger cash-account integration message reaches a DLQ, this project treats that account opening as finally failed and cleans up the pending account-opening data automatically.

Queues:

- `ledger-command-queue.fifo` redrives to `ledger-command-dlq.fifo` after `maxReceiveCount=5`.
- `securities-result-queue.fifo` redrives to `securities-result-dlq.fifo` after `maxReceiveCount=5`.

`ledger-command-dlq.fifo` means Ledger repeatedly failed to process `CASH_ACCOUNT_CREATE_REQUESTED`.

- Securities Service consumes this DLQ.
- If the securities account is already gone, the DLQ message is deleted.
- If the securities account is `ACTIVE` or `SUSPENDED`, cleanup is skipped and the DLQ message is deleted.
- If the securities account is `PENDING_CASH_ACCOUNT`, Securities calls Ledger cleanup and then deletes the pending securities account.

`securities-result-dlq.fifo` means Ledger created or reused `SECURITIES_CASH`, but Securities repeatedly failed to apply `CASH_ACCOUNT_CREATED`.

- Securities Service consumes this DLQ.
- If the securities account is already gone, Securities still attempts Ledger cleanup using `memberId` from the event.
- If the securities account is `ACTIVE` or `SUSPENDED`, cleanup is skipped and the DLQ message is deleted.
- If the securities account is `PENDING_CASH_ACCOUNT`, Securities calls Ledger cleanup and then deletes the pending securities account.

Ledger cleanup remains an internal service-to-service compensation API:

- `DELETE /internal/ledger/securities/cash/accounts?memberId={memberId}`
- It is called by Securities DLQ cleanup consumers as an internal compensation API.
- It deletes only a `SECURITIES_CASH` account with `balance=0`, `reserved_balance=0`, and no `ledger_entries`.

`outbox_events.status=FAILED` is not DLQ. It means publishing to SQS repeatedly failed before a consumer received the message.

- It is not handled by DLQ consumers.
- This stage does not automatically clean up outbox `FAILED` rows.
- A later stage can add a batch cleanup or compensation policy for outbox `FAILED`.

Physical deletion is allowed only for the account creation integration stage:

- `securities_accounts.status = PENDING_CASH_ACCOUNT`.
- Ledger `SECURITIES_CASH` account is missing, or has `balance=0` and `reserved_balance=0`.
- No `ledger_entries` exist for the Ledger cash account.
- Order, execution, and holding tables do not exist yet. When added, cleanup must check that they are empty before deletion.

Physical deletion is forbidden when:

- Securities account is `ACTIVE` or `SUSPENDED`.
- Ledger cash `balance > 0`.
- Ledger cash `reserved_balance > 0`.
- Any `ledger_entries` exist.
- Orders, executions, or holdings exist.

DLQ cleanup consumers delete a DLQ message only after cleanup succeeds or when the message no longer requires cleanup, such as an already deleted, `ACTIVE`, or `SUSPENDED` securities account. If cleanup fails, the DLQ message is not deleted and remains available after the visibility timeout.

This policy applies only to the initial securities account opening flow. Future order, execution, and holding flows should not automatically delete DLQ messages; they should use replay, correction, or reconciliation-centered policies.
