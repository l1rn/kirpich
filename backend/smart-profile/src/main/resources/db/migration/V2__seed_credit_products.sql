INSERT INTO credit_products
(title, min_amount, max_amount, min_term_months, max_term_months, interest_rate, active, created_at)
VALUES
    ('Оборотное кредитование',        500000,  50000000,  6,  36,  12.5, TRUE, CURRENT_TIMESTAMP),
    ('Инвестиционный кредит',         1000000, 200000000, 12, 120, 13.5, TRUE, CURRENT_TIMESTAMP),
    ('Кредитная линия',               500000,  100000000, 3,  24,  12.9, TRUE, CURRENT_TIMESTAMP),
    ('Овердрафт',                     100000,  10000000,  1,  12,  14.0, TRUE, CURRENT_TIMESTAMP),
    ('Экспресс-кредит',               300000,  5000000,   6,  24,  15.5, TRUE, CURRENT_TIMESTAMP),
    ('Кредит под залог недвижимости', 2000000, 150000000, 12, 180, 11.9, TRUE, CURRENT_TIMESTAMP),
    ('Рефинансирование',              1000000, 80000000,  12, 84,  12.2, TRUE, CURRENT_TIMESTAMP);