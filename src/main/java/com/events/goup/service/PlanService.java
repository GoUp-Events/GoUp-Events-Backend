package com.events.goup.service;

import com.events.goup.dto.plan.PlanResponse;
import com.events.goup.entity.User;
import com.events.goup.exception.ForbiddenException;
import com.events.goup.exception.NotFoundException;
import com.events.goup.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de acesso Free x Premium. O plano é só o atributo {@code premium} do usuário:
 * não existe entidade nem tabela de plano.
 * <p>
 * O valor é sempre lido do banco a partir do usuário autenticado (e-mail do JWT),
 * nunca de algo enviado pelo frontend. Assim, alterar {@code premium} no banco vale
 * já na próxima requisição, sem precisar gerar outro token.
 */
@Service
public class PlanService {

    public static final String PREMIUM_REQUIRED_MESSAGE = "Recurso exclusivo do GoUp Premium";

    private final UserRepository userRepository;
    private final int freeNearbyLimit;
    private final int premiumNearbyLimit;

    public PlanService(UserRepository userRepository,
                       @Value("${goup.plans.free.nearby-limit:3}") int freeNearbyLimit,
                       @Value("${goup.plans.premium.nearby-limit:10}") int premiumNearbyLimit) {
        this.userRepository = userRepository;
        this.freeNearbyLimit = freeNearbyLimit;
        this.premiumNearbyLimit = premiumNearbyLimit;
    }

    /**
     * Visitante (sem login) segue as regras do Free.
     */
    @Transactional(readOnly = true)
    public boolean isPremium(String authenticatedEmail) {
        if (authenticatedEmail == null) {
            return false;
        }
        return userRepository.findByEmail(authenticatedEmail)
                .map(User::isPremium)
                .orElse(false);
    }

    /**
     * Garante que o usuário autenticado tem premium = true; caso contrário responde 403.
     */
    @Transactional(readOnly = true)
    public User requirePremium(String authenticatedEmail) {
        if (authenticatedEmail == null) {
            throw new ForbiddenException(PREMIUM_REQUIRED_MESSAGE);
        }
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + authenticatedEmail));

        if (!user.isPremium()) {
            throw new ForbiddenException(PREMIUM_REQUIRED_MESSAGE);
        }
        return user;
    }

    public int nearbyLimit(boolean premium) {
        return premium ? premiumNearbyLimit : freeNearbyLimit;
    }

    public int maxNearbyLimit() {
        return Math.max(freeNearbyLimit, premiumNearbyLimit);
    }

    /**
     * Conteúdo da tela "Evoluir plano". Os limites vêm da mesma configuração usada nas regras.
     */
    public List<PlanResponse> findAll() {
        return List.of(
                new PlanResponse("Free", false, freeNearbyLimit, List.of(
                        "Descoberta e pesquisa de eventos",
                        "Filtros e informações dos eventos",
                        "Até " + freeNearbyLimit + " estabelecimentos próximos do evento",
                        "Distância, preço e avaliação dos estabelecimentos")),
                new PlanResponse("Premium", true, premiumNearbyLimit, List.of(
                        "Tudo do plano Free",
                        "Chatbot com IA para pedidos em linguagem natural",
                        "Até " + premiumNearbyLimit + " estabelecimentos próximos do evento",
                        "Recomendações personalizadas",
                        "Recursos ampliados de descoberta: eventos em alta e próximos por tipo de lugar"))
        );
    }
}
