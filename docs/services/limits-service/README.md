# limits-service

## Responsabilidad

Microservicio responsable de la validación de límites operativos por cuenta. Controla límites diarios, mensuales y por transacción. Participa en la Saga Choreography como validador asíncrono.

---

## Bounded Context

Limits Context — Domain Driven Design.

---

## Responsabilidades

- Mantener contadores de uso diario y mensual por cuenta.
- Validar que una transacción no exceda los límites configurados.
- Publicar `LimitValidated` o `LimitExceeded`.
- Resetear contadores automáticamente (diario a las 00:00, mensual el día 1).

---

## Eventos Consumidos

| Evento | Topic | Acción |
|---|---|---|
| TransactionCreated | nanobank.transaction.transfer.created | Valida límites para la transacción |
| AccountCreated | nanobank.account.account.created | Inicializa límites con valores por defecto |
| TransactionCompleted | nanobank.transaction.transfer.completed | Incrementa contadores de uso |

---

## Eventos Publicados

| Evento | Topic | Trigger |
|---|---|---|
| LimitValidated | nanobank.limits.validation.approved | Transacción dentro de límites |
| LimitExceeded | nanobank.limits.validation.exceeded | Transacción supera límite |

---

## Límites por Defecto

| Tipo | Monto COP | Monto USD |
|---|---|---|
| Por transacción | 10.000.000 | 2.500 |
| Diario | 30.000.000 | 7.500 |
| Mensual | 200.000.000 | 50.000 |

---

## Endpoints REST

### GET /api/v1/limits/{accountId}

Retorna los límites actuales y el uso del periodo para una cuenta.

**Response 200:**

```json
{
  "accountId": "uuid",
  "dailyLimit": 30000000.00,
  "dailyUsed": 5000000.00,
  "dailyRemaining": 25000000.00,
  "monthlyLimit": 200000000.00,
  "monthlyUsed": 15000000.00,
  "monthlyRemaining": 185000000.00,
  "currency": "COP",
  "resetAt": "datetime"
}
```

---

### PUT /api/v1/limits/{accountId}

Actualiza los límites de una cuenta. Solo `ROLE_ADMIN`.

**Request:**

```json
{
  "dailyLimit": 50000000.00,
  "monthlyLimit": 300000000.00,
  "perTransactionLimit": 15000000.00
}
```

---

## DTOs

### LimitStatusResponse

```java
public record LimitStatusResponse(
    UUID accountId,
    BigDecimal dailyLimit,
    BigDecimal dailyUsed,
    BigDecimal dailyRemaining,
    BigDecimal monthlyLimit,
    BigDecimal monthlyUsed,
    BigDecimal monthlyRemaining,
    String currency,
    Instant resetAt
) {}
```

### UpdateLimitsRequest

```java
public record UpdateLimitsRequest(
    @DecimalMin("0.01") BigDecimal dailyLimit,
    @DecimalMin("0.01") BigDecimal monthlyLimit,
    @DecimalMin("0.01") BigDecimal perTransactionLimit
) {}
```

---

## Modelo de Datos

### Tabla: `account_limits`

```sql
CREATE TABLE account_limits (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id              UUID NOT NULL UNIQUE,
    per_transaction_limit   NUMERIC(19, 4) NOT NULL,
    daily_limit             NUMERIC(19, 4) NOT NULL,
    monthly_limit           NUMERIC(19, 4) NOT NULL,
    currency                VARCHAR(3) NOT NULL DEFAULT 'COP',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Tabla: `limit_usage`

```sql
CREATE TABLE limit_usage (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id          UUID NOT NULL,
    period_type         VARCHAR(10) NOT NULL CHECK (period_type IN ('DAILY', 'MONTHLY')),
    period_start        DATE NOT NULL,
    amount_used         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    transaction_count   INTEGER NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (account_id, period_type, period_start)
);
```

---

## Configuración

```yaml
server:
  port: 8088

spring:
  application:
    name: limits-service

limits:
  default-daily-cop: 30000000
  default-monthly-cop: 200000000
  default-per-transaction-cop: 10000000
```

---

## Testing

- Validar transacción dentro de límites diarios.
- Rechazar transacción que excede límite diario.
- Rechazar transacción que excede límite mensual.
- Rechazar transacción que excede límite por transacción.
- Incrementar contadores correctamente tras TransactionCompleted.
- Reset de contadores diarios a las 00:00.
