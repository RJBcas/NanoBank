# auth-service

## Responsabilidad

Microservicio responsable del proceso de autenticación de usuarios. Gestiona login, logout, MFA y sesiones activas. Delega la emisión de tokens JWT a `identity-service`.

---

## Bounded Context

Authentication Context — Domain Driven Design.

---

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.x |
| Seguridad | Spring Security + BCrypt |
| Persistencia | PostgreSQL |
| Cache | Redis (sesiones) |
| Mensajería | Kafka (eventos de sesión) |
| Observabilidad | OpenTelemetry |

---

## Responsabilidades

- Validación de credenciales (email + password).
- Gestión del flujo MFA (TOTP / OTP por email).
- Creación y terminación de sesiones.
- Delegación de emisión de JWT a `identity-service`.
- Publicación de eventos: `UserLoggedIn`, `UserLoggedOut`.

---

## Lo que NO hace este servicio

- No emite JWT. → `identity-service`
- No gestiona datos del cliente. → `user-service`
- No gestiona roles. → `identity-service`

---

## Endpoints REST

### POST /api/v1/auth/login

Autentica al usuario y retorna tokens JWT.

**Request:**

```json
{
  "email": "string",
  "password": "string",
  "deviceId": "string (opcional)"
}
```

**Response 200:**

```json
{
  "accessToken": "string (JWT)",
  "refreshToken": "string (JWT)",
  "sessionId": "uuid",
  "mfaRequired": false,
  "mfaToken": null
}
```

**Response 200 (MFA requerido):**

```json
{
  "accessToken": null,
  "refreshToken": null,
  "sessionId": null,
  "mfaRequired": true,
  "mfaToken": "string (token temporal para completar MFA)"
}
```

**Response 401:** `InvalidCredentialsException`

**Response 423:** `AccountLockedException`

---

### POST /api/v1/auth/mfa/verify

Verifica el código MFA y completa el login.

**Request:**

```json
{
  "mfaToken": "string",
  "otp": "string (6 dígitos)"
}
```

**Response 200:**

```json
{
  "accessToken": "string (JWT)",
  "refreshToken": "string (JWT)",
  "sessionId": "uuid"
}
```

**Response 401:** `MFAInvalidException`

**Response 401:** `MFAExpiredException`

---

### POST /api/v1/auth/logout

Termina la sesión activa y revoca el token.

**Headers:** `Authorization: Bearer {token}`

**Response 204:** Sin contenido.

---

### POST /api/v1/auth/refresh

Renueva el access token. Delegado a `identity-service`.

**Request:**

```json
{
  "refreshToken": "string"
}
```

**Response 200:**

```json
{
  "accessToken": "string (JWT)",
  "expiresIn": 3600
}
```

---

### GET /api/v1/auth/sessions

Lista las sesiones activas del usuario autenticado.

**Headers:** `Authorization: Bearer {token}`

**Response 200:**

```json
{
  "sessions": [
    {
      "sessionId": "uuid",
      "deviceId": "string",
      "ipAddress": "string",
      "createdAt": "datetime",
      "lastActivityAt": "datetime"
    }
  ]
}
```

---

### DELETE /api/v1/auth/sessions/{sessionId}

Termina una sesión específica.

**Response 204:** Sin contenido.

**Response 404:** `SessionNotFoundException`

---

## DTOs

### LoginRequest

```java
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    String deviceId
) {}
```

### LoginResponse

```java
public record LoginResponse(
    String accessToken,
    String refreshToken,
    UUID sessionId,
    boolean mfaRequired,
    String mfaToken
) {}
```

### MFAVerifyRequest

```java
public record MFAVerifyRequest(
    @NotBlank String mfaToken,
    @NotBlank @Size(min = 6, max = 6) String otp
) {}
```

### SessionResponse

```java
public record SessionResponse(
    UUID sessionId,
    String deviceId,
    String ipAddress,
    Instant createdAt,
    Instant lastActivityAt
) {}
```

---

## Modelo de Datos

### Tabla: `user_credentials`

```sql
CREATE TABLE user_credentials (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    mfa_enabled     BOOLEAN NOT NULL DEFAULT false,
    mfa_secret      TEXT,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Tabla: `sessions`

```sql
CREATE TABLE sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL UNIQUE,
    user_id         UUID NOT NULL,
    device_id       TEXT,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_activity   TIMESTAMPTZ NOT NULL DEFAULT now(),
    terminated_at   TIMESTAMPTZ
);
```

### Redis: MFA Temporal

```
Key:   mfa:pending:{mfaToken}
Value: { userId, sessionId, expiresAt }
TTL:   300 segundos (5 minutos)
```

---

## Eventos Publicados

| Evento | Topic | Descripción |
|---|---|---|
| UserLoggedIn | nanobank.auth.session.created | Login exitoso completado |
| UserLoggedOut | nanobank.auth.session.terminated | Sesión terminada |

---

## Integración con identity-service

`auth-service` llama a `identity-service` vía REST para:

1. Obtener JWT post-login exitoso: `POST /api/v1/tokens`
2. Revocar JWT en logout: `DELETE /api/v1/tokens/{tokenId}`
3. Renovar JWT: `POST /api/v1/tokens/refresh`

---

## Diagrama de Secuencia

Ver: `sequence-diagrams/login-flow.md`

---

## Seguridad

- Passwords hasheados con BCrypt (cost factor 12).
- Bloqueo de cuenta tras 5 intentos fallidos (configurable).
- Rate limiting en `/auth/login` (10 req/min por IP).
- MFA con TOTP (RFC 6238) o OTP por email.
- Sesiones almacenadas en Redis con TTL.

---

## Configuración

```yaml
server:
  port: 8082

spring:
  application:
    name: auth-service

auth:
  max-failed-attempts: 5
  lockout-duration-minutes: 30
  mfa-otp-expiry-seconds: 300
  session-ttl-seconds: 86400

services:
  identity-service:
    url: ${IDENTITY_SERVICE_URL:http://identity-service:8081}
```

---

## Testing

Casos críticos a cubrir:

- Login con credenciales válidas sin MFA.
- Login con credenciales válidas con MFA.
- Login con credenciales inválidas (incremento de contador).
- Bloqueo de cuenta por intentos excedidos.
- Verificación de OTP válido.
- Verificación de OTP expirado.
- Logout y revocación de sesión.
- Refresh de token.
