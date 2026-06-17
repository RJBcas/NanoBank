# Diagrama de Secuencia — Registro en Ledger (Partida Doble)

## Flujo: LedgerEntryCreated desde Saga de Transferencia

```
transaction-service    Kafka               ledger-service          audit-service
      |                  |                      |                       |
      |--TransactionCreated (via Outbox)-------->|                      |
      |                  |                      |                       |
      |                  |         [ledger-service consume TransactionCreated]
      |                  |                      |                       |
      |                  |                      |--check duplicate: SELECT WHERE transaction_id
      |                  |                      |  (si existe → ack sin procesar, idempotente)
      |                  |                      |                       |
      |                  |                      |--validate account ACTIVE
      |                  |                      |--validate balance >= amount (para DEBIT)
      |                  |                      |                       |
      |                  |                      |  [BEGIN TRANSACTION]
      |                  |                      |--INSERT ledger_entry (DEBIT, sourceAccount)
      |                  |                      |   entry_id, transaction_id, amount, balance_after
      |                  |                      |--INSERT ledger_entry (CREDIT, destinationAccount)
      |                  |                      |   entry_id, transaction_id, amount, balance_after
      |                  |                      |--UPDATE account_balances (source: -amount)
      |                  |                      |--UPDATE account_balances (dest: +amount)
      |                  |                      |--UPDATE wallet_balances (si aplica)
      |                  |                      |--INSERT outbox_event (LedgerEntryCreated)
      |                  |                      |  [COMMIT TRANSACTION]
      |                  |                      |                       |
      |                  |  [Outbox Poller publica LedgerEntryCreated]  |
      |                  |<----LedgerEntryCreated|                      |
      |<--LedgerEntryCreated (Kafka consumer)----|                      |
      |                  |                      |--LedgerEntryCreated-->| (audit persiste)
```

---

## Flujo: LedgerEntryFailed (saldo insuficiente)

```
transaction-service    Kafka               ledger-service          transaction-service (consumer)
      |                  |                      |                       |
      |--TransactionCreated-------------------->|                       |
      |                  |                      |                       |
      |                  |         [ledger-service valida]              |
      |                  |                      |--validate balance >= amount
      |                  |                      |  FAILS: balance = 500, amount = 1000
      |                  |                      |                       |
      |                  |                      |--INSERT outbox_event (LedgerEntryFailed)
      |                  |                      |--COMMIT TX            |
      |                  |                      |                       |
      |                  |  [Outbox Poller publica LedgerEntryFailed]   |
      |                  |<----LedgerEntryFailed-|                      |
      |                  |--LedgerEntryFailed-------------------------->|
      |                  |                      |          |--UPDATE transaction (FAILED, INSUFFICIENT_FUNDS)
      |                  |                      |          |--INSERT outbox_event (TransactionFailed)
      |                  |                      |          |--COMMIT TX
```

---

## Principio de Inmutabilidad

```
[NUNCA ocurre en ledger-service]

❌  UPDATE ledger_entries SET amount = X WHERE id = Y
❌  DELETE FROM ledger_entries WHERE transaction_id = Z

[Lo que SÍ ocurre para compensaciones]

✅  INSERT ledger_entries (entry_type=CREDIT, description='COMPENSATION for entry_id=X')
    → compensated_by FK apunta a la entrada original
```

---

## Validación de Partida Doble

El ledger-service valida antes de commitar que:

```
sum(DEBIT entries para transactionId) == sum(CREDIT entries para transactionId)

Si no se cumple → ROLLBACK + LedgerEntryFailed
```

---

## Reconstrucción de Saldo

El endpoint `POST /ledger/reconstruct/{accountId}` ejecuta:

```sql
SELECT
  SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) -
  SUM(CASE WHEN entry_type = 'DEBIT'  THEN amount ELSE 0 END) AS reconstructed_balance
FROM ledger_entries
WHERE account_id = :accountId
  AND status = 'ACTIVE'
ORDER BY created_at ASC;
```

El resultado se usa para actualizar `account_balances.balance` y detectar discrepancias con el valor almacenado.
