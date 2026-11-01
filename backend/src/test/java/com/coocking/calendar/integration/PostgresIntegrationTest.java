package com.coocking.calendar.integration;

import java.util.List;
import java.util.Map;

import com.coocking.calendar.CalendarApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Проверка миграций Liquibase и репозиториев на настоящем PostgreSQL (Testcontainers).
 * Если Docker недоступен, тест пропускается.
 */
@Tag("integration")
@DisplayName("Интеграционный тест PostgreSQL")
@SpringBootTest(classes = CalendarApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostgresIntegrationTest {

    private static PostgreSQLContainer<?> postgres;

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void startContainer() {
        boolean dockerAvailable;
        try {
            dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException e) {
            dockerAvailable = false;
        }
        assumeTrue(dockerAvailable, "Docker недоступен, интеграционный тест пропущен");
        postgres = new PostgreSQLContainer<>("postgres:16");
        postgres.start();
    }

    @AfterAll
    static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + postgres.getHost() + ":"
                + postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.liquibase.url", postgres::getJdbcUrl);
        registry.add("spring.liquibase.user", postgres::getUsername);
        registry.add("spring.liquibase.password", postgres::getPassword);
    }

    @Test
    @DisplayName("Сценарий: блюда с метками, фильтр по JSONB, план питания и каскадное удаление")
    void testFullScenario() {
        String breakfastId = createDish("Омлет", List.of("BREAKFAST"));
        String anyId = createDish("Салат", List.of());
        createDish("Борщ", List.of("LUNCH", "DINNER"));

        webTestClient.get().uri("/api/v1/dish?mealType=BREAKFAST").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].name").isEqualTo("Омлет")
                .jsonPath("$[0].mealTypes[0]").isEqualTo("BREAKFAST")
                .jsonPath("$[1].name").isEqualTo("Салат");

        webTestClient.get().uri("/api/v1/dish?mealType=LUNCH&search=ЛАТ").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Салат");

        webTestClient.get().uri("/api/v1/dish").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(3);

        addToPlan("2026-11-15", "BREAKFAST", breakfastId, 201);
        addToPlan("2026-11-15", "BREAKFAST", anyId, 201);
        addToPlan("2026-11-15", "BREAKFAST", anyId, 422);
        addToPlan("2026-11-15", "LUNCH", breakfastId, 422);

        webTestClient.get().uri("/api/v1/meal-plan?from=2026-11-01&to=2026-11-30").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].position").isEqualTo(1)
                .jsonPath("$[0].dishName").isEqualTo("Омлет")
                .jsonPath("$[1].position").isEqualTo(2)
                .jsonPath("$[1].hasImage").isEqualTo(false);

        webTestClient.delete().uri("/api/v1/dish/" + breakfastId).exchange().expectStatus().isNoContent();

        webTestClient.get().uri("/api/v1/meal-plan?from=2026-11-01&to=2026-11-30").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    @DisplayName("Картинка сохраняется в БД и отдаётся с Content-Type")
    void testImageRoundTrip() {
        String id = createDish("Блюдо с фото", List.of("SNACK"));
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        org.springframework.http.client.MultipartBodyBuilder builder =
                new org.springframework.http.client.MultipartBodyBuilder();
        builder.part("file", new org.springframework.core.io.ByteArrayResource(png) {
            @Override
            public String getFilename() {
                return "a.png";
            }
        }).contentType(MediaType.IMAGE_PNG);

        webTestClient.put().uri("/api/v1/dish/" + id + "/image")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.hasImage").isEqualTo(true);

        webTestClient.get().uri("/api/v1/dish/" + id + "/image").exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG)
                .expectHeader().exists("Cache-Control")
                .expectBody(byte[].class).isEqualTo(png);

        webTestClient.delete().uri("/api/v1/dish/" + id + "/image").exchange().expectStatus().isOk();
        webTestClient.get().uri("/api/v1/dish/" + id + "/image").exchange().expectStatus().isNotFound();

        webTestClient.delete().uri("/api/v1/dish/" + id).exchange().expectStatus().isNoContent();
    }

    private String createDish(String name, List<String> mealTypes) {
        Map<?, ?> body = webTestClient.post().uri("/api/v1/dish")
                .bodyValue(Map.of("name", name, "mealTypes", mealTypes))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult().getResponseBody();
        return (String) body.get("id");
    }

    private void addToPlan(String date, String mealType, String dishId, int expectedStatus) {
        webTestClient.post().uri("/api/v1/meal-plan")
                .bodyValue(Map.of("date", date, "mealType", mealType, "dishId", dishId))
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }
}
