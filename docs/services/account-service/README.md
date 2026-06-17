# account-service

## Responsabilidad

Microservicio responsable de la gestión de cuentas financieras asociadas a clientes. Administra tipos de cuenta, estado y productos financieros.

---

## Bounded Context

Account Context — Domain Driven Design.

---

## Responsabilidades

- Creación y gestión de cuentas financieras.
- Asignación de número de cuenta único.
- Gestión de estado de cuenta (ACTIVE, BLOCKED, CLOSED).
- Publicación de eventos de ciclo de vida de cuenta.
- Consumo de eventos de cliente para sincronización.

---

## Lo que NO hace este servicio

- No gestiona saldos. → `ledger-service`
- No gestiona billeteras. → `wallet-service`
- No procesa transacciones. → `transaction-service`

---

## Endpoints REST

### POST /api/v1/accounts

Crea una nueva cuenta para un cliente.

**Request:**

```json
{
  "userId": "uuid",
  "accountType": "SAVINGS | CHECKING | INVESTMENT",
  "currency": "COP | USD"
}
```

**Response 201:**

```json
{
  "accountId": "uuid",
  "userId": "uuid",
  "accountNumber": "string (generado)",
  "accountType": "SAVINGS",
  "currency": "COP",
  "status": "ACTIVE",
  "createdAt": "datetime"
}
```

**Response 404:** `UserNotFoundException`

**Response 409:** `AccountTypeAlreadyExistsException`

---

### GET /api/v1/accounts/{accountId}

Retorna detalle de una cuenta.

**Response 200:** Detalle completo de la cuenta.

**Response 404:** `AccountNotFoundException`

---

### GET /api/v1/accounts/user/{userId}

Lista todas las cuentas de un cliente.

**Response 200:**

```json
{
  "accounts": [
    {
      "accountId": "uuid",
      "accountNumber": "string",
      "accountType": "SAVINGS",
      "currency": "COP",
      "status": "ACTIVE",
      "createdAt": "datetime"
    }
  ]
}
```

---

### PUT /api/v1/accounts/{accountId}/status

Cambia el estado de una cuenta. Solo `ROLE_ADMIN`.

**Request:**

```json
{
  "status": "BLOCKED | CLOSED",
  "reason": "string"
}
```

**Response 200:** Estado actualizado.

---

## DTOs

### CreateAccountRequest

```java
public record CreateAccountRequest(
    @NotNull UUID userId,
    @NotNull AccountType accountType,
    @NotNull Currency currency
) {}
```

### AccountResponse

```java
public record AccountResponse(
    UUID accountId,
    UUID userId,
    String accountNumber,
    AccountType accountType,
    String currency,
    AccountStatus status,
    Instant createdAt
) {}
```

### UpdateAccountStatusRequest

```java
public record UpdateAccountStatusRequest(
    @NotNull AccountStatus status,
    @NotBlank String reason
) {}
```

---

## Enumeraciones de Dominio

```java
public enum AccountType {
    SAVINGS, CHECKING, INVESTMENT
}

public enum AccountStatus {
    ACTIVE, BLOCKED, CLOSED
}
```

---

## Modelo de Datos

### Tabla: `accounts`

```sql
CREATE TABLE accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL UNIQUE,
    user_id         UUID NOT NULL,
    account_number  VARCHAR(20) NOT NULL UNIQUE,
    account_type    VARCHAR(20) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'COP',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_status ON accounts(status);
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
```

---

## Eventos Publicados

| Evento | Topic | Trigger |
|---|---|---|
| AccountCreated | nanobank.account.account.created | Cuenta creada |
| AccountBlocked | nanobank.account.account.blocked | Cuenta bloqueada |
| AccountClosed | nanobank.account.account.closed | Cuenta cerrada |

---

## Eventos Consumidos

| Evento | Topic | Acción |
|---|---|---|
| UserDeactivated | nanobank.customer.profile.deactivated | Bloquea todas las cuentas del usuario |

---

## Testing

- Crear cuenta con tipo SAVINGS para usuario activo.
- Rechazar creación si usuario no existe.
- Rechazar duplicado de tipo de cuenta por usuario.
- Bloquear cuenta y publicar evento.
- Reaccionar a UserDeactivated bloqueando cuentas asociadas.
