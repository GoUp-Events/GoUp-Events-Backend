package com.events.goup.mapper;

import com.events.goup.dto.category.CategoryResponse;
import com.events.goup.dto.event.EventResponse;
import com.events.goup.dto.user.UserSummaryResponse;
import com.events.goup.entity.Category;
import com.events.goup.entity.Event;

public final class EventMapper {

    private EventMapper() {
    }

    public static EventResponse toResponse(Event event) {
        UserSummaryResponse userResponse = new UserSummaryResponse(
                event.getUser().getId(),
                event.getUser().getName()
        );

        Category category = event.getCategory();
        CategoryResponse categoryResponse = category != null
                ? new CategoryResponse(category.getId(), category.getName(), category.getDescription())
                : null;

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getPrice(),
                event.getStatus(),
                event.getAgeRating(),
                userResponse,
                LocationMapper.toResponse(event.getLocation()),
                categoryResponse
        );
    }
}
