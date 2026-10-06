package com.events.goup.controller;

import com.events.goup.dto.event.EventFilter;
import com.events.goup.dto.event.EventRequest;
import com.events.goup.dto.event.EventResponse;
import com.events.goup.dto.place.NearbyPlaceResponse;
import com.events.goup.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventResponse>> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            Authentication authentication) {
        EventFilter filter = new EventFilter(q, city, categoryId, dateFrom, dateTo, maxPrice, sort);
        return ResponseEntity.ok(eventService.findAll(filter, emailOf(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> findById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(eventService.findById(id, emailOf(authentication)));
    }

    @GetMapping("/{id}/nearby")
    public ResponseEntity<List<NearbyPlaceResponse>> findNearbyPlaces(@PathVariable Long id,
                                                                      @RequestParam(required = false) String type,
                                                                      Authentication authentication) {
        return ResponseEntity.ok(eventService.findNearbyPlaces(id, type, emailOf(authentication)));
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request,
                                                Authentication authentication) {
        EventResponse response = eventService.create(request, authentication.getName());
        return ResponseEntity.created(URI.create("/events/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody EventRequest request,
                                                Authentication authentication) {
        return ResponseEntity.ok(eventService.update(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        eventService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // Rotas públicas: o visitante chega sem Authentication.
    private String emailOf(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
