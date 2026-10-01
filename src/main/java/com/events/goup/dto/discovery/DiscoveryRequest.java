package com.events.goup.dto.discovery;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiscoveryRequest(
        @NotBlank(message = "A mensagem é obrigatória")
        @Size(max = 500, message = "A mensagem deve ter no máximo 500 caracteres")
        String message,

        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres")
        String city
) {
}
