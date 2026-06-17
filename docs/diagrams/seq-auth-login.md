# Diagrama de Secuencia — Flujo de Autenticación

## Flujo: Login exitoso sin MFA

```
Cliente          API Gateway      auth-service        identity-service      audit-service
  |                   |                |                    |                    |
  |--POST /auth/login>|                |                    |                    |
  |                   |--forward------>|                    |                    |
  |                   |                |                    |                    |
  |                   |                |--query user_credentials (email)         |
  |                   |                |--verify BCrypt(password, hash)          |
  |                   |                |--check failed_attempts < 5              |
  |                   |                |                    |                    |
  |                   |                |--POST /tokens----->|                    |
  |                   |                |  (userId, roles,   |                    |
  |                   |                |   permissions,     |                    |
  |                   |                |   sessionId)       |                    |
  |                   |                |                    |--sign JWT (RS256)   |
  |                   |                |                    |--INSERT refresh_token
  |                   |                |                    |--COMMIT             |
  |                   |                |<--{accessToken,    |                    |
  |                   |                |   refreshToken}----|                    |
  |                   |                |                    |                    |
  |                   |                |--INSERT session     |                   |
  |                   |                |--INSERT outbox_event (UserLoggedIn)     |
  |                   |                |--COMMIT             |                   |
  |                   |                |                     |                   |
  |                   |<--200 {tokens}-|                     |                   |
  |<--200 {tokens}----|                |                     |                   |
  |                   |                |                     |                   |
  |                   |       [Outbox Poller publica UserLoggedIn]               |
  |                   |                |--UserLoggedIn--------------------------->|
  |                   |                |                     |  [audit persiste]  |
```

---

## Flujo: Login con MFA requerido

```
Cliente          API Gateway      auth-service        identity-service
  |                   |                |                    |
  |--POST /auth/login>|                |                    |
  |                   |--forward------>|                    |
  |                   |                |--verify credentials |
  |                   |                |--check mfa_enabled = true
  |                   |                |                    |
  |                   |                |--generate mfaToken (UUID)
  |                   |                |--SET Redis mfa:{mfaToken} TTL 300s
  |                   |                |--SEND OTP via email/TOTP
  |                   |                |                    |
  |                   |<--200 {mfaRequired:true, mfaToken}->|
  |<--200 {mfaToken}--|                |                    |
  |                   |                |                    |
  |--POST /auth/mfa/verify------------>|                    |
  |  (mfaToken + otp) |                |                    |
  |                   |                |--GET Redis mfa:{mfaToken}
  |                   |                |--validate OTP      |
  |                   |                |                    |
  |                   |                |--POST /tokens----->|
  |                   |                |<--{tokens}---------|
  |                   |                |                    |
  |                   |                |--DEL Redis mfa:{mfaToken}
  |                   |                |--INSERT session     |
  |                   |                |--INSERT outbox_event (UserLoggedIn)
  |                   |                |--COMMIT             |
  |                   |<--200 {tokens}-|                    |
  |<--200 {tokens}----|                |                    |
```

---

## Flujo: Validación de JWT en API Gateway

```
Cliente          API Gateway      identity-service
  |                   |                |
  |--GET /wallets---->|                |
  |  (Bearer token)   |                |
  |                   |--check Redis token:blacklist:{tokenId}
  |                   |  (si existe → 401 inmediato)
  |                   |                |
  |                   |--POST /tokens/validate-->|
  |                   |                |--verify RS256 signature
  |                   |                |--check expiry
  |                   |                |--check blacklist Redis
  |                   |<--{valid:true, claims}-|
  |                   |                |
  |                   |--forward con X-User-Id, X-Roles headers
  |                   |--forward a wallet-service
```

---

## Flujo: Refresh Token

```
Cliente          API Gateway      auth-service        identity-service
  |                   |                |                    |
  |--POST /auth/refresh-------------->  |                    |
  |  (refreshToken)   |                |                    |
  |                   |                |--POST /tokens/refresh-->|
  |                   |                |                    |--verify refresh token
  |                   |                |                    |--check revoked = false
  |                   |                |                    |--check expires_at
  |                   |                |                    |--sign new accessToken
  |                   |                |<--{accessToken}----|
  |                   |<--200----------+                    |
  |<--200 {accessToken}               |                    |
```
