# user-service

## Responsabilidad

Microservicio responsable de la gestión del perfil del cliente. Almacena información personal, estado del cliente y datos de contacto.

---

## Bounded Context

Customer Context — Domain Driven Design.

---

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.x |
| Persistencia | PostgreSQL |
| Mensajería | Kafka |
| Observabilidad | OpenTelemetry |

---

## Responsabilidades

- Registro y gestión del perfil del cliente.
- Validación de unicidad de email y documento.
- Gestión del estado del cliente (ACTIVE, SUSPENDED, DEACTIVATED).
- Publicación de eventos de ciclo de vida del cliente.

---

## Lo que NO hace este servicio

- No gestiona credenciales. → `auth-service`
- No gestiona cuentas financieras. → `account-service`
- No emite tokens. → `identity-service`

---

## Endpoints REST

### POST /api/v1/users

Registra un nuevo cliente.

**Request:**

```json
{
  "email": "string",
  "fullName": "string",
  "documentType": "DNI | PASSPORT | NIT",
  "documentNumber": "string",
  "phone": "string",
  "birthDate": "date (YYYY-MM-DD)"
}
```

**Response 201:**

```json
{
  "userId": "uuid",
  "email": "string",
  "fullName": "string",
  "status": "PENDING_VERIFICATION",
  "createdAt": "datetime"
}
```

**Response 409:** `UserAlreadyExistsException`

---

### GET /api/v1/users/{userId}

Retorna el perfil de un cliente.

**Response 200:**

```json
{
  "userId": "uuid",
  "email": "string",
  "fullName": "string",
  "documentType": "string",
  "documentNumber": "string",
  "phone": "string",
  "status": "ACTIVE",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

**Response 404:** `UserNotFoundException`

---

### PUT /api/v1/users/{userId}

Actualiza datos del cliente.

**Request:** Campos opcionales: `fullName`, `phone`.

**Response 200:** Perfil actualizado.

---

### PUT /api/v1/users/{userId}/status

Cambia el estado del cliente. Solo para `ROLE_ADMIN`.

**Request:**

```json
{
  "status": "ACTIVE | SUSPENDED | DEACTIVATED",
  "reason": "string"
}
```

**Response 200:** Estado actualizado.

---

### GET /api/v1/users/me

Retorna el perfil del usuario autenticado.

**Response 200:** Perfil del usuario extraído del JWT.

---

## DTOs

### CreateUserRequest

```java
public record CreateUserRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 150) String fullName,
    @NotNull DocumentType documentType,
    @NotBlank String documentNumber,
    @NotBlank String phone,
    @NotNull LocalDate birthDate
) {}
```

### UserResponse

```java
public record UserResponse(
    UUID userId,
    String email,
    String fullName,
    DocumentType documentType,
    String documentNumber,
    String phone,
    UserStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
```

### UpdateUserRequest

```java
public record UpdateUserRequest(
    @Size(max = 150) String fullName,
    String phone
) {}
```

### UpdateUserStatusRequest

```java
public record UpdateUserStatusRequest(
    @NotNull UserStatus status,
    @NotBlank String reason
) {}
```

---

## Enumeraciones de Dominio

```java
public enum UserStatus {
    PENDING_VERIFICATION, ACTIVE, SUSPENDED, DEACTIVATED
}

public enum DocumentType {
    DNI, PASSPORT, NIT
}
```

---

## Modelo de Datos

### Tabla: `users`

```sql
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    full_name       VARCHAR(150) NOT NULL,
    document_type   VARCHAR(20) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    phone           VARCHAR(20),
    birth_date      DATE,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_document UNIQUE (document_type, document_number)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
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
| UserCreated | nanobank.customer.profile.created | Usuario registrado |
| UserUpdated | nanobank.customer.profile.updated | Datos actualizados |
| UserDeactivated | nanobank.customer.profile.deactivated | Usuario desactivado |

---

## Testing

- Crear usuario con datos válidos.
- Rechazar creación con email duplicado.
- Rechazar creación con documento duplicado.
- Actualizar datos permitidos.
- Cambiar estado con rol admin.
- Publicar UserCreated tras creación exitosa.
