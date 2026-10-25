# API

REST, префикс `/api/v1`, JSON.

- **Спецификация:** [`api/openapi.json`](api/openapi.json) (экспорт из springdoc) и Swagger UI на http://localhost:8080/swagger-ui.html.
- **Коллекция запросов:** [`api/postman_collection.json`](api/postman_collection.json) — сквозной сценарий S2 и кабинет специалиста; токены и id сохраняются в переменные коллекции.
- **Авторизация:** `Authorization: Bearer <accessToken>` из `POST /auth/login`.
- **Ошибки:** Problem Details (RFC 9457). В поле `errors` — сообщения, которые можно показать пользователю. Коды:
  - `401` — нет токена;
  - `403` — не та роль;
  - `404` — нет заявки или она чужая;
  - `409` — недопустимый переход статуса или заявку уже изменили;
  - `422` — нарушены бизнес-правила.

## Авторизация

| Метод | Путь | Назначение |
|---|---|---|
| POST | `/auth/login` | Вход, возвращает JWT и профиль |
| GET | `/auth/me` | Текущий пользователь |

## Продукты и подбор

| Метод | Путь | Назначение |
|---|---|---|
| GET | `/products` | Продукты, цели, типы документов |
| POST | `/products/recommendations` | Подбор по задаче: `{purpose, amount, urgent}` → продукты с причинами и оценкой платежа |
| GET | `/companies/lookup?inn=` | Автозаполнение из ЕГРЮЛ/ЕГРИП. ИНН проверяется по контрольным цифрам |

## Кабинет клиента (роли `DIRECTOR`, `ACCOUNTANT`)

| Метод | Путь | Назначение |
|---|---|---|
| GET | `/applications` | Заявки своей компании |
| POST | `/applications` | Создать черновик |
| GET | `/applications/{id}` | Заявка: статус, этап, `nextAction`, шаги конвейера, предложение, открытый запрос, документы, история, объяснение решения |
| PATCH | `/applications/{id}` | Изменить черновик |
| POST | `/applications/{id}/submit` | Отправить (`{"bkiConsent": true}`) — запускает конвейер |
| POST | `/applications/{id}/cancel` | Отозвать |
| POST | `/applications/{id}/documents` | Загрузить документ: multipart `file` + `docType` |
| GET | `/applications/{id}/documents/{docId}` | Скачать документ (клиент и специалист) |
| POST | `/applications/{id}/info-requests/{rid}/response` | Ответить на запрос: `{"answers": {key: text}}` |
| POST | `/applications/{id}/offer/accept` | Принять предложение → код подтверждения |
| POST | `/applications/{id}/offer/sign` | Подписать кодом — только `DIRECTOR` |
| POST | `/applications/{id}/offer/reject` | Отказаться от предложения |

## Кабинет кредитного специалиста (роль `UNDERWRITER`)

| Метод | Путь | Назначение |
|---|---|---|
| GET | `/bank/applications?status=` | Очередь; без `status` — все поданные |
| GET | `/bank/applications/{id}` | Карточка: заявка, компания, ответы источников, решения движка и специалиста, запросы |
| POST | `/bank/applications/{id}/assign` | Взять в работу |
| POST | `/bank/applications/{id}/decision` | `{outcome: APPROVE \| COUNTER_OFFER \| DECLINE, productCode?, amount?, termMonths?, rate?, reason?, note?}`. Пустые поля контрпредложения берутся из рекомендации движка |
| POST | `/bank/applications/{id}/info-requests` | Запросить у клиента: `{items: [{label, type: TEXT \| FILE}], note?}` |

## Заглушки внешних источников (WireMock, `mocks/`)

| Метод | Путь | Ответ |
|---|---|---|
| GET | `/egrul/v1/companies/{inn}` | Статус, дата регистрации, ОКВЭД, руководитель, учредители |
| GET | `/fns/v1/business/{inn}` | Налоговая задолженность, массовость, выручка по отчётности |
| POST | `/bki/v1/reports` | `{inn, consentId}` → кредиты, платежи, просрочки |

Неизвестный ИНН → `404`. У ООО «Тишина» БКИ отвечает с задержкой 5 с, это больше таймаута клиента (3 с).
