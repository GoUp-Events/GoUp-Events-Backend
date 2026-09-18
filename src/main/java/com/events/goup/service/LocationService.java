package com.events.goup.service;

import com.events.goup.dto.location.LocationRequest;
import com.events.goup.dto.location.LocationResponse;
import com.events.goup.entity.Location;
import com.events.goup.exception.NotFoundException;
import com.events.goup.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    @Transactional(readOnly = true)
    public List<LocationResponse> findAll() {
        return locationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public LocationResponse create(LocationRequest request) {
        Location location = new Location();
        applyRequest(location, request);

        return toResponse(locationRepository.save(location));
    }

    @Transactional
    public LocationResponse update(Long id, LocationRequest request) {
        Location location = findEntityById(id);
        applyRequest(location, request);

        return toResponse(locationRepository.save(location));
    }

    @Transactional
    public void delete(Long id) {
        Location location = findEntityById(id);
        locationRepository.delete(location);
    }

    private Location findEntityById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Local não encontrado com o id: " + id));
    }

    private void applyRequest(Location location, LocationRequest request) {
        location.setName(requireField(request.name(), "name"));
        location.setAddress(requireField(request.address(), "address"));
        location.setCity(requireField(request.city(), "city"));
        location.setState(requireField(request.state(), "state"));
        location.setZip(requireField(request.zip(), "zip"));

        location.setNumber(normalizeOptional(request.number()));
        location.setNeighborhood(normalizeOptional(request.neighborhood()));
    }

    private String requireField(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("O campo " + fieldName + " é obrigatório");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getAddress(),
                location.getNumber(),
                location.getNeighborhood(),
                location.getCity(),
                location.getState(),
                location.getZip()
        );
    }
}