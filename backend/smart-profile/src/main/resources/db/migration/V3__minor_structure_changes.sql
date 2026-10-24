ALTER TABLE credit_products
    ADD active BOOLEAN;

ALTER TABLE credit_products
    ADD created_at TIMESTAMP;

ALTER TABLE credit_products
    ALTER COLUMN active SET NOT NULL;

ALTER TABLE applications
    ADD created_at TIMESTAMP;

ALTER TABLE applications
    ADD credit_product_id INT;

ALTER TABLE applications
    ADD requested_amount BIGINT;

ALTER TABLE applications
    ADD term_months INT;

ALTER TABLE credit_products
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE verifications
    ADD reasons VARCHAR(1000);

ALTER TABLE applications
    ADD CONSTRAINT FK_APPLICATIONS_ON_CREDIT_PRODUCT FOREIGN KEY (credit_product_id) REFERENCES credit_products (id);

ALTER TABLE VERIFICATIONS
DROP
COLUMN CREDIT_PRODUCT_ID;

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

ALTER TABLE applications
DROP
COLUMN status;

ALTER TABLE applications
    ADD status VARCHAR(255);