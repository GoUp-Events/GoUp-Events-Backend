package com.events.goup.service;

import com.events.goup.dto.category.CategoryResponse;
import com.events.goup.dto.event.EventRequest;
import com.events.goup.dto.event.EventResponse;
import com.events.goup.dto.location.LocationResponse;
import com.events.goup.dto.user.UserSummaryResponse;
import com.events.goup.entity.Category;
import com.events.goup.entity.Event;
import com.events.goup.entity.Location;
import com.events.goup.entity.User;
import com.events.goup.entity.enums.EventStatus;
import com.events.goup.entity.enums.Role;
import com.events.goup.exception.ForbiddenException;
import com.events.goup.exception.NotFoundException;
import com.events.goup.repository.CategoryRepository;
import com.events.goup.repository.EventRepository;
import com.events.goup.repository.LocationRepository;
import com.events.goup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<EventResponse> findAll() {
        return eventRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public EventResponse create(EventRequest request, String authenticatedEmail) {
        validateTimes(request);

        User owner = findUserByEmail(authenticatedEmail);
        Location location = findLocationById(request.locationId());
        Category category = findCategoryById(request.categoryId());

        Event event = new Event();
        applyRequest(event, request, location, category);
        event.setUser(owner);
        event.setStatus(request.status() != null ? request.status() : EventStatus.DRAFT);

        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse update(Long id, EventRequest request, String authenticatedEmail) {
        validateTimes(request);

        Event event = findEntityById(id);
        User requester = findUserByEmail(authenticatedEmail);
        checkOwnership(event, requester);

        Location location = findLocationById(request.locationId());
        Category category = findCategoryById(request.categoryId());

        applyRequest(event, request, location, category);
        event.setStatus(request.status() != null ? request.status() : event.getStatus());

        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public void delete(Long id, String authenticatedEmail) {
        Event event = findEntityById(id);
        User requester = findUserByEmail(authenticatedEmail);
        checkOwnership(event, requester);

        eventRepository.delete(event);
    }

    private void checkOwnership(Event event, User requester) {
        boolean isOwner = event.getUser().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Você não tem permissão para alterar este evento");
        }
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
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Evento não encontrado com o id: " + id));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + email));
    }

    private Location findLocationById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Local não encontrado com o id: " + id));
    }

    private Category findCategoryById(Long id) {
        if (id == null) {
            return null;
        }
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoria não encontrada com o id: " + id));
    }

    private EventResponse toResponse(Event event) {
        UserSummaryResponse userResponse = new UserSummaryResponse(
                event.getUser().getId(),
                event.getUser().getName()
        );

        LocationResponse locationResponse = new LocationResponse(
                event.getLocation().getId(),
                event.getLocation().getName(),
                event.getLocation().getAddress(),
                event.getLocation().getNumber(),
                event.getLocation().getNeighborhood(),
                event.getLocation().getCity(),
                event.getLocation().getState(),
                event.getLocation().getZip()
        );

        CategoryResponse categoryResponse = event.getCategory() != null
                ? new CategoryResponse(event.getCategory().getId(), event.getCategory().getName(), event.getCategory().getDescription())
                : null;

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getPrice(),
                event.getStatus(),
                event.getAgeRating(),
                userResponse,
                locationResponse,
                categoryResponse
        );
    }
}