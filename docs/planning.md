# NanoBank Ledger — Planning de Ejecución por Agentes

## Propósito

Este documento define el orden de implementación, dependencias, contratos y prompts de referencia para que cada microservicio pueda ser construido de forma autónoma por un agente o desarrollador, manteniendo coherencia arquitectónica con las decisiones tomadas en el handover.

---

## Principios de Ejecución

- Cada microservicio es independiente y dueño exclusivo de sus datos.
- Ningún servicio accede directamente a la base de datos de otro.
- Toda comunicación asíncrona ocurre por Kafka.
- Toda comunicación sincrónica ocurre por REST (solo cuando es estrictamente necesario).
- Todos los servicios implementan Transactional Outbox para publicación de eventos.
- Toda operación financiera es idempotente.
- OpenTelemetry está presente en todos los servicios desde el día cero.

---

## Stack por Microservicio

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.x |
| Persistencia | PostgreSQL (por servicio, base de datos separada) |
| Cache / Idempotencia | Redis |
| Mensajería | Apache Kafka |
| Seguridad | JWT (RS256) |
| Observabilidad | OpenTelemetry + Prometheus + Grafana + OpenSearch |
| Documentación API | OpenAPI 3.1 / Swagger UI |
| Testing | JUnit 5 + Mockito (80% coverage mínimo) |
| Build | Maven 3.9+ |

---

## Orden de Implementación

El orden respeta el grafo de dependencias entre servicios. Un servicio no debe implementarse antes que sus dependencias estén contractualmente definidas (al menos los DTOs y eventos).

```
Fase A — Identidad y Seguridad
  1. identity-service
  2. auth-service

Fase B — Entidades Core
  3. user-service
  4. account-service
  5. wallet-service

Fase C — Motor Financiero
  6. limits-service
  7. risk-service
  8. transaction-service
  9. ledger-service

Fase D — Soporte y Observabilidad
  10. audit-service
  11. notification-service
  12. reconciliation-service
  13. logs-service
```

---

## Dependencias entre Servicios

```
identity-service
  └── sin dependencias externas

auth-service
  └── consume: identity-service (REST — validación JWT)

user-service
  └── consume: identity-service (JWT via Gateway)
  └── publica: UserCreated, UserUpdated, UserDeactivated

account-service
  └── consume: UserCreated (Kafka)
  └── publica: AccountCreated, AccountBlocked, AccountClosed

wallet-service
  └── consume: AccountCreated (Kafka)
  └── publica: WalletCreated, WalletUpdated, WalletDeactivated

limits-service
  └── consume: TransactionCreated (Kafka)
  └── publica: LimitValidated, LimitExceeded

risk-service
  └── consume: TransactionCreated (Kafka)
  └── publica: RiskValidated, RiskRejected

transaction-service
  └── consume: RiskValidated, LimitValidated, LedgerEntryCreated (Kafka)
  └── publica: TransactionCreated, TransactionCompleted, TransactionFailed

ledger-service
  └── consume: TransactionCreated (Kafka)
  └── publica: LedgerEntryCreated, LedgerEntryFailed

audit-service
  └── consume: todos los eventos de negocio (Kafka)
  └── sin publicaciones propias

notification-service
  └── consume: TransactionCompleted, TransactionFailed, LimitExceeded (Kafka)
  └── sin publicaciones propias

reconciliation-service
  └── consume: LedgerEntryCreated (Kafka)
  └── publica: ReconciliationCompleted, ReconciliationFailed

logs-service
  └── consume: logs via OpenTelemetry Collector
  └── persiste en OpenSearch
```

---

## Estructura de Directorios por Microservicio

Cada microservicio debe seguir esta estructura hexagonal:

```
{service-name}/
├── src/
│   ├── main/
│   │   ├── java/com/nanobank/{service}/
│   │   │   ├── application/
│   │   │   │   ├── port/
│   │   │   │   │   ├── in/          # Use cases (interfaces)
│   │   │   │   │   └── out/         # Repository / messaging ports (interfaces)
│   │   │   │   └── service/         # Implementación de use cases
│   │   │   ├── domain/
│   │   │   │   ├── model/           # Entidades y Value Objects
│   │   │   │   └── event/           # Domain Events
│   │   │   └── infrastructure/
│   │   │       ├── adapter/
│   │   │       │   ├── in/
│   │   │       │   │   ├── rest/    # Controllers REST
│   │   │       │   │   └── kafka/   # Kafka Consumers
│   │   │       │   └── out/
│   │   │       │       ├── persistence/  # JPA Repositories
│   │   │       │       ├── kafka/        # Kafka Producers + Outbox
│   │   │       │       └── cache/        # Redis adapters
│   │   │       └── config/          # Spring config
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/        # Flyway migrations
│   └── test/
│       └── java/com/nanobank/{service}/
│           ├── application/
│           └── domain/
├── docs/
│   ├── README.md
│   ├── sequence-diagrams/
│   └── openapi.yaml
└── pom.xml
```

---

## Contrato Mínimo por Microservicio (Checklist de Agente)

Antes de marcar un microservicio como completo, debe cumplir:

- [ ] README.md con descripción, responsabilidades, endpoints y eventos
- [ ] openapi.yaml con todos los endpoints documentados
- [ ] DTOs de request/response definidos
- [ ] Domain Events definidos con schema completo
- [ ] Flyway migrations para el modelo de datos
- [ ] Unit tests con cobertura >= 80% en application/domain
- [ ] Global Exception Handler implementado
- [ ] OpenTelemetry configurado (traces + métricas)
- [ ] Outbox table incluida en el modelo de datos
- [ ] Idempotency check en operaciones financieras

---

## Prompt Template para Agentes

Usar este prompt como base al delegar la implementación de cada microservicio:

```
Actúa como Staff Engineer especializado en Java 21, Spring Boot 3, DDD y arquitectura hexagonal.

Implementa el microservicio {SERVICE_NAME} para la plataforma NanoBank Ledger.

Contexto arquitectónico:
- Arquitectura hexagonal estricta (ports & adapters)
- Domain Driven Design
- Event Driven via Kafka con Transactional Outbox
- Idempotencia obligatoria en operaciones financieras
- OpenTelemetry en todos los componentes
- PostgreSQL como persistencia, Flyway para migraciones

Responsabilidades del servicio:
{RESPONSABILIDADES_DEL_README}

Eventos que publica:
{EVENTOS_DEL_EVENT_CATALOG}

Eventos que consume:
{EVENTOS_CONSUMIDOS}

Endpoints REST:
{ENDPOINTS_DEL_OPENAPI}

DTOs:
{DTOS_DEFINIDOS}

Restricciones:
- Cobertura de tests >= 80%
- Global Exception Handler obligatorio
- Sin acceso directo a bases de datos de otros servicios
- Toda publicación de eventos vía Outbox
- CorrelationId, SessionId y TransactionId propagados en todos los logs
```

---

## Fase A — Identidad y Seguridad

### identity-service

**Objetivo:** Emisión y validación de JWT con RS256. Gestión de roles y permisos.

**Inputs:** Credenciales validadas por auth-service.

**Outputs:** JWT firmado, refresh token, validación de token.

**Dependencias upstream:** ninguna.

**Eventos publicados:** ninguno (comunicación sincrónica).

**Prioridad:** CRÍTICA — todos los demás servicios dependen de JWT.

---

### auth-service

**Objetivo:** Login, logout, MFA, gestión de sesión.

**Inputs:** Credenciales del usuario vía REST.

**Outputs:** JWT (delegado a identity-service), sesión activa.

**Dependencias upstream:** identity-service (REST).

**Eventos publicados:** UserLoggedIn, UserLoggedOut, MFAValidated.

**Prioridad:** CRÍTICA.

---

## Fase B — Entidades Core

### user-service

**Objetivo:** Gestión del cliente. Perfil, estado, información personal.

**Inputs:** REST (creación/actualización), eventos de auth.

**Outputs:** UserCreated, UserUpdated, UserDeactivated.

**Dependencias upstream:** identity-service (JWT validación via Gateway).

**Prioridad:** ALTA.

---

### account-service

**Objetivo:** Gestión de cuentas financieras y productos.

**Inputs:** UserCreated (Kafka), REST.

**Outputs:** AccountCreated, AccountBlocked, AccountClosed.

**Dependencias upstream:** user-service (via evento).

**Prioridad:** ALTA.

---

### wallet-service

**Objetivo:** Gestión de billeteras presupuestales asociadas a cuentas.

**Inputs:** AccountCreated (Kafka), REST.

**Outputs:** WalletCreated, WalletUpdated, WalletDeactivated, WalletTransferCompleted.

**Dependencias upstream:** account-service (via evento).

**Prioridad:** ALTA — es el feature central del MVP.

---

## Fase C — Motor Financiero

### limits-service

**Objetivo:** Validación de límites diarios y mensuales por cuenta.

**Inputs:** TransactionCreated (Kafka).

**Outputs:** LimitValidated, LimitExceeded.

**Dependencias upstream:** transaction-service (via evento).

**Prioridad:** ALTA.

---

### risk-service

**Objetivo:** Validación de reglas de riesgo y restricciones regulatorias.

**Inputs:** TransactionCreated (Kafka).

**Outputs:** RiskValidated, RiskRejected.

**Dependencias upstream:** transaction-service (via evento).

**Prioridad:** ALTA.

---

### transaction-service

**Objetivo:** Creación y orquestación funcional del movimiento financiero.

**Inputs:** REST (solicitud de transferencia), RiskValidated, LimitValidated, LedgerEntryCreated (Kafka).

**Outputs:** TransactionCreated, TransactionCompleted, TransactionFailed.

**Dependencias upstream:** risk-service, limits-service, ledger-service (via eventos).

**Prioridad:** CRÍTICA — es el coordinador de la Saga.

---

### ledger-service

**Objetivo:** Registro financiero inmutable. Fuente única de verdad.

**Inputs:** TransactionCreated (Kafka).

**Outputs:** LedgerEntryCreated, LedgerEntryFailed.

**Dependencias upstream:** transaction-service (via evento).

**Prioridad:** CRÍTICA.

---

## Fase D — Soporte y Observabilidad

### audit-service

**Objetivo:** Evidencia regulatoria. Consume todos los eventos de negocio.

**Inputs:** Todos los eventos de negocio (Kafka).

**Outputs:** sin publicaciones propias.

**Prioridad:** MEDIA.

---

### notification-service

**Objetivo:** Envío de notificaciones push, email y SMS.

**Inputs:** TransactionCompleted, TransactionFailed, LimitExceeded (Kafka).

**Outputs:** sin publicaciones propias.

**Prioridad:** MEDIA.

---

### reconciliation-service

**Objetivo:** Conciliación financiera y detección de inconsistencias.

**Inputs:** LedgerEntryCreated (Kafka), jobs programados.

**Outputs:** ReconciliationCompleted, ReconciliationFailed.

**Prioridad:** MEDIA.

---

### logs-service

**Objetivo:** Centralización de logs via OpenTelemetry Collector hacia OpenSearch.

**Inputs:** OpenTelemetry Collector (no Kafka).

**Outputs:** dashboards en OpenSearch/Grafana.

**Prioridad:** BAJA (infraestructura).

---

## Variables de Entorno Estándar por Servicio

```yaml
# Aplicación
SPRING_APPLICATION_NAME: {service-name}
SERVER_PORT: {puerto asignado}

# Base de datos
SPRING_DATASOURCE_URL: jdbc:postgresql://{host}:{port}/{db_name}
SPRING_DATASOURCE_USERNAME: {user}
SPRING_DATASOURCE_PASSWORD: {password}

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS: {kafka_host}:9092
KAFKA_GROUP_ID: {service-name}-group

# Redis
SPRING_REDIS_HOST: {redis_host}
SPRING_REDIS_PORT: 6379

# JWT
JWT_PUBLIC_KEY: {rsa_public_key_path}

# OpenTelemetry
OTEL_SERVICE_NAME: {service-name}
OTEL_EXPORTER_OTLP_ENDPOINT: http://{otel_collector}:4317
```

---

## Puertos Asignados

| Servicio | Puerto |
|---|---|
| identity-service | 8081 |
| auth-service | 8082 |
| user-service | 8083 |
| account-service | 8084 |
| wallet-service | 8085 |
| transaction-service | 8086 |
| risk-service | 8087 |
| limits-service | 8088 |
| ledger-service | 8089 |
| audit-service | 8090 |
| notification-service | 8091 |
| reconciliation-service | 8092 |
| logs-service | 8093 |
| API Gateway | 8080 |
