Actúa como un Principal Software Architect, Enterprise Architect y Staff Engineer especializado en:

- Arquitectura Bancaria
- Domain Driven Design (DDD)
- Event Driven Architecture (EDA)
- Java 21
- Spring Boot 3
- Angular 17
- Kafka
- Saga Pattern
- Transactional Outbox
- Ledger Systems
- Microservices
- OpenTelemetry
- Observabilidad
- Sistemas Financieros de Alta Escala

Voy a continuar el diseño de una solución llamada NanoBank Ledger.

IMPORTANTE:

No quiero una solución CRUD para aprobar una prueba técnica.

Quiero diseñar una plataforma financiera escalable, desacoplada y preparada para millones de usuarios, aplicando buenas prácticas de arquitectura empresarial y bancaria.

Debes asumir que ya se tomaron las siguientes decisiones arquitectónicas.

# CONTEXTO DEL PROYECTO

NanoBank Ledger es una plataforma financiera que permite:

- Gestión de clientes.
- Gestión de cuentas financieras.
- Creación de billeteras financieras.
- Registro de ingresos y gastos.
- Transferencias internas.
- Transferencias externas.
- Conciliación financiera.
- Auditoría.
- Observabilidad completa.
- Escalabilidad horizontal.

La arquitectura debe ser cloud ready y soportar crecimiento masivo.

---

# PRINCIPIOS ARQUITECTÓNICOS

1. Ledger First.

El Ledger es la única fuente de verdad financiera.

2. Event Driven Architecture.

Toda acción relevante genera eventos.

3. Data Ownership.

Cada microservicio es dueño exclusivo de sus datos.

4. Idempotency First.

Toda operación financiera debe ser idempotente.

5. Auditability.

Todo evento de negocio debe ser auditable.

6. Failure Recovery.

El sistema debe recuperarse de fallos parciales.

7. Security by Design.

8. Low Coupling.

Los servicios deben comunicarse principalmente mediante eventos.

---

# ESTILO ARQUITECTÓNICO

- Domain Driven Design (DDD)
- Event Driven Architecture (EDA)
- Hexagonal Architecture
- Microservices
- Saga Choreography
- Transactional Outbox
- CQRS Ready
- Observability First

---

# BOUNDED CONTEXTS DEFINIDOS

## Identity Context

Responsable de:

- JWT
- Refresh Token
- Roles
- Permisos
- Validación de identidad

---

## Authentication Context

Responsable de:

- Login
- Logout
- MFA
- Gestión de sesión

---

## Customer Context

Responsable de:

- Información del cliente
- Estado del cliente

---

## Account Context

Responsable de:

- Información de cuentas
- Estado de cuentas
- Productos financieros

---

## Wallet Context

Responsable de:

- Billeteras financieras
- Presupuestos
- Organización financiera

IMPORTANTE:

La Wallet NO es una cuenta bancaria.

La Wallet es una agrupación presupuestal asociada a una cuenta.

---

## Transaction Context

Responsable de:

- Creación de transacciones
- Orquestación funcional del movimiento financiero
- Estados transaccionales

---

## Risk Context

Responsable de:

- Reglas de riesgo
- Restricciones regulatorias
- Validaciones AML/Fraude futuras

---

## Limits Context

Responsable de:

- Límites diarios
- Límites mensuales
- Restricciones operativas

---

## Ledger Context

Responsable de:

- Registro financiero inmutable
- Reconstrucción de saldos
- Fuente única de verdad

---

## Audit Context

Responsable de:

- Evidencia regulatoria
- Trazabilidad

---

## Notification Context

Responsable de:

- Push
- Email
- SMS

---

## Reconciliation Context

Responsable de:

- Conciliación
- Recuperación financiera
- Detección de inconsistencias

---

# MICROSERVICIOS DEFINIDOS

- identity-service
- auth-service
- user-service
- account-service
- wallet-service
- transaction-service
- risk-service
- limits-service
- ledger-service
- reconciliation-service
- audit-service
- notification-service
- logs-service

---

# EVENT BROKER

Kafka

Motivos:

- Replay
- Persistencia
- Escalabilidad
- Event Streaming
- Auditoría

---

# PATRONES DEFINIDOS

## Saga Choreography

No utilizar Saga Orchestration.

Toda coordinación debe ocurrir mediante eventos.

---

## Transactional Outbox

Todos los eventos deben persistirse antes de publicarse.

---

## Idempotency Pattern

Toda operación financiera debe incluir:

- Idempotency-Key
- CorrelationId
- SessionId
- TransactionId

Redis será utilizado como primera capa de validación.

PostgreSQL almacenará el estado persistente.

---

## Retry Pattern

Backoff exponencial:

1s
2s
4s
8s

Aplicable únicamente a integraciones externas.

---

## Circuit Breaker

Aplicable principalmente a:

- External Bank Integrations
- Servicios externos

---

## Dead Letter Queue

Todos los eventos fallidos deben terminar en una DLQ.

---

# ESTRATEGIA DE PERSISTENCIA

Customer -> PostgreSQL

Account -> PostgreSQL

Wallet -> PostgreSQL

Transaction -> PostgreSQL

Ledger -> PostgreSQL

Audit -> PostgreSQL

Notification -> PostgreSQL

Idempotency -> Redis + PostgreSQL

Logs -> OpenSearch

---

# OBSERVABILIDAD

Obligatorio:

- OpenTelemetry
- Prometheus
- Grafana
- OpenSearch
- Distributed Tracing

Identificadores obligatorios:

- CorrelationId
- SessionId
- TransactionId
- IdempotencyKey

---

# ADRs YA DEFINIDOS

ADR-001 Kafka como Event Broker.

ADR-002 Saga Choreography.

ADR-003 Ledger como única fuente de verdad.

ADR-004 Transactional Outbox.

ADR-005 Redis para Idempotencia.

---

# ESTADO ACTUAL

Ya fue construido el README principal de arquitectura.

El siguiente paso NO es escribir código.

El siguiente paso es diseñar:

1. Event Storming completo.
2. Flujos de dominio.
3. Comandos.
4. Eventos.
5. Topics Kafka.
6. Contratos de eventos.
7. Diagramas de secuencia.
8. README individuales por microservicio.
9. Modelo de datos por contexto.
10. APIs REST.
11. Estrategia de testing.
12. Estructura de repositorios.

Tu objetivo es actuar como arquitecto principal y continuar el diseño manteniendo coherencia con todas las decisiones anteriores, cuestionando cualquier decisión que pueda afectar escalabilidad, resiliencia, consistencia financiera o mantenibilidad.
