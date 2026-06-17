# identity-service

## Responsabilidad

Microservicio responsable de la emisión, validación y revocación de tokens JWT utilizando RS256. Gestiona roles, permisos y refresh tokens. Es la autoridad central de identidad de la plataforma.

**No gestiona credenciales de usuario.** Esa responsabilidad pertenece a `auth-service`.

---

## Bounded Context

Identity Context — Domain Driven Design.

---

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.x |
| Seguridad | Spring Security + JJWT (RS256) |
| Persistencia | PostgreSQL |
| Cache | Redis |
| Observabilidad | OpenTelemetry |
| Documentación | OpenAPI 3.1 |

---

## Responsabilidades

- Generación de JWT firmados con RS256.
- Validación de JWT recibidos desde otros servicios y el API Gateway.
- Emisión y rotación de Refresh Tokens.
- Gestión de roles y permisos por usuario.
- Revocación de tokens (blacklist en Redis).

---

## Lo que NO hace este servicio

- No gestiona credenciales (usuario/contraseña). → `auth-service`
- No gestiona sesiones. → `auth-service`
- No gestiona MFA. → `auth-service`
- No gestiona datos del cliente. → `user-service`

---

## Endpoints REST

### POST /api/v1/tokens

Genera un JWT para un usuario autenticado. Llamado internamente por `auth-service`.

**Request:**

```json
{
  "userId": "uuid",
  "email": "string",
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "permissions": ["WALLET_READ", "WALLET_WRITE", "TRANSACTION_CREATE"],
  "sessionId": "uuid"
}
```

**Response 200:**

```json
{
  "accessToken": "string (JWT)",
  "refreshToken": "string (JWT)",
  "tokenId": "uuid",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

**Response 400:** `InvalidTokenRequestException`

---

### POST /api/v1/tokens/validate

Valida un JWT. Usado por el API Gateway y servicios internos.

**Request:**

```json
{
  "token": "string (JWT)"
}
```

**Response 200:**

```json
{
  "valid": true,
  "userId": "uuid",
  "tokenId": "uuid",
  "roles": ["string"],
  "permissions": ["string"],
  "expiresAt": "datetime"
}
```

**Response 401:** `TokenInvalidException`

**Response 401:** `TokenExpiredException`

**Response 401:** `TokenRevokedException`

---

### POST /api/v1/tokens/refresh

Renueva un access token a partir de un refresh token válido.

**Request:**

```json
{
  "refreshToken": "string (JWT)"
}
```

**Response 200:**

```json
{
  "accessToken": "string (JWT)",
  "tokenId": "uuid",
  "expiresIn": 3600
}
```

---

### DELETE /api/v1/tokens/{tokenId}

Revoca un token específico. Agrega el tokenId a la blacklist en Redis.

**Response 204:** Sin contenido.

**Response 404:** `TokenNotFoundException`

---

### GET /api/v1/roles/{userId}

Retorna los roles y permisos de un usuario.

**Response 200:**

```json
{
  "userId": "uuid",
  "roles": ["ROLE_USER"],
  "permissions": ["WALLET_READ", "WALLET_WRITE", "TRANSACTION_CREATE"]
}
```

---

### PUT /api/v1/roles/{userId}

Actualiza roles y permisos de un usuario.

**Request:**

```json
{
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "permissions": ["WALLET_READ", "WALLET_WRITE"]
}
```

**Response 200:** Roles actualizados.

---

## DTOs

### TokenRequest

```java
public record TokenRequest(
    @NotNull UUID userId,
    @NotBlank String email,
    @NotEmpty List<String> roles,
    List<String> permissions,
    @NotNull UUID sessionId
) {}
```

### TokenResponse

```java
public record TokenResponse(
    String accessToken,
    String refreshToken,
    UUID tokenId,
    long expiresIn,
    String tokenType
) {}
```

### TokenValidationRequest

```java
public record TokenValidationRequest(
    @NotBlank String token
) {}
```

### TokenValidationResponse

```java
public record TokenValidationResponse(
    boolean valid,
    UUID userId,
    UUID tokenId,
    List<String> roles,
    List<String> permissions,
    Instant expiresAt
) {}
```

### RoleUpdateRequest

```java
public record RoleUpdateRequest(
    @NotEmpty List<String> roles,
    List<String> permissions
) {}
```

---

## Modelo de Datos

### Tabla: `user_roles`

```sql
CREATE TABLE user_roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL UNIQUE,
    roles       TEXT[] NOT NULL,
    permissions TEXT[],
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Tabla: `refresh_tokens`

```sql
CREATE TABLE refresh_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_id     UUID NOT NULL UNIQUE,
    user_id      UUID NOT NULL,
    token_hash   TEXT NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    revoked      BOOLEAN NOT NULL DEFAULT false,
    revoked_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Redis: Token Blacklist

```
Key:   token:blacklist:{tokenId}
Value: "revoked"
TTL:   igual al tiempo de expiración restante del token
```

---

## Eventos Publicados

Este servicio no publica eventos a Kafka directamente. La comunicación es sincrónica (REST).

Los eventos `TokenIssued` y `TokenRevoked` son publicados vía Outbox cuando se requiere trazabilidad en `audit-service`.

---

## Seguridad

- JWT firmado con RS256 (clave privada en Vault / Secrets Manager).
- Clave pública expuesta en `GET /api/v1/.well-known/jwks.json` para validación por otros servicios.
- Blacklist de tokens revocados en Redis con TTL.
- Rate limiting en endpoints de generación de tokens.

---

## Diagrama de Secuencia

Ver: `sequence-diagrams/token-issuance.md`

---

## Configuración

```yaml
server:
  port: 8081

spring:
  application:
    name: identity-service
  datasource:
    url: ${SPRING_DATASOURCE_URL}
  redis:
    host: ${SPRING_REDIS_HOST}
    port: 6379

jwt:
  private-key: ${JWT_PRIVATE_KEY_PATH}
  public-key: ${JWT_PUBLIC_KEY_PATH}
  access-token-expiry: 3600
  refresh-token-expiry: 86400
```

---

## Testing

| Capa | Herramienta | Cobertura objetivo |
|---|---|---|
| Domain | JUnit 5 | 90% |
| Application (use cases) | JUnit 5 + Mockito | 85% |
| Infrastructure (adapters) | @SpringBootTest + Testcontainers | 70% |

Casos críticos a cubrir:
- Generación de token con roles válidos.
- Validación de token expirado.
- Validación de token revocado (blacklist).
- Refresh de token válido.
- Refresh de token expirado.
- Revocación de token.
