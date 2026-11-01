# MealPlannerCalendar

Семейное приложение планирования питания: календарь на месяц и общая картотека блюд с рецептами. Авторизации нет.

Техническое задание: `2026-10-31-meal-planner-calendar-app.md`.

## Требования

- Java 17+ (Gradle wrapper скачивается сам), Node.js 22, Docker.

## Запуск

```powershell
# 1. База данных
docker compose up -d postgres

# 2. Бэкенд (http://localhost:8080, Swagger: /swagger-ui.html)
cd backend
.\gradlew.bat bootRun --args="--spring.profiles.active=local"

# 3. Фронтенд (http://localhost:5173, /api проксируется на :8080)
cd frontend
npm ci
npm run dev
```

## Тесты и сборка

```powershell
cd backend
.\gradlew.bat build              # сборка + unit-тесты
.\gradlew.bat integrationTest    # Testcontainers, нужен Docker

cd frontend
npm run lint; npm test; npm run build
```

## Ограничение

Импорт рецептов с iamcook.ru предназначен только для закрытого семейного использования. Пользовательское соглашение сайта запрещает автоматизированный сбор рецептов без письменного разрешения. Не публикуйте приложение публично и не добавляйте массовый или фоновый импорт.
