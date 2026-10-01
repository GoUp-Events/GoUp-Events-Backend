package com.events.goup.dto.favorite;

import com.events.goup.dto.event.EventResponse;

import java.time.LocalDateTime;

public record FavoriteResponse(
        LocalDateTime favoritedAt,
        EventResponse event
) {
}
