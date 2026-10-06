package com.events.goup.dto.recommendation;

import com.events.goup.dto.event.EventResponse;

public record RecommendationResponse(
        String reason,
        EventResponse event
) {
}
