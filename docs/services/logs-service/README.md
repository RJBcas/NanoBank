# logs-service

## Responsabilidad

Componente de infraestructura responsable de la centralización de logs de todos los microservicios hacia OpenSearch. Opera a través del OpenTelemetry Collector y no se comunica vía Kafka.

---

## Bounded Context

Infraestructura transversal — no es un bounded context de dominio.

---

## Responsabilidades

- Recolección de logs via OpenTelemetry Collector (OTLP).
- Indexación en OpenSearch.
- Retención configurable de logs.
- Correlación de logs por `correlationId`, `sessionId` y `transactionId`.

---

## Arquitectura

```
Microservicio
  └── OpenTelemetry SDK (logs, traces, metrics)
        └── OTLP Exporter
              └── OpenTelemetry Collector
                    ├── OpenSearch (logs)
                    ├── Prometheus (metrics)
                    └── Jaeger / Tempo (traces)
```

---

## Identificadores Obligatorios en Todos los Logs

Todos los microservicios deben propagar en cada log:

```json
{
  "correlationId": "uuid",
  "sessionId": "uuid",
  "transactionId": "uuid (si aplica)",
  "idempotencyKey": "uuid (si aplica)",
  "service": "nombre-del-servicio",
  "traceId": "string (OpenTelemetry)",
  "spanId": "string (OpenTelemetry)"
}
```

---

## Stack de Observabilidad

| Componente | Propósito |
|---|---|
| OpenTelemetry Collector | Recolección centralizada |
| OpenSearch | Almacenamiento y búsqueda de logs |
| OpenSearch Dashboards | Visualización de logs |
| Prometheus | Métricas de microservicios |
| Grafana | Dashboards de métricas y alertas |
| Jaeger / Tempo | Distributed tracing |

---

## Configuración OpenTelemetry por Servicio

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    metrics:
      export:
        url: ${OTEL_METRICS_ENDPOINT}
    tracing:
      endpoint: ${OTEL_TRACES_ENDPOINT}

logging:
  pattern:
    console: >
      %d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36}
      [correlationId=%X{correlationId}]
      [sessionId=%X{sessionId}]
      [traceId=%X{traceId}]
      - %msg%n
```

---

## Puerto

El logs-service no expone puerto REST propio. Opera exclusivamente como pipeline de observabilidad.

OpenTelemetry Collector expone:
- `4317`: OTLP gRPC
- `4318`: OTLP HTTP
- `8888`: Métricas propias del collector
