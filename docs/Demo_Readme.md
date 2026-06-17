# NanoBank Ledger

## Visión General

NanoBank Ledger es una plataforma financiera orientada a la gestión de billeteras digitales, presupuestos personales y movimientos financieros internos y externos.

El sistema fue diseñado bajo principios de Domain Driven Design (DDD), Event Driven Architecture (EDA) y arquitecturas desacopladas, permitiendo escalar a millones de usuarios mientras garantiza consistencia transaccional, trazabilidad completa, resiliencia operativa y cumplimiento de requisitos regulatorios.

La solución permite:

- Gestionar clientes y cuentas financieras.
- Crear y administrar billeteras digitales.
- Realizar transferencias internas y externas.
- Validar reglas de riesgo y límites transaccionales.
- Mantener un Ledger financiero como única fuente de verdad.
- Garantizar idempotencia en operaciones críticas.
- Ejecutar conciliaciones automáticas.
- Auditar todos los eventos de negocio.
- Escalar horizontalmente mediante microservicios y eventos.

---

# Objetivos del Sistema

## Objetivos Funcionales

- Administración de clientes.
- Administración de cuentas financieras.
- Creación y gestión de billeteras.
- Registro de ingresos y gastos.
- Transferencias entre billeteras.
- Transferencias hacia cuentas externas.
- Consulta de movimientos.
- Auditoría de operaciones.
- Conciliación financiera.

## Objetivos No Funcionales

- Escalabilidad horizontal.
- Alta disponibilidad.
- Trazabilidad end-to-end.
- Idempotencia.
- Observabilidad completa.
- Seguridad basada en JWT.
- Recuperación ante fallos.
- Consistencia financiera eventual controlada.
- Soporte para millones de usuarios concurrentes.

---

# Principios Arquitectónicos

La arquitectura fue diseñada bajo los siguientes principios:

### 1. Ledger First

El Ledger es la única fuente de verdad financiera.

Ningún saldo será considerado definitivo fuera del dominio Ledger.

### 2. Event Driven

Toda operación relevante genera eventos de negocio.

### 3. Data Ownership

Cada microservicio es dueño exclusivo de sus datos.

No se permiten accesos directos a bases de datos externas.

### 4. Idempotency First

Toda operación financiera debe ser idempotente.

### 5. Auditability

Toda acción de negocio debe ser trazable y auditable.

### 6. Failure Recovery

Todo proceso debe poder recuperarse ante fallos parciales.

### 7. Security by Design

La seguridad se implementa desde el diseño y no como una capa posterior.

### 8. Low Coupling

Los servicios se comunican principalmente mediante eventos.

---

# Arquitectura General

## Estilo Arquitectónico

La plataforma utiliza:

- Domain Driven Design (DDD)
- Event Driven Architecture (EDA)
- Microservices Architecture
- Saga Choreography Pattern
- Transactional Outbox Pattern
- Hexagonal Architecture
- CQRS Ready Design

---

# Bounded Contexts

## Identity Context

Responsable de:

- Emisión de JWT
- Validación de JWT
- Gestión de roles
- Gestión de permisos
- Refresh Tokens

---

## Authentication Context

Responsable de:

- Login
- Logout
- MFA
- Gestión de sesiones

---

## Customer Context

Responsable de:

- Información del cliente
- Estado del cliente
- Perfil del cliente

---

## Account Context

Responsable de:

- Gestión de cuentas
- Estado de cuentas
- Productos financieros

---

## Wallet Context

Responsable de:

- Gestión de billeteras
- Presupuestos financieros
- Organización financiera personal

Las billeteras representan agrupaciones presupuestales y no son consideradas cuentas bancarias independientes.

---

## Transaction Context

Responsable de:

- Creación de transacciones
- Orquestación funcional del proceso financiero
- Gestión de estados transaccionales

---

## Risk Context

Responsable de:

- Validaciones regulatorias
- Reglas de riesgo
- Restricciones operativas

---

## Limits Context

Responsable de:

- Límites diarios
- Límites mensuales
- Restricciones de montos

---

## Ledger Context

Responsable de:

- Registro financiero inmutable
- Reconstrucción de saldos
- Fuente oficial de verdad financiera

---

## Audit Context

Responsable de:

- Evidencia regulatoria
- Trazabilidad de negocio
- Historial de acciones

---

## Notification Context

Responsable de:

- Notificaciones Push
- Notificaciones Email
- Notificaciones SMS

---

## Reconciliation Context

Responsable de:

- Comparación de estados
- Detección de inconsistencias
- Recuperación financiera

---

# Microservicios

La plataforma está compuesta por los siguientes servicios:

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

# Diagrama de Alto Nivel

```text
Frontend Angular

        |
        v

API Gateway

        |
+-------+--------+--------+--------+
|       |        |        |        |
v       v        v        v        v

Identity
Auth
User
Account
Wallet

                 |
                 v

          Transaction

                 |
     +-----------+-----------+
     |                       |
     v                       v

    Risk                 Limits

                 |
                 v

              Ledger

                 |
                 v

              Outbox

                 |
                 v

               Kafka

      +----------+----------+
      |          |          |
      v          v          v

    Audit   Notification   Reconciliation

                            |
                            v

                    External Bank
```

---

# Consistencia Financiera

La consistencia financiera se implementa mediante Saga Choreography.

Cada servicio participa en el proceso sin un orquestador central.

Esto reduce el acoplamiento y mejora la escalabilidad.

---

# Flujo Principal de Transferencia

1. Cliente solicita transferencia.
2. Transaction Service recibe solicitud.
3. Se valida Idempotency Key.
4. Se consulta información del cliente.
5. Se consulta información de la cuenta.
6. Se valida riesgo.
7. Se validan límites.
8. Se registra movimiento en Ledger.
9. Se ejecuta transferencia.
10. Se genera auditoría.
11. Se envía notificación.
12. Se publica evento de resultado.

---

# Patrón Saga

El sistema implementa Saga Choreography basada en eventos.

Eventos principales:

- TransactionCreated
- RiskValidated
- LimitValidated
- LedgerEntryCreated
- TransferExecuted
- TransferRejected
- AuditCreated
- NotificationSent

---

# Patrón Transactional Outbox

Todos los eventos de negocio son almacenados inicialmente en una tabla Outbox dentro de la misma transacción de base de datos.

Posteriormente son publicados a Kafka.

Beneficios:

- Evita pérdida de eventos.
- Garantiza consistencia.
- Permite reintentos seguros.

---

# Estrategia de Idempotencia

Toda operación financiera requiere:

- Idempotency-Key
- TransactionId
- SessionId
- CorrelationId

Las solicitudes duplicadas retornarán el estado previamente registrado.

La idempotencia es obligatoria para:

- Transferencias
- Pagos
- Movimientos financieros
- Integraciones externas

---

# Resiliencia

## Retry Policy

Se implementan reintentos con backoff exponencial.

Ejemplo:

- 1 segundo
- 2 segundos
- 4 segundos
- 8 segundos

---

## Circuit Breaker

Aplicado principalmente a:

- External Bank Service
- Integraciones externas
- Proveedores de terceros

---

## Dead Letter Queue

Todos los eventos fallidos son enviados a una DLQ para análisis posterior.

---

# Ledger como Fuente de Verdad

El Ledger es el registro financiero oficial del sistema.

Características:

- Inmutable.
- Auditable.
- Trazable.
- Reprocesable.

Ningún servicio distinto al Ledger es considerado fuente oficial de saldos.

---

# Estrategia de Persistencia

| Dominio      | Tecnología         |
| ------------ | ------------------ |
| Customer     | PostgreSQL         |
| Account      | PostgreSQL         |
| Wallet       | PostgreSQL         |
| Transaction  | PostgreSQL         |
| Ledger       | PostgreSQL         |
| Audit        | PostgreSQL         |
| Notification | PostgreSQL         |
| Idempotency  | Redis + PostgreSQL |
| Logs         | OpenSearch         |

---

# Observabilidad

La plataforma implementa observabilidad end-to-end.

Componentes:

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

# Seguridad

## Identity Service

Responsable de:

- Emisión de JWT
- Validación de JWT
- Refresh Tokens

## Authentication Service

Responsable de:

- Login
- Logout
- MFA
- Sesiones

## Controles

- JWT
- TLS
- Rate Limiting
- RBAC
- Secrets Management

---

# Arquitectura de Eventos

Broker seleccionado:

Kafka

Motivos:

- Escalabilidad horizontal.
- Persistencia de eventos.
- Replay de eventos.
- Integración con Event Sourcing.
- Procesamiento masivo.

---

# ADRs (Architecture Decision Records)

## ADR-001

Decisión:

Kafka como Event Broker.

Motivo:

Escalabilidad, retención y replay de eventos.

---

## ADR-002

Decisión:

Saga Choreography.

Motivo:

Reducir acoplamiento entre servicios.

---

## ADR-003

Decisión:

Ledger como fuente única de verdad.

Motivo:

Garantizar trazabilidad y consistencia financiera.

---

## ADR-004

Decisión:

Transactional Outbox.

Motivo:

Garantizar publicación confiable de eventos.

---

## ADR-005

Decisión:

Redis para Idempotencia.

Motivo:

Reducir latencia y soportar alto volumen transaccional.

---

# Roadmap Futuro

- Open Banking APIs
- Multi Tenant
- Event Sourcing Completo
- Real Time Fraud Detection
- AI Risk Scoring
- AML Integrations
- Multi Currency Support
- Global Payment Networks

---

# Conclusión

NanoBank Ledger fue diseñado bajo principios modernos de arquitectura distribuida para soportar crecimiento masivo, resiliencia operativa y trazabilidad financiera completa.

La combinación de DDD, Event Driven Architecture, Saga Choreography, Ledger First, Idempotencia y Observabilidad garantiza una plataforma preparada para evolucionar hacia escenarios empresariales y financieros de gran escala.
