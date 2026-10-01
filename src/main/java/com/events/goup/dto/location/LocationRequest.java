package com.events.goup.dto.location;

import jakarta.validation.constraints.NotBlank;

public record LocationRequest(
        @NotBlank(message = "O placeId é obrigatório")
        String placeId
) {
}
