# Diagrama de Secuencia — Wallet CRUD y Drag & Drop

## Flujo: Crear billetera

```
Cliente          API Gateway      wallet-service       account-service (REST)   ledger-service (evento)
  |                   |                |                       |                       |
  |--POST /wallets--->|                |                       |                       |
  |  (Idempotency-Key)|                |                       |                       |
  |                   |--validate JWT->|                       |                       |
  |                   |                |--check Redis idempotency key                  |
  |                   |                |  (not found → continúa)                      |
  |                   |                |                       |                       |
  |                   |                |--GET /accounts/{id}-->|                       |
  |                   |                |  (verifica que la cuenta existe y está ACTIVE)|
  |                   |                |<--AccountResponse-----|                       |
  |                   |                |                       |                       |
  |                   |                |--INSERT wallet (balance=0)                    |
  |                   |                |--INSERT outbox_event (WalletCreated)          |
  |                   |                |--SET Redis idempotency key TTL 24h            |
  |                   |                |--COMMIT TX                                    |
  |                   |                |                       |                       |
  |                   |<--201 {wallet}-|                       |                       |
  |<--201 {wallet}----|                |                       |                       |
  |                   |                |                       |                       |
  |                   |       [Outbox Poller publica WalletCreated]                   |
  |                   |                |--WalletCreated----------------------------->  | (audit)
  |                   |                |                                               |
  |                   |       [ledger-service consume WalletCreated]                  |
  |                   |                |                                          |--INSERT wallet_balances (balance=0)
```

---

## Flujo: Listar billeteras con filtros

```
Cliente          API Gateway      wallet-service       ledger-service (REST)
  |                   |                |                       |
  |--GET /wallets---->|                |                       |
  |  (?category=SAVINGS&accountId=...) |                       |
  |                   |--validate JWT->|                       |
  |                   |                |--query wallets WHERE userId + filters
  |                   |                |                       |
  |                   |                |  [Para cada wallet: balance viene de proyección]
  |                   |                |  (balance es el campo proyectado actualizado por eventos)
  |                   |                |                       |
  |                   |<--200 [wallets]|                       |
  |<--200 [wallets]----|               |                       |
```

---

## Flujo: Drag & Drop — Mover transacción entre billeteras

Este es el feature central del MVP. El usuario arrastra una transacción de Wallet A a Wallet B.

```
Cliente          API Gateway      wallet-service       transaction-service   ledger-service
  |                   |                |                       |                  |
  |--POST /wallets/transfer----------->|                       |                  |
  |  {sourceWalletId,  |               |                       |                  |
  |   destinationWalletId,             |                       |                  |
  |   amount}          |               |                       |                  |
  |   (X-Idempotency-Key)              |                       |                  |
  |                   |--validate JWT->|                       |                  |
  |                   |                |--check Redis idempotency key             |
  |                   |                |--validate sourceWallet.userId == JWT.userId
  |                   |                |--validate destinationWallet.userId == JWT.userId
  |                   |                |--validate sourceWallet.balance >= amount |
  |                   |                |--validate sourceWalletId != destinationWalletId
  |                   |                |                       |                  |
  |                   |                |--INSERT wallet_transfer (PROCESSING)     |
  |                   |                |--INSERT outbox_event (WalletTransferCompleted)
  |                   |                |--COMMIT TX                               |
  |                   |                |                       |                  |
  |                   |<--202 {transferId, PROCESSING}         |                  |
  |<--202 ACCEPTED----|                |                       |                  |
  |                   |                |                       |                  |
  |                   |       [Outbox Poller publica WalletTransferCompleted]     |
  |                   |                |--WalletTransferCompleted---------------->|
  |                   |                |                       |                  |
  |                   |       [ledger-service registra partida doble]            |
  |                   |                |                       |             |--INSERT DEBIT  (sourceWallet)
  |                   |                |                       |             |--INSERT CREDIT (destinationWallet)
  |                   |                |                       |             |--UPDATE wallet_balances (ambas)
  |                   |                |                       |             |--INSERT outbox_event (LedgerEntryCreated)
  |                   |                |                       |             |--COMMIT TX
  |                   |                |                       |                  |
  |                   |       [Outbox Poller publica LedgerEntryCreated]          |
  |                   |                |<--LedgerEntryCreated--------------------|
  |                   |                |                       |                  |
  |                   |       [wallet-service actualiza proyección]               |
  |                   |                |--UPDATE wallets SET balance WHERE id = sourceWalletId
  |                   |                |--UPDATE wallets SET balance WHERE id = destinationWalletId
  |                   |                |--UPDATE wallet_transfer (COMPLETED)
  |                   |                |--COMMIT TX
```

---

## Flujo: Consultar movimientos de una billetera con filtros

```
Cliente          API Gateway      wallet-service       ledger-service (REST)
  |                   |                |                       |
  |--GET /wallets/{id}/transactions--->|                       |
  |  (?dateFrom=&dateTo=&type=INCOME)  |                       |
  |                   |                |--validate ownership (userId == JWT.userId)
  |                   |                |--GET /ledger/entries/{walletId}?filters->|
  |                   |                |<--[LedgerEntry list]------------------|
  |                   |                |--map to TransactionResponse           |
  |                   |<--200 [transactions + pagination]                      |
  |<--200-------------|                |                       |
```

---

## Invariantes de Wallet

1. Una billetera solo puede existir si la cuenta asociada está en estado `ACTIVE`.
2. El saldo nunca puede ser negativo (validado a nivel de DB con CHECK constraint).
3. Solo el dueño de la billetera puede operarla (validación por `userId` del JWT).
4. Una transferencia entre la misma billetera es rechazada con 422.
5. Una billetera solo puede desactivarse si `balance == 0`.
6. El saldo mostrado al cliente siempre proviene de la proyección actualizada por `LedgerEntryCreated`.
