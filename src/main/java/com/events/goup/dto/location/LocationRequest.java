package com.events.goup.dto.location;
public record LocationRequest(
        String name,
        String address,
        String number,
        String neighborhood,
        String city,
        String state,
        String zip
) {
}