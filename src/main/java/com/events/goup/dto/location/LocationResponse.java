package com.events.goup.dto.location;
public record LocationResponse(
        Long id,
        String name,
        String address,
        String number,
        String neighborhood,
        String city,
        String state,
        String zip
) {
}