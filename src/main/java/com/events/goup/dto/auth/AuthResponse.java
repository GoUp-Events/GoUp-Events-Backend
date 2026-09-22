package com.events.goup.dto.auth;

import com.events.goup.dto.user.UserResponse;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
