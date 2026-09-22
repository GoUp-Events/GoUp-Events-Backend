package com.events.goup.dto.user;

import java.time.LocalDateTime;

public record UserResponse(Long id, String name, String email, String role, LocalDateTime createdAt) {
}
