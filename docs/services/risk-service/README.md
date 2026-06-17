# risk-service

## Responsabilidad

Microservicio responsable de la validación de reglas de riesgo y restricciones regulatorias sobre transacciones financieras. Participa en la Saga Choreography como validador asíncrono.

---

## Bounded Context

Risk Context — Domain Driven Design.

---

## Responsabilidades

- Evaluar transacciones contra reglas de riesgo configurables.
- Calcular un score de riesgo por transacción.
- Publicar `RiskValidated` o `RiskRejected`.
- Base para futura integración AML/Fraude.

---

## Eventos Consumidos

| Evento | Topic | Acción |
|---|---|---|
| TransactionCreated | nanobank.transaction.transfer.created | Evalúa la transacción contra reglas de riesgo |

---

## Eventos Publicados

| Evento | Topic | Trigger |
|---|---|---|
| RiskValidated | nanobank.risk.validation.approved | Transacción pasa validación |
| RiskRejected | nanobank.risk.validation.rejected | Transacción viola reglas |

---

## Reglas de Riesgo Iniciales

| Regla | Descripción | Acción |
|---|---|---|
| AMOUNT_THRESHOLD | Monto > 10.000.000 COP sin verificación adicional | REJECT |
| UNUSUAL_HOUR | Transacción entre 1am-4am por primera vez | FLAG |
| VELOCITY_CHECK | Más de 10 transacciones en 1 hora | REJECT |
| NEW_DESTINATION | Primera transacción a destinatario nuevo > 5.000.000 COP | REJECT |

---

## Endpoints REST

### GET /api/v1/risk/rules

Lista todas las reglas de riesgo activas.

**Response 200:** Lista de reglas configuradas.

### PUT /api/v1/risk/rules/{ruleId}

Actualiza una regla de riesgo. Solo `ROLE_ADMIN`.

---

## DTOs

### RiskEvaluationResult

```java
public record RiskEvaluationResult(
    UUID transactionId,
    BigDecimal riskScore,
    boolean approved,
    String reason,
    String ruleViolated
) {}
```

---

## Modelo de Datos

### Tabla: `risk_rules`

```sql
CREATE TABLE risk_rules (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_code   VARCHAR(50) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT true,
    config      JSONB NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Tabla: `risk_evaluations`

```sql
CREATE TABLE risk_evaluations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL UNIQUE,
    risk_score      NUMERIC(5, 2) NOT NULL,
    approved        BOOLEAN NOT NULL,
    rule_violated   VARCHAR(50),
    reason          TEXT,
    evaluated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Tabla: `outbox_events`

```sql
CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    published       BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## Configuración

```yaml
server:
  port: 8087

spring:
  application:
    name: risk-service

risk:
  max-amount-cop: 10000000
  velocity-window-minutes: 60
  velocity-max-transactions: 10
```

---

## Testing

- Aprobar transacción dentro de límites normales.
- Rechazar transacción por monto excesivo.
- Rechazar transacción por velocidad (más de 10 en 1 hora).
- Publicar RiskValidated tras aprobación.
- Publicar RiskRejected tras rechazo.
