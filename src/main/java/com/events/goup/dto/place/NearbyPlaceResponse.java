package com.events.goup.dto.place;

import com.events.goup.entity.enums.PriceLevel;

public record NearbyPlaceResponse(
        String placeId,
        String name,
        String address,
        String type,
        Double latitude,
        Double longitude,
        Integer distanceMeters,
        Double rating,
        Integer userRatingCount,
        PriceLevel priceLevel,
        String googleMapsUri
) {
}
