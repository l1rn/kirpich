ALTER TABLE credit_products
    ADD active BOOLEAN;

ALTER TABLE credit_products
    ADD created_at TIMESTAMP;

ALTER TABLE credit_products
    ALTER COLUMN active SET NOT NULL;

ALTER TABLE credit_products
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE credit_products
    ALTER COLUMN interest_rate SET NOT NULL;

ALTER TABLE credit_products
    ALTER COLUMN max_amount SET NOT NULL;

ALTER TABLE credit_products
    ALTER COLUMN max_term_months SET NOT NULL;

ALTER TABLE credit_products
    ALTER COLUMN min_amount SET NOT NULL;

ALTER TABLE credit_products
    ALTER COLUMN min_term_months SET NOT NULL;