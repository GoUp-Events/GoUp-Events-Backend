package com.events.goup.dto.location;

import com.events.goup.entity.enums.PriceLevel;

public record LocationResponse(
        Long id,
        String placeId,
        String name,
        String formattedAddress,
        String address,
        String number,
        String neighborhood,
        String city,
        String state,
        String zip,
        Double latitude,
        Double longitude,
        Double rating,
        PriceLevel priceLevel
) {
}
