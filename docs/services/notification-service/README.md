# notification-service

## Responsabilidad

Microservicio responsable del envío de notificaciones a usuarios (Push, Email, SMS). Opera de forma totalmente asíncrona consumiendo eventos de negocio.

---

## Bounded Context

Notification Context — Domain Driven Design.

---

## Responsabilidades

- Consumir eventos relevantes y generar notificaciones.
- Soportar múltiples canales: Push, Email, SMS.
- Gestionar preferencias de notificación por usuario.
- Reintentar notificaciones fallidas con backoff exponencial.
- Registrar historial de notificaciones enviadas.

---

## Eventos Consumidos

| Evento | Notificación generada |
|---|---|
| TransactionCompleted | "Transferencia de {amount} completada" |
| TransactionFailed | "Tu transferencia no pudo procesarse" |
| LimitExceeded | "Has alcanzado tu límite diario/mensual" |
| UserLoggedIn | "Nuevo inicio de sesión detectado" |
| AccountBlocked | "Tu cuenta ha sido bloqueada" |

---

## Endpoints REST

### GET /api/v1/notifications/{userId}

Lista el historial de notificaciones del usuario.

**Query params:** `channel`, `dateFrom`, `dateTo`, `page`, `size`

**Response 200:** Lista paginada de notificaciones.

### PUT /api/v1/notifications/preferences/{userId}

Actualiza preferencias de notificación del usuario.

**Request:**

```json
{
  "pushEnabled": true,
  "emailEnabled": true,
  "smsEnabled": false,
  "transactionAlerts": true,
  "securityAlerts": true
}
```

---

## DTOs

### NotificationResponse

```java
public record NotificationResponse(
    UUID notificationId,
    UUID userId,
    String channel,
    String title,
    String message,
    String status,
    Instant sentAt
) {}
```

### NotificationPreferencesRequest

```java
public record NotificationPreferencesRequest(
    boolean pushEnabled,
    boolean emailEnabled,
    boolean smsEnabled,
    boolean transactionAlerts,
    boolean securityAlerts
) {}
```

---

## Modelo de Datos

### Tabla: `notifications`

```sql
CREATE TABLE notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id     UUID NOT NULL UNIQUE,
    user_id             UUID NOT NULL,
    channel             VARCHAR(10) NOT NULL,
    title               VARCHAR(100) NOT NULL,
    message             TEXT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count         INTEGER NOT NULL DEFAULT 0,
    sent_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Tabla: `notification_preferences`

```sql
CREATE TABLE notification_preferences (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE,
    push_enabled        BOOLEAN NOT NULL DEFAULT true,
    email_enabled       BOOLEAN NOT NULL DEFAULT true,
    sms_enabled         BOOLEAN NOT NULL DEFAULT false,
    transaction_alerts  BOOLEAN NOT NULL DEFAULT true,
    security_alerts     BOOLEAN NOT NULL DEFAULT true,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## Configuración

```yaml
server:
  port: 8091

spring:
  application:
    name: notification-service

notification:
  max-retries: 3
  retry-backoff-seconds: [1, 2, 4, 8]
```

---

## Testing

- Enviar notificación push para TransactionCompleted.
- Respetar preferencias del usuario (canal desactivado).
- Reintentar con backoff en fallo de envío.
- Registrar notificación en historial tras envío.
