-- ============================================================
-- NanoBank Ledger — Initial Schema
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id          UUID PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(150) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS wallets (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id),
    name        VARCHAR(100) NOT NULL,
    category    VARCHAR(20)  NOT NULL,
    balance     NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency    VARCHAR(3)   NOT NULL DEFAULT 'COP',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT chk_wallet_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX IF NOT EXISTS idx_wallets_user_id ON wallets(user_id);
CREATE INDEX IF NOT EXISTS idx_wallets_status  ON wallets(status);

CREATE TABLE IF NOT EXISTS transactions (
    id          UUID PRIMARY KEY,
    wallet_id   UUID         NOT NULL REFERENCES wallets(id),
    user_id     UUID         NOT NULL REFERENCES users(id),
    type        VARCHAR(20)  NOT NULL,
    amount      NUMERIC(19,4) NOT NULL,
    currency    VARCHAR(3)   NOT NULL DEFAULT 'COP',
    category    VARCHAR(100),
    description VARCHAR(500),
    occurred_at DATE         NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT chk_transaction_amount_positive CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_transactions_wallet_id   ON transactions(wallet_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id     ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_type        ON transactions(type);
CREATE INDEX IF NOT EXISTS idx_transactions_occurred_at ON transactions(occurred_at);
CREATE INDEX IF NOT EXISTS idx_transactions_category    ON transactions(category);
