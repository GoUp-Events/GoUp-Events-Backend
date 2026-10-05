package com.events.goup.dto.plan;

import java.util.List;

public record PlanResponse(
        String name,
        boolean premium,
        int nearbyLimit,
        List<String> benefits
) {
}
