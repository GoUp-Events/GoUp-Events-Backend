package com.events.goup.service;

import com.events.goup.dto.recommendation.RecommendationResponse;
import com.events.goup.entity.Event;
import com.events.goup.entity.Favorite;
import com.events.goup.entity.User;
import com.events.goup.entity.enums.EventStatus;
import com.events.goup.mapper.EventMapper;
import com.events.goup.repository.EventRepository;
import com.events.goup.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Recomendações personalizadas (exclusivo Premium), baseadas nos favoritos do usuário:
 * eventos futuros das categorias e cidades que ele mais favorita. Sem IA, então não gasta cota.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int CANDIDATE_LIMIT = 200;
    private static final int MAX_RECOMMENDATIONS = 10;
    private static final int CATEGORY_WEIGHT = 3;
    private static final int CITY_WEIGHT = 1;

    private final EventRepository eventRepository;
    private final FavoriteRepository favoriteRepository;
    private final PlanService planService;

    @Transactional(readOnly = true)
    public List<RecommendationResponse> recommend(String authenticatedEmail) {
        User user = planService.requirePremium(authenticatedEmail);

        List<Event> favorites = favoriteRepository.findAllByUserOrderByCreatedAtAsc(user)
                .stream()
                .map(Favorite::getEvent)
                .toList();

        Set<Long> favoriteIds = favorites.stream().map(Event::getId).collect(Collectors.toSet());
        Map<String, Long> categoryCount = countBy(favorites,
                event -> event.getCategory() != null ? event.getCategory().getName() : null);
        Map<String, Long> cityCount = countBy(favorites, event -> event.getLocation().getCity());

        // Já favoritados e eventos do próprio usuário não fazem sentido como recomendação.
        List<Event> candidates = eventRepository.findDiscoveryCandidates(
                        EventStatus.PUBLISHED, LocalDate.now(ZONE), null, PageRequest.of(0, CANDIDATE_LIMIT))
                .stream()
                .filter(event -> !favoriteIds.contains(event.getId()))
                .filter(event -> !event.getUser().getId().equals(user.getId()))
                .toList();

        // sorted é estável: no empate, mantém a ordem por data que veio do banco.
        return candidates.stream()
                .sorted(Comparator.comparingLong((Event event) -> score(event, categoryCount, cityCount)).reversed())
                .limit(MAX_RECOMMENDATIONS)
                .map(event -> new RecommendationResponse(reason(event, categoryCount, cityCount), EventMapper.toResponse(event)))
                .toList();
    }

    private long score(Event event, Map<String, Long> categoryCount, Map<String, Long> cityCount) {
        String category = event.getCategory() != null ? event.getCategory().getName() : null;
        return CATEGORY_WEIGHT * categoryCount.getOrDefault(category, 0L)
                + CITY_WEIGHT * cityCount.getOrDefault(event.getLocation().getCity(), 0L);
    }

    private String reason(Event event, Map<String, Long> categoryCount, Map<String, Long> cityCount) {
        if (event.getCategory() != null && categoryCount.containsKey(event.getCategory().getName())) {
            return "Porque você favoritou eventos de " + event.getCategory().getName();
        }
        if (event.getLocation().getCity() != null && cityCount.containsKey(event.getLocation().getCity())) {
            return "Porque você costuma ir a eventos em " + event.getLocation().getCity();
        }
        return "Próximo evento na agenda";
    }

    private Map<String, Long> countBy(List<Event> events, Function<Event, String> key) {
        return events.stream()
                .map(key)
                .filter(value -> value != null)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }
}
