package com.events.goup.dto.event;


import com.events.goup.dto.category.CategoryResponse;
import com.events.goup.dto.location.LocationResponse;
import com.events.goup.dto.user.UserSummaryResponse;
import com.events.goup.entity.enums.AgeRating;
import com.events.goup.entity.enums.EventStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record EventResponse(
        Long id,
        String title,
        String description,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal price,
        EventStatus status,
        AgeRating ageRating,
        UserSummaryResponse user,
        LocationResponse location,
        CategoryResponse category
) {
}
