# MealPlannerCalendar

Семейное приложение планирования питания: календарь на месяц и общая картотека блюд с рецептами. Авторизации нет: любой, кто открыл страницу, видит и редактирует всё одинаково.

Техническое задание: `2026-10-31-meal-planner-calendar-app.md`.

## Разработчики

- Кучеров Филипп
- Шигаев Александр

## Возможности

- **Календарь на месяц**: сетка с понедельника, у каждого дня четыре приёма пищи (завтрак, обед, ужин, перекус), текущий день выделен, переключение месяцев и кнопка «Сегодня».
- **Страница дня**: добавление и удаление блюд в каждом приёме пищи, выбор из картотеки с быстрым поиском.
- **Картотека блюд**: создание, редактирование и удаление блюд (название, описание, состав, способ приготовления, картинка, метки приёмов пищи).
- **Предложение блюд для слота**: для приёма пищи показываются блюда с подходящей меткой и блюда без меток. Правило проверяется и на бэкенде.
- **Импорт с iamcook.ru**: поиск рецепта по названию, выбор из найденных и подстановка данных в форму блюда. Источник рецептов подключаемый (интерфейс `RecipeSourceClient`).

## Стек

### Бэкенд (`backend/`)
- Java 17, Gradle (Groovy DSL)
- Spring Boot 3.5, Spring WebFlux (реактивный стек)
- Spring Data R2DBC, PostgreSQL 16
- Liquibase (XML-миграции)
- Lombok, MapStruct
- jsoup (разбор HTML iamcook.ru)
- springdoc-openapi (Swagger UI)
- Тесты: JUnit 5, Mockito, Reactor Test, WebTestClient, Testcontainers

### Фронтенд (`frontend/`)
- React 18, TypeScript (strict), Vite
- react-router-dom, TanStack Query
- CSS Modules, без UI-фреймворков
- Тесты: Vitest, React Testing Library, MSW
- ESLint, Prettier

### Инфраструктура
- Docker Compose (PostgreSQL для локальной разработки)

## Структура репозитория

```
backend/             бэкенд (Spring Boot)
frontend/            фронтенд (React + Vite)
docker-compose.yml   PostgreSQL для разработки
```

## Требования

- Java 17+ (Gradle wrapper скачивается сам)
- Node.js 22
- Docker

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
