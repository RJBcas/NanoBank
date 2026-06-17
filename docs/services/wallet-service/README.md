# wallet-service

## Responsabilidad

Microservicio responsable de la gestión de billeteras financieras presupuestales. Las billeteras son agrupaciones de presupuesto asociadas a una cuenta bancaria. **No son cuentas bancarias independientes.**

Este servicio es el **feature central del MVP**.

---

## Bounded Context

Wallet Context — Domain Driven Design.

---

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.x |
| Persistencia | PostgreSQL |
| Cache | Redis (saldos en caliente) |
| Mensajería | Kafka (produce y consume) |
| Observabilidad | OpenTelemetry |

---

## Responsabilidades

- Creación y gestión de billeteras por cuenta.
- Control de saldo por billetera (derivado del Ledger).
- Transferencias entre billeteras del mismo usuario (Drag & Drop MVP).
- Categorización de billeteras (Ahorros, Gastos, Inversiones, Custom).
- Publicación de eventos de ciclo de vida de billetera.

---

## Regla de Negocio Crítica

> El saldo de una billetera **no es almacenado como estado mutable**. El saldo se reconstruye a partir de los registros del `ledger-service`. El campo `balance` en la base de datos del wallet-service es una **proyección en caché** del ledger, actualizada vía eventos.

Esta decisión garantiza el principio **Ledger First**.

---

## Lo que NO hace este servicio

- No procesa pagos externos. → `transaction-service`
- No valida límites. → `limits-service`
- No registra en el ledger directamente. → `ledger-service`
- No gestiona cuentas bancarias. → `account-service`

---

## Endpoints REST

### POST /api/v1/wallets

Crea una nueva billetera asociada a una cuenta.

**Headers:** `Authorization: Bearer {token}`, `X-Idempotency-Key: uuid`, `X-Correlation-Id: uuid`

**Request:**

```json
{
  "accountId": "uuid",
  "name": "string",
  "category": "SAVINGS | EXPENSES | INVESTMENTS | CUSTOM",
  "initialBalance": 0.00,
  "currency": "COP | USD",
  "description": "string (opcional)"
}
```

**Response 201:**

```json
{
  "walletId": "uuid",
  "accountId": "uuid",
  "name": "string",
  "category": "SAVINGS",
  "balance": 0.00,
  "currency": "COP",
  "status": "ACTIVE",
  "createdAt": "datetime"
}
```

**Response 409:** `WalletAlreadyExistsException` (idempotency check)

**Response 404:** `AccountNotFoundException`

---

### GET /api/v1/wallets

Lista todas las billeteras del usuario autenticado.

**Query params:** `accountId (opcional)`, `category (opcional)`, `status (opcional)`

**Response 200:**

```json
{
  "wallets": [
    {
      "walletId": "uuid",
      "accountId": "uuid",
      "name": "string",
      "category": "SAVINGS",
      "balance": 1500.00,
      "currency": "COP",
      "status": "ACTIVE",
      "createdAt": "datetime",
      "updatedAt": "datetime"
    }
  ],
  "total": 3
}
```

---

### GET /api/v1/wallets/{walletId}

Retorna el detalle de una billetera específica.

**Response 200:** Detalle completo de la billetera.

**Response 404:** `WalletNotFoundException`

**Response 403:** `WalletAccessDeniedException`

---

### PUT /api/v1/wallets/{walletId}

Actualiza los datos de una billetera (nombre, descripción, categoría).

**Request:**

```json
{
  "name": "string (opcional)",
  "category": "string (opcional)",
  "description": "string (opcional)"
}
```

**Response 200:** Billetera actualizada.

---

### DELETE /api/v1/wallets/{walletId}

Desactiva una billetera. Solo posible si el saldo es cero.

**Response 204:** Sin contenido.

**Response 409:** `WalletNotEmptyException`

---

### POST /api/v1/wallets/transfer

Transfiere fondos entre dos billeteras del mismo usuario. Feature de **Drag & Drop** del MVP.

**Headers:** `X-Idempotency-Key: uuid`, `X-Correlation-Id: uuid`, `X-Session-Id: uuid`

**Request:**

```json
{
  "sourceWalletId": "uuid",
  "destinationWalletId": "uuid",
  "amount": 500.00,
  "currency": "COP",
  "description": "string (opcional)"
}
```

**Response 202:** Transferencia en proceso.

```json
{
  "transferId": "uuid",
  "status": "PROCESSING",
  "message": "Transfer initiated successfully"
}
```

**Response 409:** `InsufficientBalanceException`

**Response 409:** `DuplicateTransferException` (idempotency)

**Response 422:** `SameWalletTransferException`

---

### GET /api/v1/wallets/{walletId}/transactions

Lista los movimientos de una billetera con filtros.

**Query params:** `category`, `dateFrom`, `dateTo`, `type (INCOME|EXPENSE)`, `page`, `size`

**Response 200:**

```json
{
  "transactions": [
    {
      "transactionId": "uuid",
      "type": "INCOME",
      "amount": 500.00,
      "currency": "COP",
      "description": "string",
      "category": "string",
      "occurredAt": "datetime"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "total": 45
  }
}
```

---

## DTOs

### CreateWalletRequest

```java
public record CreateWalletRequest(
    @NotNull UUID accountId,
    @NotBlank @Size(max = 100) String name,
    @NotNull WalletCategory category,
    @DecimalMin("0.00") BigDecimal initialBalance,
    @NotNull Currency currency,
    @Size(max = 255) String description
) {}
```

### WalletResponse

```java
public record WalletResponse(
    UUID walletId,
    UUID accountId,
    String name,
    WalletCategory category,
    BigDecimal balance,
    Currency currency,
    WalletStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
```

### WalletTransferRequest

```java
public record WalletTransferRequest(
    @NotNull UUID sourceWalletId,
    @NotNull UUID destinationWalletId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotNull Currency currency,
    @Size(max = 255) String description
) {}
```

### WalletTransferResponse

```java
public record WalletTransferResponse(
    UUID transferId,
    String status,
    String message
) {}
```

### WalletTransactionFilter

```java
public record WalletTransactionFilter(
    String category,
    LocalDate dateFrom,
    LocalDate dateTo,
    TransactionType type,
    int page,
    int size
) {}
```

---

## Enumeraciones de Dominio

```java
public enum WalletCategory {
    SAVINGS, EXPENSES, INVESTMENTS, CUSTOM
}

public enum WalletStatus {
    ACTIVE, INACTIVE, BLOCKED
}

public enum Currency {
    COP, USD
}
```

---

## Modelo de Datos

### Tabla: `wallets`

```sql
CREATE TABLE wallets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL,
    user_id         UUID NOT NULL,
    name            VARCHAR(100) NOT NULL,
    category        VARCHAR(20) NOT NULL,
    balance         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL DEFAULT 'COP',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT wallet_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_wallets_account_id ON wallets(account_id);
CREATE INDEX idx_wallets_user_id ON wallets(user_id);
CREATE INDEX idx_wallets_status ON wallets(status);
```

### Tabla: `wallet_transfers`

```sql
CREATE TABLE wallet_transfers (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_id             UUID NOT NULL UNIQUE,
    idempotency_key         UUID NOT NULL UNIQUE,
    source_wallet_id        UUID NOT NULL REFERENCES wallets(id),
    destination_wallet_id   UUID NOT NULL REFERENCES wallets(id),
    amount                  NUMERIC(19, 4) NOT NULL,
    currency                VARCHAR(3) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    description             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at            TIMESTAMPTZ
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

## Eventos Publicados

| Evento | Topic | Trigger |
|---|---|---|
| WalletCreated | nanobank.wallet.wallet.created | Wallet creada exitosamente |
| WalletUpdated | nanobank.wallet.wallet.updated | Wallet actualizada |
| WalletDeactivated | nanobank.wallet.wallet.deactivated | Wallet desactivada |
| WalletTransferCompleted | nanobank.wallet.transfer.completed | Transferencia entre wallets completada |

---

## Eventos Consumidos

| Evento | Topic | Acción |
|---|---|---|
| AccountCreated | nanobank.account.account.created | Permite crear wallets para esa cuenta |
| AccountBlocked | nanobank.account.account.blocked | Bloquea operaciones en wallets asociadas |
| LedgerEntryCreated | nanobank.ledger.entry.created | Actualiza proyección de saldo en caché |

---

## Saga: Transferencia entre Wallets

```
1. Cliente solicita transferencia (POST /wallets/transfer)
2. wallet-service valida idempotency key (Redis)
3. wallet-service valida saldo disponible (proyección)
4. wallet-service persiste WalletTransfer en estado PROCESSING
5. wallet-service publica WalletTransferCompleted via Outbox → Kafka
6. ledger-service consume WalletTransferCompleted
7. ledger-service registra entradas dobles (débito + crédito)
8. ledger-service publica LedgerEntryCreated
9. wallet-service consume LedgerEntryCreated
10. wallet-service actualiza proyección de saldo
11. wallet-service marca transferencia como COMPLETED
```

---

## Diagrama de Secuencia

Ver: `sequence-diagrams/wallet-transfer.md`

Ver: `sequence-diagrams/wallet-crud.md`

---

## Configuración

```yaml
server:
  port: 8085

spring:
  application:
    name: wallet-service

wallet:
  max-wallets-per-account: 10
  min-transfer-amount: 0.01
```

---

## Testing

Casos críticos:

- Crear wallet con saldo inicial cero.
- Crear wallet con idempotency key duplicada.
- Transferir entre wallets con saldo suficiente.
- Transferir con saldo insuficiente.
- Transferir a la misma wallet (error 422).
- Desactivar wallet con saldo > 0 (error 409).
- Filtrar transacciones por fecha y categoría.
- Actualización de saldo vía evento LedgerEntryCreated.
