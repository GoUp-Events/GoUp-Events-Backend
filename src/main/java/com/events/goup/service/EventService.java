package com.events.goup.service;

import com.events.goup.client.GooglePlacesClient;
import com.events.goup.client.GooglePlacesClient.Place;
import com.events.goup.dto.event.EventRequest;
import com.events.goup.dto.event.EventResponse;
import com.events.goup.dto.place.NearbyPlaceResponse;
import com.events.goup.entity.Category;
import com.events.goup.entity.Event;
import com.events.goup.entity.Location;
import com.events.goup.entity.User;
import com.events.goup.entity.enums.EventStatus;
import com.events.goup.entity.enums.Role;
import com.events.goup.exception.ForbiddenException;
import com.events.goup.exception.NotFoundException;
import com.events.goup.mapper.EventMapper;
import com.events.goup.repository.CategoryRepository;
import com.events.goup.repository.EventRepository;
import com.events.goup.repository.FavoriteRepository;
import com.events.goup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    // Limite proposital para economizar a cota gratuita do Google Places.
    private static final int NEARBY_LIMIT = 3;

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final LocationService locationService;
    private final GooglePlacesClient googlePlacesClient;

    /**
     * Lista pública: rascunhos (DRAFT) não aparecem. O dono vê os próprios em /users/me/events.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> findAll() {
        return eventRepository.findAllExceptStatus(EventStatus.DRAFT)
                .stream()
                .map(EventMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findAllByOwner(String authenticatedEmail) {
        return eventRepository.findAllByOwner(findUserByEmail(authenticatedEmail))
                .stream()
                .map(EventMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse findById(Long id, String authenticatedEmail) {
        return EventMapper.toResponse(findVisibleEntity(id, authenticatedEmail));
    }

    @Transactional(readOnly = true)
    public List<NearbyPlaceResponse> findNearbyPlaces(Long id, String authenticatedEmail) {
        Location location = findVisibleEntity(id, authenticatedEmail).getLocation();

        // Pede um a mais porque o próprio local do evento costuma vir como o mais próximo.
        List<Place> places = googlePlacesClient.searchNearby(
                location.getPlaceId(), location.getLatitude(), location.getLongitude(), NEARBY_LIMIT + 1);

        return places.stream()
                .filter(place -> !location.getPlaceId().equals(place.id()))
                .limit(NEARBY_LIMIT)
                .map(place -> toNearbyResponse(place, location))
                .toList();
    }

    @Transactional
    public EventResponse create(EventRequest request, String authenticatedEmail) {
        validateTimes(request);

        User owner = findUserByEmail(authenticatedEmail);
        Category category = findCategoryById(request.categoryId());
        Location location = locationService.findOrCreateByPlaceId(request.placeId());

        Event event = new Event();
        applyRequest(event, request, location, category);
        event.setUser(owner);
        event.setStatus(request.status() != null ? request.status() : EventStatus.DRAFT);

        return EventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse update(Long id, EventRequest request, String authenticatedEmail) {
        validateTimes(request);

        Event event = findEntityById(id);
        User requester = findUserByEmail(authenticatedEmail);
        checkOwnership(event, requester);

        Category category = findCategoryById(request.categoryId());
        Location location = locationService.findOrCreateByPlaceId(request.placeId());

        applyRequest(event, request, location, category);
        event.setStatus(request.status() != null ? request.status() : event.getStatus());

        return EventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional
    public void delete(Long id, String authenticatedEmail) {
        Event event = findEntityById(id);
        User requester = findUserByEmail(authenticatedEmail);
        checkOwnership(event, requester);

        favoriteRepository.deleteAllByEvent(event);
        eventRepository.delete(event);
    }

    /**
     * Busca o evento respeitando a visibilidade: DRAFT só é visível para o dono ou ADMIN.
     * Para os demais responde 404, sem revelar que o rascunho existe.
     */
    @Transactional(readOnly = true)
    public Event findVisibleEntity(Long id, String authenticatedEmail) {
        Event event = findEntityById(id);

        if (event.getStatus() == EventStatus.DRAFT) {
            User requester = authenticatedEmail != null
                    ? userRepository.findByEmail(authenticatedEmail).orElse(null)
                    : null;
            if (requester == null || !canManage(event, requester)) {
                throw new NotFoundException("Evento não encontrado com o id: " + id);
            }
        }
        return event;
    }

    private void checkOwnership(Event event, User requester) {
        if (!canManage(event, requester)) {
            throw new ForbiddenException("Você não tem permissão para alterar este evento");
        }
    }

    private boolean canManage(Event event, User requester) {
        boolean isOwner = event.getUser().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ADMIN;
        return isOwner || isAdmin;
    }

    private void validateTimes(EventRequest request) {
        if (request.endTime() != null && !request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("O horário de término deve ser posterior ao horário de início");
        }
    }

    private void applyRequest(Event event, EventRequest request, Location location, Category category) {
        event.setTitle(request.title().trim());
        event.setDescription(request.description() != null ? request.description().trim() : null);
        event.setEventDate(request.eventDate());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setPrice(request.price());
        event.setAgeRating(request.ageRating());
        event.setLocation(location);
        event.setCategory(category);
    }

    private Event findEntityById(Long id) {
        return eventRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Evento não encontrado com o id: " + id));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + email));
    }

    private Category findCategoryById(Long id) {
        if (id == null) {
            return null;
        }
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoria não encontrada com o id: " + id));
    }

    private NearbyPlaceResponse toNearbyResponse(Place place, Location origin) {
        Double latitude = place.location() != null ? place.location().latitude() : null;
        Double longitude = place.location() != null ? place.location().longitude() : null;
        Integer distance = latitude != null && longitude != null
                ? (int) Math.round(distanceInMeters(origin.getLatitude(), origin.getLongitude(), latitude, longitude))
                : null;

        return new NearbyPlaceResponse(
                place.id(),
                place.displayName() != null ? place.displayName().text() : null,
                place.formattedAddress(),
                place.primaryTypeDisplayName() != null ? place.primaryTypeDisplayName().text() : null,
                latitude,
                longitude,
                distance,
                place.googleMapsUri()
        );
    }

    // Fórmula de Haversine.
    private double distanceInMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
