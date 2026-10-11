package com.events.goup.dto.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Filtros de GET /events. Todos são opcionais.
 *
 * @param sort "date" (padrão, Free) ou "popular" (mais favoritados, exclusivo Premium)
 */
public record EventFilter(
        String q,
        String city,
        Long categoryId,
        LocalDate dateFrom,
        LocalDate dateTo,
        BigDecimal maxPrice,
        String sort,
        List<Long> categoryIds,
        Boolean freeOnly,
        String ageRating
) {
}
