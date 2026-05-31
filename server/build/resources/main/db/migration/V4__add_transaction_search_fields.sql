-- Сохраняем номер карты и номер договора в транзакции,
-- чтобы поддержать поиск по этим реквизитам на клиенте.
ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS recipient_card_number VARCHAR(32),
    ADD COLUMN IF NOT EXISTS contract_number VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_transactions_card_number
    ON transactions(recipient_card_number);
CREATE INDEX IF NOT EXISTS idx_transactions_contract_number
    ON transactions(contract_number);
