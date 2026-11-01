package com.coocking.calendar.config.properties;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Настройки подключения к внешнему источнику рецептов ({@code app.external-recipe}).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.external-recipe")
public class ExternalRecipeProperties {

    /** Активный источник рецептов */
    private String source = "iamcook";

    /** Базовый адрес сайта */
    private String baseUrl = "https://www.iamcook.ru";

    /** Путь поиска (POST) */
    private String searchPath = "/advsearch";

    /** Префикс пути страницы рецепта (после него идёт числовой id) */
    private String recipePath = "/showrecipe/";

    /** Таймаут запроса */
    private Duration timeout = Duration.ofSeconds(10);

    /** Минимальный интервал между исходящими запросами */
    private Duration minRequestInterval = Duration.ofSeconds(1);

    /** Максимальное число результатов поиска */
    private int maxResults = 10;

    /** Максимальный размер скачиваемой картинки */
    private DataSize maxImageSize = DataSize.ofMegabytes(5);

    /** Заголовок User-Agent исходящих запросов */
    private String userAgent = "MealPlannerCalendar/1.0 (семейное приложение планирования питания)";

    /** Домен, которому должны принадлежать адреса картинок (допускаются его поддомены) */
    private String imageHostSuffix = "iamcook.ru";

    /** CSS-селекторы разбора HTML */
    private Selectors selectors = new Selectors();

    /**
     * CSS-селекторы страниц источника.
     */
    @Getter
    @Setter
    public static class Selectors {

        /** Контейнер результатов поиска */
        private String searchContainer = "#advsearch";

        /** Карточка рецепта в результатах поиска */
        private String searchCard = "div.recblockwide";

        /** Название рецепта в карточке */
        private String searchCardTitle = "div.header a";

        /** Ссылка на рецепт в карточке */
        private String searchCardLink = "a[href^=\"/showrecipe/\"]";

        /** Миниатюра в карточке */
        private String searchCardImage = "img.preimage";

        /** Счётчик найденных рецептов */
        private String searchCount = "p.scnotice";

        /** Заголовок страницы рецепта */
        private String recipeTitle = "#recbody h1";

        /** Описание рецепта */
        private String recipeDescription = "div.recdescription";

        /** Список ингредиентов */
        private String recipeIngredients = "div.ingredients div.ilist";

        /** Блок шагов приготовления */
        private String recipeInstructions = "div.instructions";

        /** Мета-тег с картинкой */
        private String recipeImageMeta = "meta[property=\"og:image\"]";

        /** Скрипт JSON-LD */
        private String recipeJsonLd = "script[type=\"application/ld+json\"]";
    }
}
