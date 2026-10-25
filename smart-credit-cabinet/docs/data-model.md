# Модель данных

PostgreSQL. Схемой управляет Flyway: [`V1__init.sql`](../backend/src/main/resources/db/migration/V1__init.sql). SQL совместим с H2 в режиме PostgreSQL — на нём идут тесты. JSON-снимки (ответы источников, факторы, предложения) хранятся в `text`.

```mermaid
erDiagram
    companies ||--o{ users : "сотрудники клиента"
    companies ||--o{ applications : "заявки"
    users ||--o{ applications : "создал"
    applications ||--|{ application_checks : "шаги конвейера"
    applications ||--o{ application_events : "журнал"
    applications ||--o{ decisions : "решения"
    decisions ||--o| offers : "предложение"
    applications ||--o{ info_requests : "запросы информации"
    applications ||--o{ documents : "документы"
    info_requests |o--o{ documents : "по запросу"

    companies {
        bigint id PK
        varchar inn UK
        varchar name
        varchar legal_form "OOO | IP"
        date registered_at
    }
    users {
        bigint id PK
        varchar login UK
        varchar role "DIRECTOR | ACCOUNTANT | UNDERWRITER"
        bigint company_id FK
    }
    applications {
        bigint id PK
        varchar number UK
        varchar status
        varchar product_code
        varchar purpose
        bigint amount
        int term_months
        bigint revenue_12m
        bigint version "оптимистическая блокировка"
    }
    application_checks {
        varchar step "VALIDATION | EGRUL | FNS | BKI | DECISION"
        varchar status "PENDING | RUNNING | OK | FAILED"
        int attempts
        text result "снимок ответа источника"
    }
    application_events {
        varchar event_type
        varchar from_status
        varchar to_status
        varchar actor_type "CLIENT | SYSTEM | UNDERWRITER"
        text message
    }
    decisions {
        varchar source "ENGINE | UNDERWRITER"
        varchar outcome
        int score
        varchar grade
        text factors
        text reasons
        text proposal
        varchar policy_version
    }
    offers {
        varchar kind "AS_REQUESTED | COUNTER"
        bigint amount
        int term_months
        double rate
        varchar status "ACTIVE | ACCEPTED | SIGNED | REJECTED"
    }
    info_requests {
        varchar requested_by "SYSTEM | UNDERWRITER"
        varchar return_status
        text items
        text response
    }
    documents {
        varchar doc_type
        varchar filename
        varchar sha256
        varchar storage_path
    }
```

| Таблица | Зачем |
|---|---|
| `companies`, `users` | Клиенты и сотрудники. У сотрудника банка нет компании |
| `applications` | Центральная сущность. `version` защищает от одновременной правки двумя специалистами |
| `application_checks` | Шаги конвейера и снимки ответов ЕГРЮЛ/ФНС/БКИ — данные, на которых принято решение |
| `application_events` | Только вставка. История для клиента и аудит для банка |
| `decisions` | Решения движка и специалиста: балл, факторы, причины, предложение или рекомендация, версия политики |
| `offers` | Предложение клиенту, статус принятия и подписания (хэш кода) |
| `info_requests` | Запросы документов и пояснений: кто запросил, куда вернуть заявку после ответа |
| `documents` | Метаданные файлов и SHA-256; сами файлы лежат на томе `uploads` |
