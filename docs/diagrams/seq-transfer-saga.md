# Diagrama de Secuencia — Saga de Transferencia Interna

## Flujo: Transferencia exitosa entre billeteras

```
Cliente          API Gateway      transaction-service    risk-service      limits-service     ledger-service    notification-service   audit-service
  |                   |                  |                    |                  |                   |                   |                  |
  |--POST /transfer-->|                  |                    |                  |                   |                   |                  |
  |  (Idempotency-Key)|                  |                    |                  |                   |                   |                  |
  |                   |--validate JWT--->|                    |                  |                   |                   |                  |
  |                   |                  |                    |                  |                   |                   |                  |
  |                   |                  |--check Redis------->|                 |                   |                   |                  |
  |                   |                  |  (idempotency key) |                 |                   |                   |                  |
  |                   |                  |<--not found--------|                 |                   |                   |                  |
  |                   |                  |                    |                 |                   |                   |                  |
  |                   |                  |--INSERT transaction (PENDING)        |                   |                   |                  |
  |                   |                  |--INSERT outbox_event                 |                   |                   |                  |
  |                   |                  |--COMMIT TX--------------------------------->            |                   |                  |
  |                   |                  |                    |                 |                   |                   |                  |
  |                   |<--202 ACCEPTED---|                    |                 |                   |                   |                  |
  |<--202 ACCEPTED----|                  |                    |                 |                   |                   |                  |
  |                   |                  |                    |                 |                   |                   |                  |
  |                   |       [Outbox Poller publica a Kafka]                   |                   |                   |                  |
  |                   |                  |--TransactionCreated-->|              |                   |                   |                  |
  |                   |                  |--TransactionCreated---+------------->|                   |                   |                  |
  |                   |                  |--TransactionCreated---+-------------------------------------> (audit)        |                  |
  |                   |                  |                    |              |                     |                   |                  |
  |                   |         [risk-service evalúa]         |              |                     |                   |                  |
  |                   |                  |                    |--eval rules--|                     |                   |                  |
  |                   |                  |                    |--INSERT risk_evaluation            |                   |                  |
  |                   |                  |                    |--INSERT outbox_event               |                   |                  |
  |                   |                  |                    |--COMMIT TX                         |                   |                  |
  |                   |                  |                    |                  |                  |                   |                  |
  |                   |         [limits-service evalúa]                          |                  |                   |                  |
  |                   |                  |                    |              |--check limits       |                   |                  |
  |                   |                  |                    |              |--UPDATE limit_usage  |                   |                  |
  |                   |                  |                    |              |--INSERT outbox_event |                   |                  |
  |                   |                  |                    |              |--COMMIT TX           |                   |                  |
  |                   |                  |                    |              |                      |                   |                  |
  |                   |       [Outbox Pollers publican a Kafka]               |                    |                   |                  |
  |                   |                  |<--RiskValidated----|              |                     |                   |                  |
  |                   |                  |<--LimitValidated---+--------------+                     |                   |                  |
  |                   |                  |                    |              |                      |                   |                  |
  |                   |         [transaction-service: ambas validaciones OK]                       |                   |                  |
  |                   |                  |--UPDATE saga_state (risk=true, limit=true)              |                   |                  |
  |                   |                  |--INSERT outbox_event (LedgerEntryRequest)               |                   |                  |
  |                   |                  |--COMMIT TX                                              |                   |                  |
  |                   |                  |                    |              |                      |                   |                  |
  |                   |       [Outbox Poller publica a Kafka]                                      |                   |                  |
  |                   |                  |--LedgerEntryRequest--------------------------------->  |                   |                  |
  |                   |                  |                    |              |                  |  |                   |                  |
  |                   |         [ledger-service registra partida doble]                        |  |                   |                  |
  |                   |                  |                    |              |             |--INSERT DEBIT entry     |                  |
  |                   |                  |                    |              |             |--INSERT CREDIT entry    |                  |
  |                   |                  |                    |              |             |--UPDATE account_balance |                  |
  |                   |                  |                    |              |             |--UPDATE wallet_balance  |                  |
  |                   |                  |                    |              |             |--INSERT outbox_event    |                  |
  |                   |                  |                    |              |             |--COMMIT TX              |                  |
  |                   |                  |                    |              |                  |                   |                  |
  |                   |       [Outbox Poller publica a Kafka]                                  |                   |                  |
  |                   |                  |<--LedgerEntryCreated-----------------------------|  |                   |                  |
  |                   |                  |                    |              |                  |                   |                  |
  |                   |         [transaction-service: saga completada]                         |                   |                  |
  |                   |                  |--UPDATE transaction (COMPLETED)                      |                   |                  |
  |                   |                  |--INSERT outbox_event (TransactionCompleted)           |                   |                  |
  |                   |                  |--COMMIT TX                                           |                   |                  |
  |                   |                  |                    |              |                  |                   |                  |
  |                   |       [Outbox Poller publica a Kafka]                                  |                   |                  |
  |                   |                  |--TransactionCompleted------------------------------->| (notification)    |                  |
  |                   |                  |--TransactionCompleted----------------------------------------------->  | (audit)          |
  |                   |                  |                    |              |                  |                   |                  |
  |                   |         [notification-service envía push/email]                        |                   |                  |
  |                   |                  |                    |              |         |--INSERT notification      |                  |
  |                   |                  |                    |              |         |--SEND push/email          |                  |
  |                   |                  |                    |              |         |--UPDATE status SENT       |                  |
```

---

## Flujo: Transferencia fallida por riesgo rechazado

```
  ...
  [risk-service evalúa: regla violada]
  |--INSERT outbox_event (RiskRejected)
  |--COMMIT TX

  [transaction-service consume RiskRejected]
  |--UPDATE transaction (FAILED, reason=RISK_REJECTED)
  |--INSERT outbox_event (TransactionFailed)
  |--COMMIT TX

  [notification-service consume TransactionFailed]
  |--SEND notificación de fallo al usuario

  [audit-service consume RiskRejected + TransactionFailed]
  |--INSERT audit_event (ambos eventos)
```

---

## Invariantes de la Saga

1. Un `TransactionCreated` activa **en paralelo** `risk-service` y `limits-service`.
2. La publicación del `LedgerEntryRequest` ocurre **solo cuando ambos** validan exitosamente.
3. Si cualquier paso falla, el siguiente en la cadena publica un evento de fallo.
4. `transaction-service` es el único que actualiza el estado final de la transacción.
5. Todo paso usa **Transactional Outbox**: primero persiste en DB, luego Kafka publica.
6. La idempotency key previene procesamiento duplicado en todos los nodos.
