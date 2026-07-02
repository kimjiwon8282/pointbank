USE banking_db;

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    account_number VARCHAR(30) NOT NULL UNIQUE,
    account_name VARCHAR(100) NOT NULL,
    account_password_hash VARCHAR(255) NOT NULL,
    balance BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_accounts_member_id (member_id)
);

CREATE TABLE IF NOT EXISTS transfer_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    transfer_id BIGINT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_transfer_requests_member_key (member_id, idempotency_key)
);

CREATE TABLE IF NOT EXISTS transfers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    risk_score INT NOT NULL DEFAULT 0,
    failure_reason VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_transfers_from_created (from_account_id, created_at DESC, id DESC),
    INDEX idx_transfers_to_created (to_account_id, created_at DESC, id DESC)
);

CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    transfer_id BIGINT NOT NULL,
    entry_type VARCHAR(30) NOT NULL,
    amount BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    description VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_ledger_account_created_id (account_id, created_at DESC, id DESC)
);

CREATE TABLE IF NOT EXISTS fraud_reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transfer_id BIGINT NOT NULL,
    risk_score INT NOT NULL,
    reasons VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);
