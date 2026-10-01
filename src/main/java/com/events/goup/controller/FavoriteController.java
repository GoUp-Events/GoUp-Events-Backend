package com.events.goup.controller;

import com.events.goup.dto.favorite.FavoriteResponse;
import com.events.goup.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/events/{eventId}/favorite")
    public ResponseEntity<Void> favorite(@PathVariable Long eventId, Authentication authentication) {
        favoriteService.favorite(eventId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/events/{eventId}/favorite")
    public ResponseEntity<Void> unfavorite(@PathVariable Long eventId, Authentication authentication) {
        favoriteService.unfavorite(eventId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me/favorites")
    public ResponseEntity<List<FavoriteResponse>> myFavorites(Authentication authentication) {
        return ResponseEntity.ok(favoriteService.findAllByUser(authentication.getName()));
    }
}
