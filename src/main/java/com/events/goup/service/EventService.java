package com.events.goup.service;

import com.events.goup.client.GooglePlacesClient;
import com.events.goup.client.GooglePlacesClient.Place;
import com.events.goup.dto.event.EventFilter;
import com.events.goup.dto.event.EventRequest;
import com.events.goup.dto.event.EventResponse;
import com.events.goup.dto.place.NearbyPlaceResponse;
import com.events.goup.entity.Category;
import com.events.goup.entity.Event;
import com.events.goup.entity.Location;
import com.events.goup.entity.User;
import com.events.goup.entity.enums.EventStatus;
import com.events.goup.entity.enums.PriceLevel;
import com.events.goup.entity.enums.Role;
import com.events.goup.exception.ForbiddenException;
import com.events.goup.exception.NotFoundException;
import com.events.goup.mapper.EventMapper;
import com.events.goup.repository.CategoryRepository;
import com.events.goup.repository.EventRepository;
import com.events.goup.repository.FavoriteRepository;
import com.events.goup.repository.FavoriteRepository.EventFavoriteCount;
import com.events.goup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final String SORT_DATE = "date";
    private static final String SORT_POPULAR = "popular";
    // Máximo aceito pelo Nearby Search do Google.
    private static final int GOOGLE_MAX_RESULTS = 20;

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final LocationService locationService;
    private final GooglePlacesClient googlePlacesClient;
    private final PlanService planService;

    /**
     * Lista pública com pesquisa e filtros: rascunhos (DRAFT) não aparecem.
     * O dono vê os próprios em /users/me/events.
     * Mesmo fluxo para Free e Premium; só a ordenação "popular" exige premium = true.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> findAll(EventFilter filter, String authenticatedEmail) {
        String sort = filter.sort() == null || filter.sort().isBlank() ? SORT_DATE : filter.sort().trim().toLowerCase();
        if (!sort.equals(SORT_DATE) && !sort.equals(SORT_POPULAR)) {
            throw new IllegalArgumentException("Ordenação inválida: use \"" + SORT_DATE + "\" ou \"" + SORT_POPULAR + "\"");
        }
        if (sort.equals(SORT_POPULAR)) {
            planService.requirePremium(authenticatedEmail);
        }
        if (filter.dateFrom() != null && filter.dateTo() != null && filter.dateTo().isBefore(filter.dateFrom())) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial");
        }

        List<Event> events = eventRepository.search(
                EventStatus.DRAFT,
                filter.q() != null && !filter.q().isBlank() ? "%" + filter.q().trim().toLowerCase() + "%" : null,
                filter.city() != null && !filter.city().isBlank() ? filter.city().trim().toLowerCase() : null,
                filter.categoryId(),
                filter.dateFrom(),
                filter.dateTo(),
                filter.maxPrice());

        if (sort.equals(SORT_POPULAR)) {
            events = sortByFavorites(events);
        }

        return events.stream()
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

    /**
     * Mesma rota para todos: o Free (e o visitante) recebe até 3 lugares, o Premium recebe mais.
     * Filtrar por tipo de lugar (ex.: só bares) é exclusivo do Premium.
     */
    @Transactional(readOnly = true)
    public List<NearbyPlaceResponse> findNearbyPlaces(Long id, String type, String authenticatedEmail) {
        Location location = findVisibleEntity(id, authenticatedEmail).getLocation();
        boolean premium = planService.isPremium(authenticatedEmail);
        String normalizedType = normalizeNearbyType(type, premium);
        int limit = planService.nearbyLimit(premium);

        // Pede um a mais porque o próprio local do evento costuma vir como o mais próximo.
        int maxResults = Math.min(planService.maxNearbyLimit() + 1, GOOGLE_MAX_RESULTS);
        List<Place> places = googlePlacesClient.searchNearby(
                location.getPlaceId(), location.getLatitude(), location.getLongitude(), maxResults, normalizedType);

        return places.stream()
                .filter(place -> !location.getPlaceId().equals(place.id()))
                .limit(limit)
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

    private String normalizeNearbyType(String type, boolean premium) {
        if (type == null || type.isBlank()) {
            return null;
        }
        if (!premium) {
            throw new ForbiddenException(PlanService.PREMIUM_REQUIRED_MESSAGE);
        }
        String normalized = type.trim().toLowerCase();
        List<String> allowed = googlePlacesClient.getNearbyTypes();
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("Tipo de lugar inválido. Use um destes: " + String.join(", ", allowed));
        }
        return normalized;
    }

    // Mais favoritados primeiro; empate mantém a ordem por data que veio do banco.
    private List<Event> sortByFavorites(List<Event> events) {
        Map<Long, Long> totals = favoriteRepository.countGroupedByEvent()
                .stream()
                .collect(Collectors.toMap(EventFavoriteCount::getEventId, EventFavoriteCount::getTotal));

        return events.stream()
                .sorted(Comparator.comparingLong((Event event) -> totals.getOrDefault(event.getId(), 0L)).reversed())
                .toList();
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
                place.rating(),
                place.userRatingCount(),
                PriceLevel.fromGoogle(place.priceLevel()),
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

