# NanoBank Ledger — MVP

**ID de Evaluación:** FS-SR-2026-002  
**Stack:** Java 21 · Spring Boot 3.3.5 · Angular 17 · H2 (dev) / PostgreSQL (prod)

---

## Inicio Rápido

### Backend

```bash
cd backend/nanobank-ledger
./mvnw spring-boot:run
# Windows: mvnw.cmd spring-boot:run
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:nanobankdb`)

### Frontend

```bash
cd frontend/nanobank-frontend
npm install
npm start
```

- App: `http://localhost:4200`

> El backend debe estar corriendo antes de iniciar el frontend.

---

## Tests

### Backend (JUnit 5 + Mockito)

```bash
cd backend/nanobank-ledger
./mvnw test
./mvnw test jacoco:report
# Reporte: target/site/jacoco/index.html
```

Casos cubiertos: `WalletServiceTest` (11), `TransactionServiceTest` (9), `AuthServiceTest` (4), `WalletTest` domain (6).

### Frontend (Jasmine + Karma)

```bash
cd frontend/nanobank-frontend
npm run test:coverage
# Reporte: coverage/nanobank-frontend/index.html
```

---

## Elección de Base de Datos

| Perfil | Motor | Motivo |
|--------|-------|--------|
| `default` (dev/evaluación) | H2 in-memory (modo PostgreSQL) | Zero-setup para el evaluador. Sin Docker, sin instalación. |
| `prod` | PostgreSQL 15+ | ACID completo, soporte de particionamiento por fecha para el Ledger, extensión `uuid-ossp`, y compatibilidad con consultas financieras complejas con índices parciales. |

**Por qué no MongoDB/NoSQL:** Las transacciones financieras exigen joins referenciales y consistencia ACID en múltiples tablas (wallet → transaction → ledger\_entry). Un modelo documental agregaría complejidad de validación que el modelo relacional resuelve por diseño con foreign keys y transacciones.

**Modelo para escalar:** La tabla `transactions` usa `wallet_id` + `created_at` como candidatos naturales a partition key en PostgreSQL (`PARTITION BY RANGE (created_at)`), lo que permite particionar por mes a medida que el volumen crece, sin cambios en el código de aplicación.

---

## Arquitectura

El MVP implementa un **monolito modular** con paquetes por bounded context (`auth`, `wallet`, `transaction`, `shared`) y arquitectura hexagonal dentro de cada módulo (`domain` / `application` / `infrastructure`).

Esta decisión balancea **entregabilidad en tiempo reducido** con **escalabilidad real**: cada bounded context puede extraerse a un microservicio independiente sin refactoring de dominio, dado que las interfaces de puerto ya están definidas y no hay dependencias cruzadas entre dominios.

Para la visión completa de arquitectura a escala (microservicios, Kafka, Saga, Outbox, Ledger, Observabilidad) ver [`docs/Demo_Readme.md`](docs/Demo_Readme.md).

### Patrones aplicados en el MVP

| Patrón | Dónde |
|--------|-------|
| DTO (Request/Response) | Todas las capas de infrastructure |
| Global Exception Handler | `shared/infrastructure/GlobalExceptionHandler` |
| Repository (Port/Adapter) | Interfaces de dominio + implementaciones JPA |
| JWT Bearer Token | `auth` module + `SecurityConfig` |
| Standalone Components + Signals | Todos los componentes Angular |
| Auth Guard + HTTP Interceptor | `core/guards` + `core/interceptors` |

---

## Funcionalidades Implementadas

- **Auth:** Registro, login, JWT (24h), protección de rutas.
- **Wallets:** Crear, listar, detalle, transferir entre billeteras (actualiza saldos atómicamente).
- **Transacciones:** Registrar ingreso/gasto, filtrar por categoría y fecha en tiempo real.
- **Drag & Drop:** Mover transacción entre billeteras (Angular CDK `DragDropModule`), el backend actualiza los saldos en una sola transacción ACID.
- **Dashboard:** Resumen de billeteras con saldo total, lista de movimientos recientes.

---

## Bitácora de Prompts — IA Mastery

### Prompt 1 — Arquitectura inicial

**Prompt:**
> "Diseña la arquitectura de un sistema de gestión financiera de billeteras digitales que deba escalar a millones de usuarios. El sistema necesita: registro de billeteras, transacciones, transferencias, audit trail y seguridad JWT. Usa DDD y Event-Driven Architecture."

**Lo que la IA generó:** Una arquitectura de microservicios completa con 13 servicios, Kafka, Saga Choreography y Transactional Outbox.

**Modificación aplicada (criterio senior):** La IA propuso empezar con microservicios desde el día 1, lo cual viola el principio de _evolutionary architecture_. Con el tiempo disponible (3 horas), arrancar con 13 servicios independientes habría resultado en un sistema incompleto y no deployable. **Decisión:** implementar un monolito modular con los mismos bounded contexts como paquetes internos. Los puertos hexagonales ya delimitan las fronteras de extracción futura. La arquitectura de microservicios quedó documentada en `docs/` como el _target state_.

---

### Prompt 2 — Backend Spring Boot: módulo wallet

**Prompt:**
> "Genera el módulo wallet para Spring Boot 3.3.5 con arquitectura hexagonal. Necesito: entidad Wallet con campos id, name, type, balance, userId, timestamps; WalletRepository (puerto); WalletService con operaciones create, findAll, findById, transfer; WalletController REST con DTOs separados para request y response; GlobalExceptionHandler con errores tipados."

**Lo que la IA generó:** `WalletService` con lógica de transferencia directamente mezclada con persistencia: el método `transfer()` abría y cerraba la transacción manualmente llamando a `walletRepository.save()` dos veces sin `@Transactional`.

**Modificación aplicada (criterio senior):** Violación de SRP y riesgo de inconsistencia financiera. Si el segundo `save()` falla, el sistema queda con saldo incorrecto. Se refactorizó para que `transfer()` sea un único método `@Transactional` que lee ambas billeteras, valida, modifica en memoria y persiste en una sola operación atómica. Adicionalmente, la IA no separó la validación de negocio del caso de uso, por lo que se extrajo la guard clause de balance insuficiente a una excepción de dominio (`InsufficientFundsException extends DomainException`) en lugar de lanzar `RuntimeException` directamente.

---

### Prompt 3 — Tests unitarios backend

**Prompt:**
> "Genera tests unitarios para WalletService y TransactionService con JUnit 5 y Mockito. Cubre: creación exitosa, wallet no encontrada, transferencia exitosa, transferencia con fondos insuficientes, balance negativo, filtrado por categoría y fecha."

**Lo que la IA generó:** Tests con `@SpringBootTest` y `@AutoConfigureTestDatabase`, levantando el contexto completo de Spring para tests unitarios.

**Modificación aplicada (criterio senior):** `@SpringBootTest` convierte tests unitarios en tests de integración: más lentos, acoplados a la infraestructura y difíciles de aislar para coverage. Se reemplazó por `@ExtendWith(MockitoExtension.class)` con mocks explícitos de repositorios. Esto mantiene los tests en milisegundos, aislados del ORM, y directamente enfocados en la lógica de negocio que es lo que el coverage del servicio debe medir.

---

### Prompt 4 — Frontend Angular: componentes standalone con signals

**Prompt:**
> "Genera los componentes Angular 17 para el dashboard de NanoBank: WalletListComponent, TransactionListComponent, DragDropTransactionComponent. Usa Standalone Components obligatorio, Signals para estado reactivo, CdkDragDrop para drag and drop entre listas de billeteras. Incluye JWT interceptor y Auth Guard."

**Lo que la IA generó:** El `WalletListComponent` usaba `BehaviorSubject` + `async pipe` (patrón RxJS clásico) para el estado de billeteras, y `NgModule` para registrar `DragDropModule`.

**Modificación aplicada (criterio senior):** La prueba técnica exige Signals explícitamente. La IA usó el patrón anterior de RxJS por inercia de entrenamiento. Se reemplazó `BehaviorSubject<Wallet[]>` por `wallets = signal<Wallet[]>([])` y la actualización de estado por `this.wallets.set(data)`. Adicionalmente, en Standalone Components no existe NgModule raíz, por lo que `DragDropModule` debe declararse en el array `imports` del propio decorador `@Component`, no en ningún módulo. La IA generó un `AppModule` innecesario que se eliminó.

---

### Prompt 5 — Drag & Drop con actualización de saldo en backend

**Prompt:**
> "Implementa el endpoint REST para mover una transacción de una billetera a otra: PATCH /api/wallets/transactions/{transactionId}/move con body {targetWalletId}. Debe actualizar los saldos de origen y destino en una sola transacción ACID. Genera también el componente Angular que invoca este endpoint al soltar en el CdkDropList de destino."

**Lo que la IA generó:** El endpoint recibía `sourceWalletId` y `targetWalletId` en el body, pero derivaba el `sourceWalletId` buscando la transacción y luego lo validaba contra el body — una redundancia que expone surface de ataque: el cliente podría enviar un `sourceWalletId` diferente al real.

**Modificación aplicada (criterio senior):** Se eliminó `sourceWalletId` del request body. El servicio determina el wallet origen directamente desde la transacción en base de datos (`transaction.getWalletId()`). El cliente solo necesita proveer `targetWalletId`. Esto aplica el principio de _least privilege input_: no confiar en datos que el servidor puede derivar por sí mismo, reduciendo la superficie de manipulación de saldos.

---

## Estructura del Proyecto

```
Prueba_tecnica/
├── README.md                    ← este archivo
├── backend/
│   └── nanobank-ledger/
│       ├── src/main/java/com/nanobank/ledger/
│       │   ├── auth/            ← JWT, login, registro
│       │   ├── wallet/          ← billeteras, transferencias
│       │   ├── transaction/     ← ingresos, gastos, filtros
│       │   └── shared/          ← exceptions, config, security
│       └── src/test/            ← JUnit 5 + Mockito
└── frontend/
    └── nanobank-frontend/
        └── src/app/
            ├── core/            ← guards, interceptors, models, services
            └── features/
                ├── auth/        ← login, registro
                └── dashboard/   ← wallets, transactions, drag & drop
└── docs/
    ├── Demo_Readme.md           ← arquitectura completa a escala
    ├── planning.md              ← planning inicial
    ├── event-catalog.md         ← catálogo de eventos
    └── services/                ← README por microservicio (target state)
```
