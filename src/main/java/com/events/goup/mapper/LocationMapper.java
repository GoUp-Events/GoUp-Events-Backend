package com.events.goup.mapper;

import com.events.goup.dto.location.LocationResponse;
import com.events.goup.entity.Location;

public final class LocationMapper {

    private LocationMapper() {
    }

    public static LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getPlaceId(),
                location.getName(),
                location.getFormattedAddress(),
                location.getAddress(),
                location.getNumber(),
                location.getNeighborhood(),
                location.getCity(),
                location.getState(),
                location.getZip(),
                location.getLatitude(),
                location.getLongitude(),
                location.getRating(),
                location.getPriceLevel()
        );
    }
}
