package com.events.goup;

import com.events.goup.entity.Category;
import com.events.goup.entity.Event;
import com.events.goup.entity.Location;
import com.events.goup.entity.User;
import com.events.goup.entity.enums.AgeRating;
import com.events.goup.entity.enums.EventStatus;
import com.events.goup.entity.enums.Role;
import com.events.goup.repository.CategoryRepository;
import com.events.goup.repository.EventRepository;
import com.events.goup.repository.FavoriteRepository;
import com.events.goup.repository.LocationRepository;
import com.events.goup.repository.UserRepository;
import com.events.goup.security.JwtService;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Base dos testes de integração: sobe a aplicação inteira com H2 em memória, Spring Security e JWT reais.
 * Cada teste roda numa transação que é desfeita no final, então um teste não suja o outro.
 * As categorias do CategorySeeder existem em todos os testes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {

    protected static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    protected static final String PASSWORD = "senha123";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected EntityManager entityManager;
    @Autowired protected UserRepository userRepository;
    @Autowired protected LocationRepository locationRepository;
    @Autowired protected EventRepository eventRepository;
    @Autowired protected CategoryRepository categoryRepository;
    @Autowired protected FavoriteRepository favoriteRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;

    /**
     * Grava tudo no banco e limpa o cache do Hibernate antes da requisição,
     * para a API ler do banco como acontece em produção.
     */
    protected ResultActions perform(RequestBuilder requestBuilder) throws Exception {
        entityManager.flush();
        entityManager.clear();
        return mockMvc.perform(requestBuilder);
    }

    protected LocalDate daysFromNow(int days) {
        return LocalDate.now(ZONE).plusDays(days);
    }

    protected User createUser(String email, boolean premium) {
        return createUser(email, premium, Role.USER);
    }

    protected User createAdmin(String email) {
        return createUser(email, false, Role.ADMIN);
    }

    protected User createUser(String email, boolean premium, Role role) {
        User user = new User("Usuário " + email, email, passwordEncoder.encode(PASSWORD), role);
        user.setPremium(premium);
        return userRepository.save(user);
    }

    protected String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getEmail(), user.getRole().name());
    }

    protected Category categoryAt(int index) {
        return categoryRepository.findAll().get(index);
    }

    protected Location createLocation(String placeId, String name, String city, double latitude, double longitude) {
        Location location = new Location();
        location.setPlaceId(placeId);
        location.setName(name);
        location.setFormattedAddress(name + ", " + city);
        location.setCity(city);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return locationRepository.save(location);
    }

    protected Event createEvent(User owner, Location location, Category category,
                                EventStatus status, LocalDate date, String title) {
        Event event = new Event(title, "Descrição de " + title, date, LocalTime.of(20, 0), null,
                BigDecimal.valueOf(30), status, AgeRating.FREE, owner, location, category);
        return eventRepository.save(event);
    }

    protected Event createPublishedEvent(User owner, Location location, String title, int daysAhead) {
        return createEvent(owner, location, null, EventStatus.PUBLISHED, daysFromNow(daysAhead), title);
    }
}