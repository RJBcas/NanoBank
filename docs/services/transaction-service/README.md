# transaction-service

## Responsabilidad

Microservicio responsable de la creación y orquestación funcional del movimiento financiero. Es el punto de entrada para todas las operaciones transaccionales y el coordinador de la **Saga Choreography** de transferencias.

---

## Bounded Context

Transaction Context — Domain Driven Design.

---

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.x |
| Persistencia | PostgreSQL |
| Cache | Redis (idempotencia) |
| Mensajería | Kafka |
| Observabilidad | OpenTelemetry |

---

## Responsabilidades

- Recibir y validar solicitudes de transferencia.
- Verificar idempotencia (Redis + PostgreSQL).
- Publicar `TransactionCreated` para disparar la Saga.
- Escuchar resultados de `risk-service`, `limits-service` y `ledger-service`.
- Actualizar el estado de la transacción según avanza la Saga.
- Publicar `TransactionCompleted` o `TransactionFailed`.

---

## Flujo de la Saga Choreography

```
[Cliente] → POST /transactions/transfer
     ↓
[transaction-service]
  - Valida idempotency key
  - Persiste transacción en estado PENDING
  - Publica TransactionCreated via Outbox

[risk-service] consume TransactionCreated
  → Publica RiskValidated o RiskRejected

[limits-service] consume TransactionCreated
  → Publica LimitValidated o LimitExceeded

[transaction-service] consume RiskValidated + LimitValidated
  - Cuando ambos llegan: publica LedgerEntryRequest
  - Si cualquiera falla: publica TransactionFailed

[ledger-service] consume LedgerEntryRequest
  → Publica LedgerEntryCreated o LedgerEntryFailed

[transaction-service] consume LedgerEntryCreated
  → Actualiza estado a COMPLETED
  → Publica TransactionCompleted

[transaction-service] consume LedgerEntryFailed
  → Actualiza estado a FAILED
  → Publica TransactionFailed
```

---

## Lo que NO hace este servicio

- No registra en el ledger directamente. → `ledger-service`
- No valida riesgo. → `risk-service`
- No valida límites. → `limits-service`
- No envía notificaciones. → `notification-service`

---

## Endpoints REST

### POST /api/v1/transactions/transfer

Solicita una transferencia financiera. Punto de entrada principal.

**Headers:** 
- `Authorization: Bearer {token}`
- `X-Idempotency-Key: uuid` (OBLIGATORIO)
- `X-Correlation-Id: uuid`
- `X-Session-Id: uuid`

**Request:**

```json
{
  "sourceAccountId": "uuid",
  "sourceWalletId": "uuid",
  "destinationAccountId": "uuid",
  "destinationWalletId": "uuid (null si es externa)",
  "amount": 1000.00,
  "currency": "COP",
  "transactionType": "INTERNAL_TRANSFER | EXTERNAL_TRANSFER",
  "description": "string (opcional)",
  "externalBankCode": "string (requerido si EXTERNAL_TRANSFER)"
}
```

**Response 202:** Transacción aceptada y en proceso.

```json
{
  "transactionId": "uuid",
  "idempotencyKey": "uuid",
  "status": "PENDING",
  "estimatedCompletionTime": "datetime",
  "correlationId": "uuid"
}
```

**Response 200 (idempotente):** Retorna el estado actual si ya fue procesada.

**Response 422:** `InvalidTransactionException`

**Response 429:** `RateLimitExceededException`

---

### POST /api/v1/transactions/income

Registra un ingreso en una billetera.

**Headers:** `X-Idempotency-Key: uuid` (OBLIGATORIO)

**Request:**

```json
{
  "walletId": "uuid",
  "accountId": "uuid",
  "amount": 500.00,
  "currency": "COP",
  "category": "SALARY | FREELANCE | INVESTMENT_RETURN | OTHER",
  "description": "string"
}
```

**Response 202:** Ingreso registrado.

---

### POST /api/v1/transactions/expense

Registra un gasto en una billetera.

**Headers:** `X-Idempotency-Key: uuid` (OBLIGATORIO)

**Request:**

```json
{
  "walletId": "uuid",
  "accountId": "uuid",
  "amount": 200.00,
  "currency": "COP",
  "category": "FOOD | TRANSPORT | ENTERTAINMENT | UTILITIES | OTHER",
  "description": "string"
}
```

**Response 202:** Gasto registrado.

---

### GET /api/v1/transactions/{transactionId}

Consulta el estado de una transacción.

**Response 200:**

```json
{
  "transactionId": "uuid",
  "idempotencyKey": "uuid",
  "type": "INTERNAL_TRANSFER",
  "sourceAccountId": "uuid",
  "sourceWalletId": "uuid",
  "destinationAccountId": "uuid",
  "destinationWalletId": "uuid",
  "amount": 1000.00,
  "currency": "COP",
  "status": "COMPLETED | PENDING | FAILED | PROCESSING",
  "failureReason": "string (si aplica)",
  "createdAt": "datetime",
  "completedAt": "datetime (si aplica)"
}
```

**Response 404:** `TransactionNotFoundException`

---

### GET /api/v1/transactions

Lista transacciones con filtros.

**Query params:** `accountId`, `walletId`, `type`, `status`, `dateFrom`, `dateTo`, `page`, `size`

**Response 200:** Lista paginada de transacciones.

---

## DTOs

### TransferRequest

```java
public record TransferRequest(
    @NotNull UUID sourceAccountId,
    @NotNull UUID sourceWalletId,
    @NotNull UUID destinationAccountId,
    UUID destinationWalletId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotNull Currency currency,
    @NotNull TransactionType transactionType,
    @Size(max = 255) String description,
    String externalBankCode
) {}
```

### TransactionResponse

```java
public record TransactionResponse(
    UUID transactionId,
    UUID idempotencyKey,
    TransactionType type,
    UUID sourceAccountId,
    UUID sourceWalletId,
    UUID destinationAccountId,
    UUID destinationWalletId,
    BigDecimal amount,
    Currency currency,
    TransactionStatus status,
    String failureReason,
    Instant createdAt,
    Instant completedAt
) {}
```

### IncomeRequest

```java
public record IncomeRequest(
    @NotNull UUID walletId,
    @NotNull UUID accountId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotNull Currency currency,
    @NotNull IncomeCategory category,
    @Size(max = 255) String description
) {}
```

### ExpenseRequest

```java
public record ExpenseRequest(
    @NotNull UUID walletId,
    @NotNull UUID accountId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotNull Currency currency,
    @NotNull ExpenseCategory category,
    @Size(max = 255) String description
) {}
```

---

## Enumeraciones de Dominio

```java
public enum TransactionType {
    INTERNAL_TRANSFER, EXTERNAL_TRANSFER, INCOME, EXPENSE
}

public enum TransactionStatus {
    PENDING, PROCESSING, RISK_CHECK, LIMIT_CHECK, LEDGER_PENDING, COMPLETED, FAILED, ROLLED_BACK
}

public enum IncomeCategory {
    SALARY, FREELANCE, INVESTMENT_RETURN, RENTAL, OTHER
}

public enum ExpenseCategory {
    FOOD, TRANSPORT, ENTERTAINMENT, UTILITIES, HEALTH, EDUCATION, OTHER
}
```

---

## Modelo de Datos

### Tabla: `transactions`

```sql
CREATE TABLE transactions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id          UUID NOT NULL UNIQUE,
    idempotency_key         UUID NOT NULL UNIQUE,
    correlation_id          UUID NOT NULL,
    session_id              UUID,
    type                    VARCHAR(30) NOT NULL,
    source_account_id       UUID NOT NULL,
    source_wallet_id        UUID,
    destination_account_id  UUID,
    destination_wallet_id   UUID,
    amount                  NUMERIC(19, 4) NOT NULL,
    currency                VARCHAR(3) NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    failure_reason          TEXT,
    ledger_entry_id         UUID,
    description             TEXT,
    category                VARCHAR(50),
    external_bank_code      VARCHAR(50),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at            TIMESTAMPTZ
);

CREATE INDEX idx_transactions_source_account ON transactions(source_account_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_idempotency ON transactions(idempotency_key);
```

### Tabla: `saga_state`

```sql
CREATE TABLE saga_state (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id      UUID NOT NULL UNIQUE,
    risk_validated      BOOLEAN,
    risk_validated_at   TIMESTAMPTZ,
    limit_validated     BOOLEAN,
    limit_validated_at  TIMESTAMPTZ,
    ledger_created      BOOLEAN,
    ledger_created_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Tabla: `outbox_events`

```sql
CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id    UUID NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    published       BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox_events(published) WHERE published = false;
```

### Redis: Idempotency Cache

```
Key:   idempotency:{idempotencyKey}
Value: { transactionId, status, createdAt }
TTL:   86400 segundos (24 horas)
```

---

## Eventos Publicados

| Evento | Topic | Trigger |
|---|---|---|
| TransactionCreated | nanobank.transaction.transfer.created | Transacción aceptada |
| TransactionCompleted | nanobank.transaction.transfer.completed | Saga exitosa |
| TransactionFailed | nanobank.transaction.transfer.failed | Saga fallida |

---

## Eventos Consumidos

| Evento | Topic | Acción |
|---|---|---|
| RiskValidated | nanobank.risk.validation.approved | Marca risk_validated = true en saga_state |
| RiskRejected | nanobank.risk.validation.rejected | Marca transacción como FAILED |
| LimitValidated | nanobank.limits.validation.approved | Marca limit_validated = true en saga_state |
| LimitExceeded | nanobank.limits.validation.exceeded | Marca transacción como FAILED |
| LedgerEntryCreated | nanobank.ledger.entry.created | Marca transacción como COMPLETED |
| LedgerEntryFailed | nanobank.ledger.entry.failed | Marca transacción como FAILED |

---

## Lógica de Avance de Saga

```java
// En el consumer de RiskValidated y LimitValidated:
// Solo se avanza al ledger cuando AMBAS validaciones pasan

if (sagaState.isRiskValidated() && sagaState.isLimitValidated()) {
    publishLedgerEntryRequest(transactionId);
}
```

---

## Diagrama de Secuencia

Ver: `sequence-diagrams/transfer-saga.md`

---

## Configuración

```yaml
server:
  port: 8086

spring:
  application:
    name: transaction-service

transaction:
  idempotency-ttl-seconds: 86400
  max-amount-cop: 50000000
  max-amount-usd: 10000
```

---

## Testing

Casos críticos:

- Crear transacción con idempotency key nueva.
- Crear transacción con idempotency key duplicada (retorna estado previo).
- Avanzar saga cuando risk y limit ambos pasan.
- Fallar saga cuando risk rechaza.
- Fallar saga cuando limit es excedido.
- Fallar saga cuando ledger falla.
- Completar saga cuando ledger confirma.
- Consultar transacción por ID.
- Calcular saldo correctamente en ingresos y gastos.
