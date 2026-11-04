# Правила написания кода

Следуй этим правилам при написании нового кода и не вводи новые подходы без явной просьбы.

## Стек
- Java 21 (toolchain + `options.release = 21`), Gradle (Groovy DSL), Spring Boot 3.5.x, `io.spring.dependency-management`.
- Реактивный стек: Spring WebFlux + Spring Data R2DBC (PostgreSQL, `r2dbc-postgresql`). Блокирующие вызовы в реактивной цепочке запрещены.
- Миграции БД: Liquibase (XML changelog, нужен JDBC-драйвер `postgresql`). Схему не генерировать через ORM.
- Lombok (`compileOnly` + `annotationProcessor`), MapStruct 1.5.x (`componentModel = "spring"`).
- Jackson (+ jsr310) для JSON/JSONB; springdoc-openapi (webflux-ui) для документации API.
- Тесты: JUnit 5, Mockito (`@MockitoBean`), `reactor-test` (`StepVerifier`), `WebTestClient`.
- Кодировка UTF-8. Комментарии, Javadoc, логи и пользовательские сообщения — на русском языке.
- Новые зависимости добавлять в `build.gradle` в сгруппированные блоки с комментарием-заголовком.

## Архитектура
Корневой пакет `com.<org>.<app>`, слоистая структура (зависимости только сверху вниз):

```
controller/            REST-контроллеры (тонкие)
service/               бизнес-логика; service/entity/ — CRUD-сервисы сущностей; service/generator/ — генераторы
dao/model/entity/      R2DBC-сущности (BaseEntity + наследники)
dao/model/json/        POJO для JSONB-полей
dao/model/type/        enum-типы
dao/repository/        R2dbcRepository
dao/converter/         Spring Data converters (Reading/Writing)
mapper/entity/         MapStruct-мапперы entity <-> DTO; mapper/function/ — мапперы для составных результатов
model/request|response DTO запросов/ответов (+ подпакет json/ для вложенных JSON-структур)
model/exception/       DTO ошибок
exception/             иерархия исключений + exception/handler/ (GlobalExceptionHandler)
config/                конфигурации (database, jackson, openapi, properties), WebFilter
utils/                 статические утилиты, utils/validator/ — валидаторы
```

Правила слоёв:
- **Controller**: `@RestController`, `@RequestMapping("/api/v1/<ресурс-в-единственном-числе>")`, `@RequiredArgsConstructor`, поля `private final`. Только приём запроса → маппинг DTO→entity → вызов сервиса → маппинг в Response DTO. Бизнес-логики нет. Возвращает `Mono<T>` / `Flux<T>` / `Mono<Void>`. Entity наружу не отдавать.
- **Service**: CRUD-сервис наследует `BaseEntityService<Entity, UUID>` (create/update/findById/findAll); специфичные запросы добавляются в наследнике. Составные операции — отдельные сервисы (`GameProcessService`), оркеструют entity-сервисы. Сервисы работают с entity, не с DTO. Запись — под `@Transactional`.
- **Repository**: интерфейс `XxxR2dbcRepository extends R2dbcRepository<Entity, UUID>` с `@Repository`; запросы — derived methods или `@Query`.
- **Entity**: наследует `BaseEntity` (UUID `id`, `create_time`, `create_user`, `last_modify_time`, `last_modify_user`; аудит заполняется `AuditingEntityCallback` из reactive context). Аннотации: `@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor @Table("snake_case")`; каждое поле с `@Column("snake_case")` и Javadoc `Поле БД - <имя>`. Типы — обёртки (`Long`, `Boolean`), время — `Instant` (в БД хранится как `bigint` epoch millis через converters).
- **Enum/JSON-поля** в БД хранятся строкой/JSONB; для каждого — пара converters (`XToStringConverter` / `StringToXConverter`, `@WritingConverter` / `@ReadingConverter`), регистрируются в `DatabaseConfig.r2dbcCustomConversions`.
- **Mapper**: интерфейс `XxxEntityMapper extends BaseEntityMapper<Entity>`, `@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)`; методы `requestDtoToEntity`, `entityToResponseDto`, `patchEntityFromEntity` (игнорирует `id`, `createTime`, `createUser`). Ручной маппинг — только `default`-методы в интерфейсе.
- **DTO**: Request implements `RequestEntity`, Response implements `ResponseEntity`; отдельные классы на каждый, без наследования от entity. Request — `@Getter @Setter @NoArgsConstructor`; Response — + `@SuperBuilder @AllArgsConstructor`. Каждый класс — `@Schema(description=...)`, каждое поле — `@Schema` (description, example, requiredMode, format; для аудит-полей `accessMode = READ_ONLY`).
- **Ошибки**: единая иерархия от `<App>Exception` (`message` — для логов/разработчика на английском, `userMessage` — для пользователя на русском): `NotFoundException`, `BadRequestException`, `ValidationException`, `ImpossibleOperationException`. В реактивной цепочке — `Mono.error(...)`/`switchIfEmpty(Mono.error(...))`, вне цепочки (валидаторы) — `throw`. Маппинг в HTTP — только в `GlobalExceptionHandler` (4xx — `warn`/`debug`, 5xx — `error` со стек-трейсом).
- **Валидация** сложных payload — классы `XxxValidator implements DefaultPayloadValidator<T>` (`@Component`), в `utils/validator`.
- **Конфигурация**: свойства приложения — `@ConfigurationProperties` (`config/properties`), значения в `application.yml` под префиксом `app`; локальные — `application-local.yml`. Секреты в код не коммитить.
- **БД-миграции**: `db/changelog/init/changelog-<N.M>-create_table-<table>.xml` (схема) и `db/changelog/initData/...` (данные: csv/sql); подключаются через `changelog-master.xml`. Один changeSet на таблицу, `id` = имя файла, обязательны `<comment>`, `remarks` на каждой колонке, `<rollback>`. Имена таблиц/колонок — `snake_case`, PK — `uuid`, время — `bigint` (epoch ms, default `${now}`), аудит-колонки как в `BaseEntity`. Применённые changeSet не править — добавлять новые.

## Реактивность
- Никаких `block()`, `Thread.sleep`, блокирующего I/O в цепочках.
- Цепочки начинаются с `Mono.deferContextual(Mono::just)` (чтобы контекст — requestId, userId — был доступен), далее `.flatMap`/`.map`; параллельные независимые запросы — `Mono.zip`.
- Контекст запроса (`requestId`, `clientId`) кладётся в Reactor Context в `ReactiveContextFilter`; ключи — константы в `ContextUtil`. Не использовать `ThreadLocal`.
- Заголовки запроса принимать как `@RequestHeader MultiValueMap<String, String>` и разбирать в `RequestHeaderUtils`.

## Код-стайл
- Отступ 4 пробела, фигурные скобки в стиле K&R, импорты без `*`, порядок: проект/сторонние → `java.*` → `static` (отдельными блоками, как у IDEA по умолчанию). Неиспользуемые импорты удалять.
- Именование: классы `PascalCase` с суффиксами слоя (`…Controller`, `…Service`, `…R2dbcRepository`, `…Entity`, `…EntityMapper`, `…RequestDto`, `…ResponseDto`, `…Validator`, `…Converter`, `…Exception`); методы/поля `camelCase`; константы `UPPER_SNAKE_CASE` (`private static final`); пакеты — нижний регистр.
- Внедрение зависимостей — только через конструктор (`@RequiredArgsConstructor` + `private final`); field injection запрещён (кроме тестов).
- Утилитные классы: `@NoArgsConstructor(access = AccessLevel.PRIVATE)`, статические методы.
- Проверки на null — `Objects.isNull/nonNull`, `StringUtils.hasText`, `ObjectUtils.isEmpty`; `@NonNull`/`@Nullable` (Spring) в сигнатурах исключений и переопределяемых методах.
- Логирование: `@Slf4j`, параметризованные сообщения (`{}`), на русском. В сервисах — `doOnSuccess` (info/debug) и `doOnError` (error) с идентификаторами сущностей (playerId, seasonId и т.п.). Не логировать секреты.
- Javadoc — на публичных базовых классах и полях entity; закомментированный мёртвый код не добавлять.
- Коммиты: короткие английские сообщения в повелительном наклонении в стиле `Add …` / `Fix …` (например, `Add openapi description for map request/response dto`).

## Тесты
- Расположение зеркально коду: `src/test/java/<тот же пакет>/XxxTest.java`; вспомогательное — `TestHelper`.
- Класс помечается `@DisplayName("Класс Xxx")`; группировка сценариев — `@Nested` + `@DisplayName` на русском; методы `testOk`, `testXxx`.
- Контроллеры: `@WebFluxTest(XxxController.class)` + `@ContextConfiguration(classes = {Controller, MapperImpl})`, `@MockitoBean` для сервиса, проверки через `WebTestClient`.
- Сервисы: `@SpringJUnitConfig(classes = {Service, MapperImpl})`, `@MockitoBean` для репозитория и `TransactionalOperator`, проверки через `StepVerifier` (`expectNext`, `verifyComplete`, `expectError`).
- Тестовые данные собираются через `@SuperBuilder` в приватных фабричных методах (`createTestInputDto`).
- Каждая новая бизнес-логика / эндпоинт сопровождается тестами на успешный и ошибочный сценарии.
