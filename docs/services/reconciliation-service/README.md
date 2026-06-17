# reconciliation-service

## Responsabilidad

Microservicio responsable de la conciliación financiera. Detecta inconsistencias entre el ledger y los estados proyectados de los demás servicios. Garantiza la integridad financiera del sistema.

---

## Bounded Context

Reconciliation Context — Domain Driven Design.

---

## Responsabilidades

- Comparar saldos del ledger contra proyecciones en wallet-service y account-service.
- Detectar transacciones incompletas o inconsistentes.
- Procesar eventos en Dead Letter Queue (DLQ).
- Ejecutar jobs de conciliación periódicos.
- Publicar `ReconciliationCompleted` o `ReconciliationFailed`.

---

## Eventos Consumidos

| Evento | Topic | Acción |
|---|---|---|
| LedgerEntryCreated | nanobank.ledger.entry.created | Registra entrada para conciliación |
| TransactionCompleted | nanobank.transaction.transfer.completed | Verifica consistencia |
| TransactionFailed | nanobank.transaction.transfer.failed | Verifica no existan entradas en ledger |
| *.dlq | Todos los DLQ | Analiza y reintenta o escala |

---

## Eventos Publicados

| Evento | Topic | Trigger |
|---|---|---|
| ReconciliationCompleted | nanobank.reconciliation.process.completed | Conciliación sin inconsistencias |
| ReconciliationFailed | nanobank.reconciliation.process.failed | Inconsistencias detectadas |

---

## Endpoints REST

### GET /api/v1/reconciliation/status

Retorna el estado de la última conciliación ejecutada.

**Response 200:**

```json
{
  "reconciliationId": "uuid",
  "status": "COMPLETED | IN_PROGRESS | FAILED",
  "periodStart": "datetime",
  "periodEnd": "datetime",
  "totalTransactions": 1500,
  "matchedTransactions": 1498,
  "unmatchedTransactions": 2,
  "completedAt": "datetime"
}
```

---

### POST /api/v1/reconciliation/trigger

Dispara una conciliación manual. Solo `ROLE_ADMIN`.

**Response 202:** Conciliación iniciada.

---

### GET /api/v1/reconciliation/discrepancies

Lista las discrepancias detectadas en la última conciliación.

---

## Modelo de Datos

### Tabla: `reconciliation_runs`

```sql
CREATE TABLE reconciliation_runs (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reconciliation_id       UUID NOT NULL UNIQUE,
    period_start            TIMESTAMPTZ NOT NULL,
    period_end              TIMESTAMPTZ NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    total_transactions      INTEGER,
    matched_transactions    INTEGER,
    unmatched_transactions  INTEGER,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at            TIMESTAMPTZ
);
```

### Tabla: `reconciliation_discrepancies`

```sql
CREATE TABLE reconciliation_discrepancies (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reconciliation_id   UUID NOT NULL REFERENCES reconciliation_runs(reconciliation_id),
    transaction_id      UUID,
    discrepancy_type    VARCHAR(50) NOT NULL,
    description         TEXT NOT NULL,
    resolved            BOOLEAN NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## Configuración

```yaml
server:
  port: 8092

spring:
  application:
    name: reconciliation-service

reconciliation:
  schedule: "0 0 * * * *"
  dlq-batch-size: 100
```
