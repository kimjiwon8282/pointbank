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

CREATE TABLE IF NOT EXISTS securities_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100) NOT NULL,
    market VARCHAR(20) NOT NULL,
    sector VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_securities_products_stock_code UNIQUE (stock_code),
    CONSTRAINT chk_securities_products_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),

    INDEX idx_securities_products_market (market),
    INDEX idx_securities_products_status (status)
);

CREATE TABLE IF NOT EXISTS securities_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NULL,
    securities_account_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    order_side VARCHAR(20) NOT NULL,
    quantity BIGINT NOT NULL,
    order_price BIGINT NOT NULL,
    order_amount BIGINT NOT NULL,
    fee BIGINT NOT NULL DEFAULT 0,
    tax BIGINT NOT NULL DEFAULT 0,
    -- BUY: total debit amount (order_amount + fee).
    -- SELL: net credit amount (order_amount - fee - tax).
    total_amount BIGINT NOT NULL,
    quote_observed_at DATETIME(6) NULL,
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(255) NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_securities_orders_order_no UNIQUE (order_no),
    CONSTRAINT uk_securities_orders_member_id_idempotency_key UNIQUE (member_id, idempotency_key),
    CONSTRAINT chk_securities_orders_side CHECK (order_side IN ('BUY', 'SELL')),
    CONSTRAINT chk_securities_orders_status
        CHECK (status IN ('REQUESTED', 'FUNDS_COMPLETED', 'COMPLETED', 'FAILED', 'MANUAL_REVIEW')),
    CONSTRAINT chk_securities_orders_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_securities_orders_price_positive CHECK (order_price > 0),
    CONSTRAINT chk_securities_orders_amount_positive CHECK (order_amount > 0),
    CONSTRAINT chk_securities_orders_fee_non_negative CHECK (fee >= 0),
    CONSTRAINT chk_securities_orders_tax_non_negative CHECK (tax >= 0),
    CONSTRAINT chk_securities_orders_total_non_negative CHECK (total_amount >= 0),
    CONSTRAINT chk_securities_orders_total_amount_policy CHECK (
        (order_side = 'BUY' AND total_amount = order_amount + fee AND tax = 0)
        OR
        (order_side = 'SELL' AND total_amount = order_amount - fee - tax)
    ),

    INDEX idx_securities_orders_member_created (member_id, created_at DESC, id DESC),
    INDEX idx_securities_orders_account_created (securities_account_id, created_at DESC, id DESC),
    INDEX idx_securities_orders_stock_code (stock_code),
    INDEX idx_securities_orders_status (status)
);

CREATE TABLE IF NOT EXISTS securities_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    securities_account_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    execution_side VARCHAR(20) NOT NULL,
    execution_price BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    execution_amount BIGINT NOT NULL,
    fee BIGINT NOT NULL DEFAULT 0,
    tax BIGINT NOT NULL DEFAULT 0,
    buy_cost BIGINT NULL,
    realized_profit BIGINT NULL,
    realized_return_rate DECIMAL(10, 4) NULL,
    executed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_securities_executions_order_id UNIQUE (order_id),
    CONSTRAINT chk_securities_executions_side CHECK (execution_side IN ('BUY', 'SELL')),
    CONSTRAINT chk_securities_executions_price_positive CHECK (execution_price > 0),
    CONSTRAINT chk_securities_executions_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_securities_executions_amount_positive CHECK (execution_amount > 0),
    CONSTRAINT chk_securities_executions_fee_non_negative CHECK (fee >= 0),
    CONSTRAINT chk_securities_executions_tax_non_negative CHECK (tax >= 0),
    CONSTRAINT chk_securities_executions_realized_fields CHECK (
        (execution_side = 'BUY'
            AND buy_cost IS NULL
            AND realized_profit IS NULL
            AND realized_return_rate IS NULL
            AND tax = 0)
        OR
        (execution_side = 'SELL'
            AND buy_cost IS NOT NULL
            AND buy_cost >= 0
            AND realized_profit IS NOT NULL
            AND realized_return_rate IS NOT NULL)
    ),

    INDEX idx_securities_executions_order_no (order_no),
    INDEX idx_securities_executions_member_executed (member_id, executed_at DESC, id DESC),
    INDEX idx_securities_executions_account_executed (securities_account_id, executed_at DESC, id DESC),
    INDEX idx_securities_executions_stock_code (stock_code),
    INDEX idx_securities_executions_side (execution_side)
);

CREATE TABLE IF NOT EXISTS securities_holdings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    securities_account_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    stock_code VARCHAR(20) NOT NULL,
    quantity BIGINT NOT NULL,
    reserved_quantity BIGINT NOT NULL DEFAULT 0,
    avg_buy_price DECIMAL(18, 4) NOT NULL,
    total_buy_amount BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_securities_holdings_account_stock UNIQUE (securities_account_id, stock_code),
    CONSTRAINT chk_securities_holdings_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT chk_securities_holdings_reserved_non_negative CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_securities_holdings_available_non_negative CHECK (quantity >= reserved_quantity),
    CONSTRAINT chk_securities_holdings_avg_price_non_negative CHECK (avg_buy_price >= 0),
    CONSTRAINT chk_securities_holdings_total_amount_non_negative CHECK (total_buy_amount >= 0),

    INDEX idx_securities_holdings_member_id (member_id),
    INDEX idx_securities_holdings_stock_code (stock_code)
);

INSERT IGNORE INTO securities_products (stock_code, stock_name, market, sector, status)
VALUES
    ('005930', '삼성전자', 'KOSPI', '반도체', 'ACTIVE'),
    ('000660', 'SK하이닉스', 'KOSPI', '반도체', 'ACTIVE'),
    ('035420', 'NAVER', 'KOSPI', '인터넷', 'ACTIVE'),
    ('035720', '카카오', 'KOSPI', '인터넷', 'ACTIVE');

CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_securities_outbox_events_event_id UNIQUE (event_id),
    INDEX idx_securities_outbox_events_status_created (status, created_at)
);

CREATE TABLE IF NOT EXISTS processed_messages (
    event_id VARCHAR(64) PRIMARY KEY,
    message_type VARCHAR(80) NOT NULL,
    processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
