# audit-service

## Responsabilidad

Microservicio responsable de la trazabilidad completa de todos los eventos de negocio de la plataforma. Genera evidencia regulatoria inmutable y facilita auditorías de cumplimiento.

---

## Bounded Context

Audit Context — Domain Driven Design.

---

## Responsabilidades

- Consumir todos los eventos de negocio de todos los dominios.
- Persistir un registro inmutable por cada evento recibido.
- Exponer APIs de consulta para equipos de compliance y auditoría.
- Correlacionar eventos por `correlationId` y `transactionId`.

---

## Lo que NO hace este servicio

- No publica eventos propios.
- No modifica ni elimina registros de auditoría.
- No toma decisiones de negocio.

---

## Eventos Consumidos

Este servicio suscribe a **todos** los topics de la plataforma:

```
nanobank.identity.*
nanobank.auth.*
nanobank.customer.*
nanobank.account.*
nanobank.wallet.*
nanobank.transaction.*
nanobank.risk.*
nanobank.limits.*
nanobank.ledger.*
nanobank.reconciliation.*
```

---

## Endpoints REST

### GET /api/v1/audit/events

Lista eventos de auditoría con filtros.

**Query params:** `entityType`, `entityId`, `eventType`, `userId`, `dateFrom`, `dateTo`, `correlationId`, `page`, `size`

**Response 200:**

```json
{
  "events": [
    {
      "auditId": "uuid",
      "eventId": "uuid",
      "eventType": "TransactionCreated",
      "sourceService": "transaction-service",
      "entityType": "TRANSACTION",
      "entityId": "uuid",
      "userId": "uuid",
      "correlationId": "uuid",
      "sessionId": "uuid",
      "transactionId": "uuid",
      "payload": {},
      "occurredAt": "datetime",
      "recordedAt": "datetime"
    }
  ],
  "pagination": { "page": 0, "size": 20, "total": 500 }
}
```

---

### GET /api/v1/audit/events/{correlationId}/trace

Retorna el trace completo de una operación por correlationId.

**Response 200:** Lista ordenada de todos los eventos de la operación.

---

### GET /api/v1/audit/events/user/{userId}

Historial de auditoría de un usuario específico.

---

## DTOs

### AuditEventResponse

```java
public record AuditEventResponse(
    UUID auditId,
    UUID eventId,
    String eventType,
    String sourceService,
    String entityType,
    UUID entityId,
    UUID userId,
    UUID correlationId,
    UUID sessionId,
    UUID transactionId,
    Map<String, Object> payload,
    Instant occurredAt,
    Instant recordedAt
) {}
```

---

## Modelo de Datos

### Tabla: `audit_events` (append-only)

```sql
CREATE TABLE audit_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID NOT NULL UNIQUE,
    event_type      VARCHAR(100) NOT NULL,
    source_service  VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       UUID,
    user_id         UUID,
    correlation_id  UUID,
    session_id      UUID,
    transaction_id  UUID,
    payload         JSONB NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_correlation_id ON audit_events(correlation_id);
CREATE INDEX idx_audit_transaction_id ON audit_events(transaction_id);
CREATE INDEX idx_audit_user_id ON audit_events(user_id);
CREATE INDEX idx_audit_event_type ON audit_events(event_type);
CREATE INDEX idx_audit_occurred_at ON audit_events(occurred_at);
```

---

## Configuración

```yaml
server:
  port: 8090

spring:
  application:
    name: audit-service

audit:
  retention-years: 7
```

---

## Testing

- Persistir evento recibido de Kafka.
- Rechazar duplicados por eventId.
- Consultar trace completo por correlationId.
- Filtrar por usuario y rango de fechas.
