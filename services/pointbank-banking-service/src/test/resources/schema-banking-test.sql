CREATE TABLE accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    account_password_hash VARCHAR(255) NOT NULL,
    balance BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_accounts_member_id UNIQUE (member_id),
    CONSTRAINT uk_accounts_account_number UNIQUE (account_number),
    INDEX idx_accounts_member_id (member_id),
    CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0)
);

CREATE TABLE account_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    transfer_id BIGINT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    amount BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_account_transactions_member_created (member_id, created_at DESC, id DESC),
    INDEX idx_account_transactions_account_created (account_id, created_at DESC, id DESC),
    INDEX idx_account_transactions_transfer_id (transfer_id)
);

CREATE TABLE transfers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transfer_no VARCHAR(40) NOT NULL,
    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    from_member_id BIGINT NOT NULL,
    to_member_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    CONSTRAINT uk_transfers_transfer_no UNIQUE (transfer_no),
    INDEX idx_transfers_from_member_created (from_member_id, created_at DESC, id DESC),
    INDEX idx_transfers_to_member_created (to_member_id, created_at DESC, id DESC),
    INDEX idx_transfers_from_account_created (from_account_id, created_at DESC, id DESC),
    INDEX idx_transfers_to_account_created (to_account_id, created_at DESC, id DESC),
    INDEX idx_transfers_status_created (status, created_at DESC, id DESC),
    CONSTRAINT chk_transfers_amount_positive CHECK (amount > 0)
);
