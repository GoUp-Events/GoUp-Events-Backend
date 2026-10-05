package com.events.goup.controller;

import com.events.goup.dto.event.EventResponse;
import com.events.goup.dto.recommendation.RecommendationResponse;
import com.events.goup.dto.user.UserResponse;
import com.events.goup.service.EventService;
import com.events.goup.service.RecommendationService;
import com.events.goup.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EventService eventService;
    private final RecommendationService recommendationService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(userService.findByEmail(authentication.getName()));
    }

    // Inclui os rascunhos (DRAFT), que não aparecem na listagem pública.
    @GetMapping("/me/events")
    public ResponseEntity<List<EventResponse>> myEvents(Authentication authentication) {
        return ResponseEntity.ok(eventService.findAllByOwner(authentication.getName()));
    }

    // Exclusivo Premium: o RecommendationService responde 403 se o usuário não tiver premium = true.
    @GetMapping("/me/recommendations")
    public ResponseEntity<List<RecommendationResponse>> myRecommendations(Authentication authentication) {
        return ResponseEntity.ok(recommendationService.recommend(authentication.getName()));
    }
}
