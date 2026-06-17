# ledger-service

## Responsabilidad

Microservicio responsable del registro financiero inmutable. Es la **única fuente de verdad financiera** de la plataforma. Registra cada movimiento como una entrada doble (débito + crédito) garantizando trazabilidad y consistencia contable.

---

## Bounded Context

Ledger Context — Domain Driven Design.

---

## Principio Fundamental

> **Ledger First:** Ningún saldo es considerado definitivo fuera de este dominio. Todos los servicios que muestran saldos son proyecciones derivadas del ledger.

El modelo implementa **Double-Entry Bookkeeping** (Partida Doble):
- Cada transacción genera exactamente dos entradas: un DÉBITO y un CRÉDITO.
- La suma de todos los débitos siempre debe igualar la suma de todos los créditos.
- Las entradas son **inmutables**: nunca se eliminan, solo se compensan.

---

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.x |
| Persistencia | PostgreSQL (append-only) |
| Mensajería | Kafka |
| Observabilidad | OpenTelemetry |

---

## Responsabilidades

- Recibir solicitudes de registro de movimientos financieros.
- Validar integridad de la partida doble.
- Registrar entradas inmutables en el ledger.
- Calcular y actualizar saldos corrientes por cuenta y billetera.
- Publicar confirmación de registro.
- Soportar reconstrucción de saldo desde cero via event replay.

---

## Lo que NO hace este servicio

- No decide si una transacción es válida (riesgo/límites). → otros servicios
- No procesa pagos externos. → `transaction-service`
- No notifica al usuario. → `notification-service`
- No modifica ni elimina entradas existentes (inmutabilidad).

---

## Endpoints REST

### GET /api/v1/ledger/balance/{accountId}

Retorna el saldo actual de una cuenta calculado desde el ledger.

**Response 200:**

```json
{
  "accountId": "uuid",
  "balance": 15000.00,
  "currency": "COP",
  "calculatedAt": "datetime",
  "totalDebits": 5000.00,
  "totalCredits": 20000.00
}
```

---

### GET /api/v1/ledger/balance/wallet/{walletId}

Retorna el saldo actual de una billetera calculado desde el ledger.

**Response 200:**

```json
{
  "walletId": "uuid",
  "accountId": "uuid",
  "balance": 3500.00,
  "currency": "COP",
  "calculatedAt": "datetime"
}
```

---

### GET /api/v1/ledger/entries/{accountId}

Lista todas las entradas del ledger para una cuenta con filtros y paginación.

**Query params:** `dateFrom`, `dateTo`, `entryType (DEBIT|CREDIT)`, `page`, `size`

**Response 200:**

```json
{
  "entries": [
    {
      "entryId": "uuid",
      "transactionId": "uuid",
      "accountId": "uuid",
      "walletId": "uuid",
      "entryType": "CREDIT",
      "amount": 1000.00,
      "currency": "COP",
      "balanceAfter": 6000.00,
      "description": "string",
      "createdAt": "datetime"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "total": 150
  }
}
```

---

### GET /api/v1/ledger/entries/transaction/{transactionId}

Retorna las entradas asociadas a una transacción específica.

**Response 200:** Par de entradas (débito + crédito).

---

### POST /api/v1/ledger/reconstruct/{accountId}

Inicia la reconstrucción del saldo de una cuenta desde el primer registro.

**Uso:** Operación administrativa. Requiere rol `ROLE_ADMIN`.

**Response 202:** Reconstrucción iniciada.

```json
{
  "reconstructionId": "uuid",
  "accountId": "uuid",
  "status": "IN_PROGRESS"
}
```

---

## DTOs

### LedgerBalanceResponse

```java
public record LedgerBalanceResponse(
    UUID accountId,
    BigDecimal balance,
    String currency,
    Instant calculatedAt,
    BigDecimal totalDebits,
    BigDecimal totalCredits
) {}
```

### LedgerEntryResponse

```java
public record LedgerEntryResponse(
    UUID entryId,
    UUID transactionId,
    UUID accountId,
    UUID walletId,
    EntryType entryType,
    BigDecimal amount,
    String currency,
    BigDecimal balanceAfter,
    String description,
    Instant createdAt
) {}
```

### LedgerEntryFilter

```java
public record LedgerEntryFilter(
    LocalDate dateFrom,
    LocalDate dateTo,
    EntryType entryType,
    int page,
    int size
) {}
```

---

## Enumeraciones de Dominio

```java
public enum EntryType {
    DEBIT, CREDIT
}

public enum LedgerEntryStatus {
    ACTIVE, COMPENSATED
}
```

---

## Modelo de Datos

### Tabla: `ledger_entries` (append-only, nunca se modifica)

```sql
CREATE TABLE ledger_entries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id            UUID NOT NULL UNIQUE,
    transaction_id      UUID NOT NULL,
    account_id          UUID NOT NULL,
    wallet_id           UUID,
    entry_type          VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount              NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency            VARCHAR(3) NOT NULL,
    balance_after       NUMERIC(19, 4) NOT NULL,
    description         TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    compensated_by      UUID REFERENCES ledger_entries(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Inmutabilidad: prohibir UPDATE y DELETE via trigger
CREATE RULE no_update_ledger AS ON UPDATE TO ledger_entries DO INSTEAD NOTHING;
CREATE RULE no_delete_ledger AS ON DELETE TO ledger_entries DO INSTEAD NOTHING;

CREATE INDEX idx_ledger_account_id ON ledger_entries(account_id);
CREATE INDEX idx_ledger_wallet_id ON ledger_entries(wallet_id);
CREATE INDEX idx_ledger_transaction_id ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_created_at ON ledger_entries(created_at);
```

### Tabla: `account_balances` (proyección del saldo corriente)

```sql
CREATE TABLE account_balances (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL UNIQUE,
    balance         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL,
    last_entry_id   UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Tabla: `wallet_balances` (proyección del saldo corriente por wallet)

```sql
CREATE TABLE wallet_balances (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id       UUID NOT NULL UNIQUE,
    account_id      UUID NOT NULL,
    balance         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL,
    last_entry_id   UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
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

---

## Lógica de Partida Doble

Toda entrada en el ledger genera exactamente dos registros dentro de la misma transacción de base de datos:

```
TRANSFERENCIA: Wallet A → Wallet B (500 COP)

Entrada 1: DEBIT  | account_A | wallet_A | -500 COP | balance_after: 1500
Entrada 2: CREDIT | account_B | wallet_B | +500 COP | balance_after: 2500

Invariante: sum(DEBIT) == sum(CREDIT) en toda transacción
```

---

## Eventos Publicados

| Evento | Topic | Trigger |
|---|---|---|
| LedgerEntryCreated | nanobank.ledger.entry.created | Entradas registradas exitosamente |
| LedgerEntryFailed | nanobank.ledger.entry.failed | Error al registrar (saldo insuficiente, cuenta bloqueada) |

---

## Eventos Consumidos

| Evento | Topic | Acción |
|---|---|---|
| TransactionCreated | nanobank.transaction.transfer.created | Registra las entradas dobles del ledger |
| WalletTransferCompleted | nanobank.wallet.transfer.completed | Registra las entradas para transferencias entre wallets |
| AccountCreated | nanobank.account.account.created | Inicializa balance en account_balances |

---

## Diagrama de Secuencia

Ver: `sequence-diagrams/ledger-entry.md`

---

## Configuración

```yaml
server:
  port: 8089

spring:
  application:
    name: ledger-service

ledger:
  enable-immutability-rules: true
  balance-cache-ttl-seconds: 300
```

---

## Testing

Casos críticos:

- Registrar entrada de débito y crédito en la misma transacción.
- Validar que el saldo nunca queda negativo.
- Rechazar registro si cuenta está bloqueada.
- Rechazar registro duplicado (mismo transactionId).
- Calcular saldo correcto desde múltiples entradas.
- Reconstruir saldo desde cero correctamente.
- Verificar inmutabilidad (intentar actualizar/eliminar retorna error).
- Publicar LedgerEntryCreated solo tras commit exitoso.
