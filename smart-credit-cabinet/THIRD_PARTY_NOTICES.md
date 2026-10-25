# Сторонние компоненты

Проверяемая версия не использует платных подписок, платных API, покупных данных и закрытых лицензий. ML- и LLM-моделей в решении нет.

## Смоделированные интеграции

| Что в реальности | Чем заменено | Где |
|---|---|---|
| ЕГРЮЛ / ЕГРИП (ФНС) | Заглушка WireMock, упрощённый контракт | `mocks/mappings/egrul-*.json` |
| Сервис ФНС «Прозрачный бизнес» | Заглушка WireMock | `mocks/mappings/fns-*.json` |
| Бюро кредитных историй | Заглушка WireMock, включая сценарий таймаута | `mocks/mappings/bki-*.json` |
| Подписание договора (ПЭП/КЭП) | Код подтверждения в интерфейсе | `ClientApplicationService.acceptOffer/signOffer` |
| Выдача кредита | Смена статуса на «Деньги выданы» | там же |

## Данные

Все компании, люди, ИНН, ОГРН, кредиты и суммы синтетические (`DemoDataSeeder`, `mocks/__files`). Параметры продуктов упрощены по публичным страницам alfabank.ru (исследование команды) и не являются офертой.

## Бэкенд (Maven, версии фиксирует `spring-boot-starter-parent` 4.1.1)

| Компонент | Версия | Лицензия |
|---|---|---|
| Spring Boot, Spring Framework, Spring Security, Spring Data JPA | 4.1.1 / 7.0.x / 7.1.x | Apache-2.0 |
| Hibernate ORM | 7.4.x | Apache-2.0 |
| Flyway (Community) | 12.4.x | Apache-2.0 |
| Jackson | 3.1.x / 2.21.x | Apache-2.0 |
| Nimbus JOSE + JWT | 10.9.x | Apache-2.0 |
| springdoc-openapi (Swagger UI) | 3.1.1 | Apache-2.0 |
| PostgreSQL JDBC Driver | из BOM Spring Boot | BSD-2-Clause |
| H2 Database (тесты и локальный запуск) | 2.4.x | MPL-2.0 / EPL-1.0 |
| Lombok | из BOM Spring Boot | MIT |
| JUnit 5, AssertJ, Mockito, JsonPath (тесты) | из BOM Spring Boot | EPL-2.0 / Apache-2.0 / MIT / Apache-2.0 |

## Фронтенд (npm, версии фиксирует `package-lock.json`)

| Компонент | Версия | Лицензия |
|---|---|---|
| React, React DOM | 19.3.1 | MIT |
| React Router | 7.18.4 | MIT |
| Ant Design, @ant-design/icons | 6.6.7 / 6.3.4 | MIT |
| Vite, @vitejs/plugin-react | 8.3.4 / 6.1.2 | MIT |
| TypeScript | 5.9.3 | Apache-2.0 |

## Образы Docker

| Образ | Тег | Лицензия |
|---|---|---|
| postgres | 17.11-alpine | PostgreSQL License |
| wiremock/wiremock | 3.13.2 | Apache-2.0 |
| eclipse-temurin (JDK/JRE) | 21.0.12.1_1-noble | GPL-2.0 with Classpath Exception |
| node | 24.21.0-alpine | MIT |
| nginx | 1.28.3-alpine | BSD-2-Clause |

## Инструменты разработки

Инструменты, которыми пользовались при разработке (в том числе ИИ-ассистенты для написания кода), в проверяемую версию не входят.
