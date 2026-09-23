package com.events.goup.dto.event;
import com.events.goup.entity.enums.AgeRating;
import com.events.goup.entity.enums.EventStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record EventRequest(

        @NotBlank(message = "O título é obrigatório")
        String title,

        String description,

        @NotNull(message = "A data do evento é obrigatória")
        @FutureOrPresent(message = "A data do evento não pode estar no passado")
        LocalDate eventDate,

        @NotNull(message = "O horário de início é obrigatório")
        LocalTime startTime,

        LocalTime endTime,

        @NotNull(message = "O preço é obrigatório")
        @PositiveOrZero(message = "O preço não pode ser negativo")
        BigDecimal price,

        EventStatus status,

        @NotNull(message = "A classificação etária é obrigatória")
        AgeRating ageRating,

        @NotNull(message = "O local é obrigatório")
        Long locationId,

        Long categoryId
) {
}