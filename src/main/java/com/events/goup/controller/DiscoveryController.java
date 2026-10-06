package com.events.goup.controller;

import com.events.goup.dto.discovery.DiscoveryRequest;
import com.events.goup.dto.discovery.DiscoveryResponse;
import com.events.goup.service.DiscoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/discovery")
@RequiredArgsConstructor
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    // Exclusivo Premium: o DiscoveryService responde 403 se o usuário autenticado não tiver premium = true.
    @PostMapping
    public ResponseEntity<DiscoveryResponse> discover(@Valid @RequestBody DiscoveryRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(discoveryService.discover(request, authentication.getName()));
    }
}
