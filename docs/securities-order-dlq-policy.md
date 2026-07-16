# 모의투자 매수 주문 DLQ 철회 정책

주문 전용 DLQ는 재처리보다 **철회 우선**으로 처리한다. 철회는 주문 row나 원장 이력을 삭제하는 작업이 아니라 주문 상태 변경과 필요한 반대 원장 거래를 기록하는 작업이다.

## ledger-order-command-dlq.fifo

- `BUY_ORDER_REQUESTED`를 `STOCKBUY-{orderNo}` 원장 요청과 대조한다.
- 원 매수 차감이 없거나 `COMPLETED`가 아니면 돈이 움직이지 않은 것으로 판단하고 `BUY_FUNDS_FAILED`를 `LEDGER_ORDER_COMMAND_DLQ` 사유로 발행한다.
- Securities는 이 실패 결과를 받아 주문을 `CANCELED`로 변경한다.
- 원 매수 차감이 `COMPLETED`이면 취소 결과를 발행하지 않는다. `BUY_FUNDS_DEBITED` 결과 Outbox를 보장하여 정상 결과 처리 또는 주문 결과 DLQ 보상 흐름으로 연결한다.

## securities-order-result-dlq.fifo

### BUY_FUNDS_FAILED

- 예수금이 차감되지 않은 실패 결과로 간주한다.
- `REQUESTED` 또는 `FAILED` 주문은 `CANCELED`로 변경한다.
- `FUNDS_COMPLETED` 또는 `COMPLETED`와 충돌하면 `MANUAL_REVIEW`로 변경한다.

### BUY_FUNDS_DEBITED

- Ledger 차감은 성공했지만 Securities의 체결·보유종목 반영은 완료되지 않은 결과다.
- 주문이 `REQUESTED` 또는 `FUNDS_COMPLETED`일 때만 Ledger 보상 입금을 요청한다.
- 보상은 `SECURITIES_BUY_REVERSAL` transfer와 `STOCK_BUY_REVERSAL` CREDIT 원장 엔트리로 별도 기록한다.
- 보상 성공 후 주문을 `REVERSED`로 변경한다.
- 보상 호출 실패 또는 결과 불일치는 주문을 `MANUAL_REVIEW`로 변경하고 반복 DLQ 처리는 중단한다.
- 이미 `COMPLETED`인 주문은 체결·보유종목 반영이 끝난 상태일 수 있으므로 자동 보상하지 않는다.

## 불변 정책

- 주문 row를 물리 삭제하지 않는다.
- 기존 `STOCK_BUY` 원장 엔트리를 삭제하거나 수정하지 않는다.
- 보상 입금은 `STOCKREV-{orderNo}` requestNo로 멱등 처리한다.
- 체결 완료 주문의 보유종목 반대 정정은 이 단계에서 자동 수행하지 않는다.
- 사용자는 `CANCELED`, `FAILED`, `REVERSED` 상태를 확인하고 새로운 Idempotency-Key로 다시 주문할 수 있다.

## 기존 로컬 DB 수동 반영

MySQL init SQL은 새 볼륨 생성 시에만 자동 적용된다. 기존 `securities_db` 볼륨에는 다음과 같이 주문 상태 CHECK 제약을 다시 생성해야 한다. 실제 제약 이름은 `SHOW CREATE TABLE securities_orders`로 먼저 확인한다.

```sql
ALTER TABLE securities_orders DROP CHECK chk_securities_orders_status;
ALTER TABLE securities_orders
    ADD CONSTRAINT chk_securities_orders_status
    CHECK (status IN (
        'REQUESTED', 'FUNDS_COMPLETED', 'COMPLETED', 'FAILED',
        'MANUAL_REVIEW', 'CANCELED', 'REVERSED'
    ));
```
