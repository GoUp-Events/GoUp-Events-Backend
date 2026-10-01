package com.events.goup.config;

import com.events.goup.entity.Category;
import com.events.goup.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Categorias são uma lista fixa. Este seed insere as que ainda não existem a cada inicialização.
 */
@Component
@RequiredArgsConstructor
public class CategorySeeder implements ApplicationRunner {

    private static final List<Category> CATEGORIES = List.of(
            new Category("Shows e Música", "Shows, festivais e apresentações musicais"),
            new Category("Festas e Baladas", "Festas, baladas e eventos noturnos"),
            new Category("Teatro e Dança", "Peças, espetáculos de dança e stand-up"),
            new Category("Gastronomia", "Festivais gastronômicos, feiras de comida e degustações"),
            new Category("Cerveja", "Eventos cervejeiros, tours e degustações"),
            new Category("Cultura e Arte", "Exposições, museus, cinema e eventos culturais"),
            new Category("Esportes", "Corridas, campeonatos e atividades esportivas"),
            new Category("Ar Livre e Natureza", "Trilhas, parques e passeios ao ar livre"),
            new Category("Infantil e Família", "Programação para crianças e famílias"),
            new Category("Feiras e Exposições", "Feiras, mercados e exposições comerciais"),
            new Category("Cursos e Workshops", "Oficinas, palestras e cursos"),
            new Category("Festas Típicas", "Festas tradicionais e típicas da região")
    );

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Category category : CATEGORIES) {
            if (!categoryRepository.existsByName(category.getName())) {
                categoryRepository.save(new Category(category.getName(), category.getDescription()));
            }
        }
    }
}
