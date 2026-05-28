CREATE SEQUENCE IF NOT EXISTS card_number_seq START WITH 1;

CREATE TABLE IF NOT EXISTS bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(14, 2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, type, currency)
);

CREATE TABLE IF NOT EXISTS user_cards (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    card_number VARCHAR(32) NOT NULL UNIQUE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS currency_rates (
    currency VARCHAR(3) PRIMARY KEY,
    rate_to_rub NUMERIC(14, 6) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO currency_rates (currency, rate_to_rub)
VALUES
    ('RUB', 1.000000),
    ('USD', 92.500000),
    ('EUR', 100.200000),
    ('CNY', 12.900000)
ON CONFLICT (currency) DO UPDATE
SET rate_to_rub = EXCLUDED.rate_to_rub,
    updated_at = NOW();

INSERT INTO bank_accounts (user_id, type, currency, balance)
SELECT u.id, 'CURRENT', 'RUB', COALESCE(w.balance, 0.00)
FROM users u
LEFT JOIN wallets w ON w.user_id = u.id
ON CONFLICT (user_id, type, currency) DO NOTHING;

INSERT INTO bank_accounts (user_id, type, currency, balance)
SELECT u.id, 'SAVINGS', 'RUB', 0.00
FROM users u
ON CONFLICT (user_id, type, currency) DO NOTHING;

INSERT INTO bank_accounts (user_id, type, currency, balance)
SELECT u.id, 'INVESTMENT', 'RUB', 0.00
FROM users u
ON CONFLICT (user_id, type, currency) DO NOTHING;

INSERT INTO user_cards (user_id, card_number, is_primary)
SELECT u.id, CONCAT('2200', LPAD(nextval('card_number_seq')::text, 12, '0')), TRUE
FROM users u
WHERE NOT EXISTS (
    SELECT 1
    FROM user_cards c
    WHERE c.user_id = u.id
);

CREATE INDEX IF NOT EXISTS idx_bank_accounts_user_id ON bank_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_bank_accounts_type ON bank_accounts(type);
CREATE INDEX IF NOT EXISTS idx_user_cards_user_id ON user_cards(user_id);
