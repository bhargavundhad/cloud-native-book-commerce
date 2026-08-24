package com.bookecommerce.product_service.service;

import com.bookecommerce.product_service.dto.CategoryCreateRequest;
import com.bookecommerce.product_service.dto.CategoryResponse;
import com.bookecommerce.product_service.dto.CategoryUpdateRequest;
import com.bookecommerce.product_service.entity.Category;
import com.bookecommerce.product_service.exception.ConflictException;
import com.bookecommerce.product_service.exception.ResourceNotFoundException;
import com.bookecommerce.product_service.repository.BookRepository;
import com.bookecommerce.product_service.repository.CategoryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    public CategoryService(CategoryRepository categoryRepository, BookRepository bookRepository) {
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
    }

    public CategoryResponse create(CategoryCreateRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw duplicateName();
        }
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id) {
        return toResponse(getCategory(id));
    }

    public CategoryResponse update(UUID id, CategoryUpdateRequest request) {
        Category category = getCategory(id);
        if (categoryRepository.existsByNameAndIdNot(request.name(), id)) {
            throw duplicateName();
        }
        category.setName(request.name());
        category.setDescription(request.description());
        return toResponse(categoryRepository.save(category));
    }

    public void delete(UUID id) {
        Category category = getCategory(id);
        if (bookRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category cannot be deleted while books reference it", "CATEGORY_IN_USE");
        }
        try {
            categoryRepository.delete(category);
            categoryRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Category cannot be deleted while books reference it", "CATEGORY_IN_USE");
        }
    }

    private Category getCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found", "CATEGORY_NOT_FOUND"));
    }

    private ConflictException duplicateName() {
        return new ConflictException("A category with this name already exists", "CATEGORY_NAME_EXISTS");
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription(), category.getCreatedAt());
    }
}