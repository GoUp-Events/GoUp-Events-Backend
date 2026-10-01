package com.events.goup.service;

import com.events.goup.client.GooglePlacesClient;
import com.events.goup.client.GooglePlacesClient.PlaceDetails;
import com.events.goup.dto.location.LocationResponse;
import com.events.goup.entity.Location;
import com.events.goup.entity.enums.PriceLevel;
import com.events.goup.exception.NotFoundException;
import com.events.goup.mapper.LocationMapper;
import com.events.goup.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final GooglePlacesClient googlePlacesClient;

    @Transactional(readOnly = true)
    public List<LocationResponse> findAll() {
        return locationRepository.findAll()
                .stream()
                .map(LocationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationResponse findById(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Local não encontrado com o id: " + id));
        return LocationMapper.toResponse(location);
    }

    @Transactional
    public LocationResponse resolve(String placeId) {
        return LocationMapper.toResponse(findOrCreateByPlaceId(placeId));
    }

    /**
     * Reaproveita o Location se o placeId já existe; senão busca o Place Details no Google e salva.
     */
    @Transactional
    public Location findOrCreateByPlaceId(String placeId) {
        String normalizedPlaceId = placeId.trim();

        return locationRepository.findByPlaceId(normalizedPlaceId)
                .orElseGet(() -> locationRepository.save(fromPlaceDetails(
                        normalizedPlaceId, googlePlacesClient.getPlaceDetails(normalizedPlaceId))));
    }

    private Location fromPlaceDetails(String placeId, PlaceDetails details) {
        Location location = new Location();
        location.setPlaceId(placeId);

        String name = details.displayName() != null ? details.displayName().text() : null;
        location.setName(truncate(name != null ? name : details.formattedAddress(), 150));
        location.setFormattedAddress(truncate(details.formattedAddress(), 255));
        location.setAddress(truncate(details.component("route", false), 200));
        location.setNumber(truncate(details.component("street_number", false), 20));
        location.setNeighborhood(truncate(firstNonNull(
                details.component("sublocality_level_1", false),
                details.component("sublocality", false)), 100));
        location.setCity(truncate(firstNonNull(
                details.component("locality", false),
                details.component("administrative_area_level_2", false)), 100));
        location.setState(truncate(details.component("administrative_area_level_1", true), 2));
        location.setZip(truncate(details.component("postal_code", false), 9));
        location.setLatitude(details.location().latitude());
        location.setLongitude(details.location().longitude());
        location.setRating(details.rating());
        location.setPriceLevel(PriceLevel.fromGoogle(details.priceLevel()));

        return location;
    }

    private String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}
