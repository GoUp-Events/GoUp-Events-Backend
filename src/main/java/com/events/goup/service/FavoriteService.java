package com.events.goup.service;

import com.events.goup.dto.favorite.FavoriteResponse;
import com.events.goup.entity.Event;
import com.events.goup.entity.Favorite;
import com.events.goup.entity.User;
import com.events.goup.exception.NotFoundException;
import com.events.goup.mapper.EventMapper;
import com.events.goup.repository.FavoriteRepository;
import com.events.goup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final EventService eventService;

    /**
     * Idempotente: favoritar um evento já favoritado não gera erro nem duplica.
     */
    @Transactional
    public void favorite(Long eventId, String authenticatedEmail) {
        User user = findUserByEmail(authenticatedEmail);
        Event event = eventService.findVisibleEntity(eventId, authenticatedEmail);

        if (!favoriteRepository.existsByUserAndEvent(user, event)) {
            favoriteRepository.save(new Favorite(user, event));
        }
    }

    @Transactional
    public void unfavorite(Long eventId, String authenticatedEmail) {
        User user = findUserByEmail(authenticatedEmail);
        Event event = eventService.findVisibleEntity(eventId, authenticatedEmail);

        favoriteRepository.findByUserAndEvent(user, event).ifPresent(favoriteRepository::delete);
    }

    /**
     * Lista de favoritos (versão simplificada do "roteiro"), na ordem em que foram adicionados.
     */
    @Transactional(readOnly = true)
    public List<FavoriteResponse> findAllByUser(String authenticatedEmail) {
        User user = findUserByEmail(authenticatedEmail);

        return favoriteRepository.findAllByUserOrderByCreatedAtAsc(user)
                .stream()
                .map(favorite -> new FavoriteResponse(favorite.getCreatedAt(), EventMapper.toResponse(favorite.getEvent())))
                .toList();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + email));
    }
}
