package com.events.goup.dto.discovery;

import com.events.goup.dto.event.EventResponse;

import java.util.List;

public record DiscoveryResponse(
        String summary,
        List<EventResponse> events
) {
}
