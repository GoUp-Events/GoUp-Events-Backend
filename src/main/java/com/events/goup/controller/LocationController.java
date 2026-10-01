package com.events.goup.controller;

import com.events.goup.dto.location.LocationRequest;
import com.events.goup.dto.location.LocationResponse;
import com.events.goup.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public ResponseEntity<List<LocationResponse>> findAll() {
        return ResponseEntity.ok(locationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.findById(id));
    }

    /**
     * Recebe o placeId do autocomplete do Google e devolve o Location salvo (novo ou reaproveitado).
     */
    @PostMapping
    public ResponseEntity<LocationResponse> resolve(@Valid @RequestBody LocationRequest request) {
        return ResponseEntity.ok(locationService.resolve(request.placeId()));
    }
}
