USE ledger_db;

CREATE TABLE IF NOT EXISTS ledger_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    account_number VARCHAR(30) NULL,
    account_password_hash VARCHAR(255) NULL,
    balance BIGINT NOT NULL DEFAULT 0,
    reserved_balance BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_ledger_accounts_account_number UNIQUE (account_number),
    CONSTRAINT uk_ledger_accounts_member_type UNIQUE (member_id, account_type),
    CONSTRAINT chk_ledger_accounts_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_ledger_accounts_reserved_non_negative CHECK (reserved_balance >= 0),
    CONSTRAINT chk_ledger_accounts_available_non_negative CHECK (balance >= reserved_balance),
    INDEX idx_ledger_accounts_member_type (member_id, account_type)
);

CREATE TABLE IF NOT EXISTS ledger_transfer_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_no VARCHAR(64) NOT NULL,
    transfer_type VARCHAR(40) NOT NULL,
    source_account_id BIGINT NULL,
    target_account_id BIGINT NULL,
    from_member_id BIGINT NULL,
    to_member_id BIGINT NULL,
    amount BIGINT NOT NULL,
    source_balance_after BIGINT NULL,
    target_balance_after BIGINT NULL,
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_ledger_transfer_requests_request_no UNIQUE (request_no),
    CONSTRAINT chk_ledger_transfer_requests_amount_positive CHECK (amount > 0),
    INDEX idx_ledger_transfer_requests_status_created (status, created_at DESC, id DESC),
    INDEX idx_ledger_transfer_requests_from_member_created (from_member_id, created_at DESC, id DESC),
    INDEX idx_ledger_transfer_requests_to_member_created (to_member_id, created_at DESC, id DESC)
);

CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_no VARCHAR(64) NOT NULL,
    transfer_request_id BIGINT NULL,
    ledger_account_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    entry_type VARCHAR(40) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    reserved_balance_after BIGINT NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_ledger_entries_request_no (request_no),
    INDEX idx_ledger_entries_account_created (ledger_account_id, created_at DESC, id DESC),
    INDEX idx_ledger_entries_member_created (member_id, created_at DESC, id DESC)
);
