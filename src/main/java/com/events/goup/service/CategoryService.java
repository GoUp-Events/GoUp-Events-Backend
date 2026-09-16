package com.events.goup.service;

import com.events.goup.dto.CategoryRequest;
import com.events.goup.dto.CategoryResponse;
import com.events.goup.entity.Category;
import com.events.goup.exception.DuplicateNameException;
import com.events.goup.exception.NotFoundException;
import com.events.goup.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        List<Category> categories = categoryRepository.findAll();
        List<CategoryResponse> responses = new ArrayList<>();

        for (Category category : categories) {
            responses.add(toResponse(category));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        Category category = findEntityById(id);
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = normalizeName(request.name());

        if (categoryRepository.existsByName(name)) {
            throw new DuplicateNameException("Já existe uma categoria com o nome: " + name);
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(request.description());

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findEntityById(id);
        String name = normalizeName(request.name());

        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new DuplicateNameException("Já existe uma categoria com o nome: " + name);
        }

        category.setName(name);
        category.setDescription(request.description());

        Category updated = categoryRepository.save(category);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findEntityById(id);
        categoryRepository.delete(category);
    }

    private Category findEntityById(Long id) {
        Category category = categoryRepository.findById(id).orElse(null);

        if (category == null) {
            throw new NotFoundException("Categoria não encontrada com o id: " + id);
        }

        return category;
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome da categoria é obrigatório");
        }
        return name.trim();
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
