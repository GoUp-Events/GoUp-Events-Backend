package com.events.goup.dto.place;

public record NearbyPlaceResponse(
        String placeId,
        String name,
        String address,
        String type,
        Double latitude,
        Double longitude,
        Integer distanceMeters,
        String googleMapsUri
) {
}
