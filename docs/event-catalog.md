# NanoBank Ledger — Event Catalog

## Propósito

Catálogo oficial de todos los eventos de dominio publicados en la plataforma NanoBank Ledger. Este documento es la fuente de verdad para la definición de contratos de eventos, topics Kafka y ownership.

---

## Convenciones

### Nomenclatura de Topics

```
nanobank.{dominio}.{entidad}.{accion}

Ejemplos:
  nanobank.identity.token.issued
  nanobank.transaction.transfer.created
  nanobank.ledger.entry.created
```

### Estructura Base de Todo Evento

```json
{
  "eventId": "uuid-v4",
  "eventType": "NombreDelEvento",
  "eventVersion": "1.0",
  "occurredAt": "2026-01-01T00:00:00.000Z",
  "correlationId": "uuid-v4",
  "sessionId": "uuid-v4",
  "transactionId": "uuid-v4",
  "idempotencyKey": "uuid-v4",
  "sourceService": "nombre-del-servicio",
  "payload": { }
}
```

### Versionado

Los eventos se versionan con `eventVersion`. Cambios breaking requieren nueva versión. Los consumidores deben tolerar campos adicionales (schema evolution forward-compatible).

---

## Catálogo por Dominio

---

## Identity Domain

### Topic: `nanobank.identity.token.issued`

**Evento:** `TokenIssued`

**Owner:** identity-service

**Descripción:** Se emite cuando un JWT es generado exitosamente.

**Schema:**

```json
{
  "eventId": "string (uuid)",
  "eventType": "TokenIssued",
  "eventVersion": "1.0",
  "occurredAt": "datetime",
  "correlationId": "string (uuid)",
  "sessionId": "string (uuid)",
  "transactionId": null,
  "idempotencyKey": "string (uuid)",
  "sourceService": "identity-service",
  "payload": {
    "userId": "string (uuid)",
    "tokenId": "string (uuid)",
    "roles": ["string"],
    "permissions": ["string"],
    "expiresAt": "datetime",
    "tokenType": "ACCESS | REFRESH"
  }
}
```

**Consumidores:** audit-service

---

### Topic: `nanobank.identity.token.revoked`

**Evento:** `TokenRevoked`

**Owner:** identity-service

**Schema:**

```json
{
  "payload": {
    "tokenId": "string (uuid)",
    "userId": "string (uuid)",
    "reason": "LOGOUT | EXPIRED | SUSPICIOUS_ACTIVITY"
  }
}
```

**Consumidores:** audit-service

---

## Authentication Domain

### Topic: `nanobank.auth.session.created`

**Evento:** `UserLoggedIn`

**Owner:** auth-service

```json
{
  "payload": {
    "userId": "string (uuid)",
    "sessionId": "string (uuid)",
    "ipAddress": "string",
    "userAgent": "string",
    "mfaUsed": "boolean",
    "loggedInAt": "datetime"
  }
}
```

**Consumidores:** audit-service, notification-service

---

### Topic: `nanobank.auth.session.terminated`

**Evento:** `UserLoggedOut`

**Owner:** auth-service

```json
{
  "payload": {
    "userId": "string (uuid)",
    "sessionId": "string (uuid)",
    "reason": "USER_INITIATED | TIMEOUT | FORCED",
    "terminatedAt": "datetime"
  }
}
```

**Consumidores:** audit-service

---

## Customer Domain

### Topic: `nanobank.customer.profile.created`

**Evento:** `UserCreated`

**Owner:** user-service

```json
{
  "payload": {
    "userId": "string (uuid)",
    "email": "string",
    "fullName": "string",
    "documentType": "DNI | PASSPORT | NIT",
    "documentNumber": "string",
    "phone": "string",
    "status": "PENDING_VERIFICATION",
    "createdAt": "datetime"
  }
}
```

**Consumidores:** account-service, audit-service, notification-service

---

### Topic: `nanobank.customer.profile.updated`

**Evento:** `UserUpdated`

**Owner:** user-service

```json
{
  "payload": {
    "userId": "string (uuid)",
    "changedFields": ["string"],
    "updatedAt": "datetime"
  }
}
```

**Consumidores:** audit-service

---

### Topic: `nanobank.customer.profile.deactivated`

**Evento:** `UserDeactivated`

**Owner:** user-service

```json
{
  "payload": {
    "userId": "string (uuid)",
    "reason": "string",
    "deactivatedAt": "datetime"
  }
}
```

**Consumidores:** account-service, audit-service, notification-service

---

## Account Domain

### Topic: `nanobank.account.account.created`

**Evento:** `AccountCreated`

**Owner:** account-service

```json
{
  "payload": {
    "accountId": "string (uuid)",
    "userId": "string (uuid)",
    "accountNumber": "string",
    "accountType": "SAVINGS | CHECKING | INVESTMENT",
    "currency": "COP | USD",
    "status": "ACTIVE",
    "createdAt": "datetime"
  }
}
```

**Consumidores:** wallet-service, limits-service, audit-service

---

### Topic: `nanobank.account.account.blocked`

**Evento:** `AccountBlocked`

**Owner:** account-service

```json
{
  "payload": {
    "accountId": "string (uuid)",
    "userId": "string (uuid)",
    "reason": "FRAUD | REGULATORY | USER_REQUEST",
    "blockedAt": "datetime"
  }
}
```

**Consumidores:** transaction-service, audit-service, notification-service

---

### Topic: `nanobank.account.account.closed`

**Evento:** `AccountClosed`

**Owner:** account-service

```json
{
  "payload": {
    "accountId": "string (uuid)",
    "userId": "string (uuid)",
    "closedAt": "datetime"
  }
}
```

**Consumidores:** wallet-service, audit-service, notification-service

---

## Wallet Domain

### Topic: `nanobank.wallet.wallet.created`

**Evento:** `WalletCreated`

**Owner:** wallet-service

```json
{
  "payload": {
    "walletId": "string (uuid)",
    "accountId": "string (uuid)",
    "userId": "string (uuid)",
    "name": "string",
    "category": "SAVINGS | EXPENSES | INVESTMENTS | CUSTOM",
    "initialBalance": "decimal",
    "currency": "COP | USD",
    "status": "ACTIVE",
    "createdAt": "datetime"
  }
}
```

**Consumidores:** audit-service, notification-service

---

### Topic: `nanobank.wallet.wallet.updated`

**Evento:** `WalletUpdated`

**Owner:** wallet-service

```json
{
  "payload": {
    "walletId": "string (uuid)",
    "changedFields": ["string"],
    "updatedAt": "datetime"
  }
}
```

**Consumidores:** audit-service

---

### Topic: `nanobank.wallet.transfer.completed`

**Evento:** `WalletTransferCompleted`

**Owner:** wallet-service

**Descripción:** Transferencia entre billeteras del mismo usuario completada.

```json
{
  "payload": {
    "transferId": "string (uuid)",
    "sourceWalletId": "string (uuid)",
    "destinationWalletId": "string (uuid)",
    "amount": "decimal",
    "currency": "COP | USD",
    "completedAt": "datetime"
  }
}
```

**Consumidores:** ledger-service, audit-service, notification-service

---

## Transaction Domain

### Topic: `nanobank.transaction.transfer.created`

**Evento:** `TransactionCreated`

**Owner:** transaction-service

**Descripción:** Solicitud de transacción recibida. Dispara la Saga.

```json
{
  "payload": {
    "transactionId": "string (uuid)",
    "idempotencyKey": "string (uuid)",
    "sourceAccountId": "string (uuid)",
    "sourceWalletId": "string (uuid)",
    "destinationAccountId": "string (uuid)",
    "destinationWalletId": "string (uuid)",
    "amount": "decimal",
    "currency": "COP | USD",
    "transactionType": "INTERNAL_TRANSFER | EXTERNAL_TRANSFER | INCOME | EXPENSE",
    "description": "string",
    "status": "PENDING",
    "createdAt": "datetime"
  }
}
```

**Consumidores:** risk-service, limits-service, ledger-service, audit-service

---

### Topic: `nanobank.transaction.transfer.completed`

**Evento:** `TransactionCompleted`

**Owner:** transaction-service

```json
{
  "payload": {
    "transactionId": "string (uuid)",
    "idempotencyKey": "string (uuid)",
    "status": "COMPLETED",
    "ledgerEntryId": "string (uuid)",
    "completedAt": "datetime"
  }
}
```

**Consumidores:** notification-service, audit-service, reconciliation-service

---

### Topic: `nanobank.transaction.transfer.failed`

**Evento:** `TransactionFailed`

**Owner:** transaction-service

```json
{
  "payload": {
    "transactionId": "string (uuid)",
    "idempotencyKey": "string (uuid)",
    "reason": "RISK_REJECTED | LIMIT_EXCEEDED | LEDGER_ERROR | INSUFFICIENT_FUNDS",
    "errorCode": "string",
    "failedAt": "datetime"
  }
}
```

**Consumidores:** notification-service, audit-service, reconciliation-service

---

## Risk Domain

### Topic: `nanobank.risk.validation.approved`

**Evento:** `RiskValidated`

**Owner:** risk-service

```json
{
  "payload": {
    "transactionId": "string (uuid)",
    "riskScore": "decimal",
    "validatedAt": "datetime"
  }
}
```

**Consumidores:** transaction-service

---

### Topic: `nanobank.risk.validation.rejected`

**Evento:** `RiskRejected`

**Owner:** risk-service

```json
{
  "payload": {
    "transactionId": "string (uuid)",
    "riskScore": "decimal",
    "reason": "string",
    "ruleViolated": "string",
    "rejectedAt": "datetime"
  }
}
```

**Consumidores:** transaction-service, audit-service

---

## Limits Domain

### Topic: `nanobank.limits.validation.approved`

**Evento:** `LimitValidated`

**Owner:** limits-service

```json
{
  "payload": {
    "transactionId": "string (uuid)",
    "accountId": "string (uuid)",
    "dailyUsed": "decimal",
    "monthlyUsed": "decimal",
    "validatedAt": "datetime"
  }
}
```

**Consumidores:** transaction-service

---

### Topic: `nanobank.limits.validation.exceeded`

**Evento:** `LimitExceeded`

**Owner:** limits-service

```json
{
  "payload": {
    "transactionId": "string (uuid)",
    "accountId": "string (uuid)",
    "limitType": "DAILY | MONTHLY",
    "limitAmount": "decimal",
    "requestedAmount": "decimal",
    "exceededAt": "datetime"
  }
}
```

**Consumidores:** transaction-service, notification-service, audit-service

---

## Ledger Domain

### Topic: `nanobank.ledger.entry.created`

**Evento:** `LedgerEntryCreated`

**Owner:** ledger-service

```json
{
  "payload": {
    "entryId": "string (uuid)",
    "transactionId": "string (uuid)",
    "debitAccountId": "string (uuid)",
    "creditAccountId": "string (uuid)",
    "amount": "decimal",
    "currency": "COP | USD",
    "entryType": "DEBIT | CREDIT",
    "balanceAfter": "decimal",
    "createdAt": "datetime"
  }
}
```

**Consumidores:** transaction-service, reconciliation-service, audit-service

---

### Topic: `nanobank.ledger.entry.failed`

**Evento:** `LedgerEntryFailed`

**Owner:** ledger-service

```json
{
  "payload": {
    "transactionId": "string (uuid)",
    "reason": "INSUFFICIENT_FUNDS | ACCOUNT_BLOCKED | DUPLICATE_ENTRY",
    "failedAt": "datetime"
  }
}
```

**Consumidores:** transaction-service, audit-service

---

## Reconciliation Domain

### Topic: `nanobank.reconciliation.process.completed`

**Evento:** `ReconciliationCompleted`

**Owner:** reconciliation-service

```json
{
  "payload": {
    "reconciliationId": "string (uuid)",
    "periodStart": "datetime",
    "periodEnd": "datetime",
    "totalTransactions": "integer",
    "matchedTransactions": "integer",
    "unmatchedTransactions": "integer",
    "completedAt": "datetime"
  }
}
```

**Consumidores:** audit-service

---

### Topic: `nanobank.reconciliation.process.failed`

**Evento:** `ReconciliationFailed`

**Owner:** reconciliation-service

```json
{
  "payload": {
    "reconciliationId": "string (uuid)",
    "reason": "string",
    "affectedTransactionIds": ["string"],
    "failedAt": "datetime"
  }
}
```

**Consumidores:** audit-service, notification-service

---

## Dead Letter Queue

Todos los topics tienen un DLQ correspondiente con el sufijo `.dlq`:

```
nanobank.transaction.transfer.created.dlq
nanobank.ledger.entry.created.dlq
nanobank.risk.validation.approved.dlq
...
```

Los eventos en DLQ son consumidos por `reconciliation-service` para análisis y recuperación.

---

## Resumen de Topics

| Topic | Owner | Consumidores principales |
|---|---|---|
| nanobank.identity.token.issued | identity-service | audit |
| nanobank.identity.token.revoked | identity-service | audit |
| nanobank.auth.session.created | auth-service | audit, notification |
| nanobank.auth.session.terminated | auth-service | audit |
| nanobank.customer.profile.created | user-service | account, audit, notification |
| nanobank.customer.profile.updated | user-service | audit |
| nanobank.customer.profile.deactivated | user-service | account, audit, notification |
| nanobank.account.account.created | account-service | wallet, limits, audit |
| nanobank.account.account.blocked | account-service | transaction, audit, notification |
| nanobank.account.account.closed | account-service | wallet, audit, notification |
| nanobank.wallet.wallet.created | wallet-service | audit, notification |
| nanobank.wallet.wallet.updated | wallet-service | audit |
| nanobank.wallet.transfer.completed | wallet-service | ledger, audit, notification |
| nanobank.transaction.transfer.created | transaction-service | risk, limits, ledger, audit |
| nanobank.transaction.transfer.completed | transaction-service | notification, audit, reconciliation |
| nanobank.transaction.transfer.failed | transaction-service | notification, audit, reconciliation |
| nanobank.risk.validation.approved | risk-service | transaction |
| nanobank.risk.validation.rejected | risk-service | transaction, audit |
| nanobank.limits.validation.approved | limits-service | transaction |
| nanobank.limits.validation.exceeded | limits-service | transaction, notification, audit |
| nanobank.ledger.entry.created | ledger-service | transaction, reconciliation, audit |
| nanobank.ledger.entry.failed | ledger-service | transaction, audit |
| nanobank.reconciliation.process.completed | reconciliation-service | audit |
| nanobank.reconciliation.process.failed | reconciliation-service | audit, notification |
