# Leon Bets Parser

Асинхронний парсер prematch даних букмекера Leon без використання браузера.

## Можливості

- Парсинг даних через HTTP API (без браузера/емулятора)
- Підтримка: Football, Tennis, Hockey, Basketball
- Збір даних з "Top Leagues" для кожного спорту
- Отримання всіх ринків та коефіцієнтів для матчів
- Асинхронна обробка з обмеженням паралельних запитів
- Rate limiting для запобігання блокуванню
- Retry з exponential backoff при помилках API

## Вимоги

- Java 23+
- Gradle 9+

## Конфігурація

Налаштування в `src/main/resources/application.yml`:

```yaml
leon:
  api:
    base-url: https://leon.bet
    timeout: 30s
    retry:
      max-attempts: 3
      delay: 1s
  parser:
    max-parallel-requests: 3    # Максимум паралельних запитів
    matches-per-league: 2       # Матчів на лігу
    target-sports:              # Цільові види спорту
      - Soccer
      - Tennis
      - IceHockey
      - Basketball
```

## Запуск

```bash
./gradlew bootRun
```

## Тести

```bash
./gradlew test
```

## Формат виводу

```
Sport, League
    Match name, Date UTC, Match ID
        Market name
            Outcome, Coefficient, Outcome ID
```

Приклад:

```
Ice Hockey, USA NHL
    Florida Panthers - Buffalo Sabres, 2026-02-03 00:00:00 UTC, 1970324850685030
        Winner
            1, 2.19, 1970327764252977
            X, 4.19, 1970327764252978
            2, 2.74, 1970327764252976
        Winner (Including OT/SO)
            1, 1.77, 1970327764253002
            2, 2.07, 1970327764253001
```

## Структура проєкту

```
src/main/java/com/example/demo/
├── LeonBetsParserApplication.java  # Точка входу
├── config/
│   ├── LeonApiProperties.java      # Конфігурація
│   └── WebClientConfig.java        # HTTP клієнт
├── model/
│   ├── Sport.java                  # Спорт
│   ├── Region.java                 # Регіон
│   ├── League.java                 # Ліга
│   ├── Event.java                  # Матч
│   ├── EventsResponse.java         # Відповідь API
│   ├── Market.java                 # Ринок ставок
│   └── Runner.java                 # Результат
└── service/
    ├── LeonApiService.java         # API клієнт
    └── LeonBetsParser.java         # Основна логіка
```

## Технології

- Spring Boot 4.0.2
- Spring WebFlux (реактивний стек)
- Project Reactor
- Java Records
- Jackson JSON
