USE securities_db;

CREATE TABLE IF NOT EXISTS securities_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    account_password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_securities_accounts_member_id UNIQUE (member_id),
    CONSTRAINT uk_securities_accounts_account_number UNIQUE (account_number),

    INDEX idx_securities_accounts_member_id (member_id)
);
