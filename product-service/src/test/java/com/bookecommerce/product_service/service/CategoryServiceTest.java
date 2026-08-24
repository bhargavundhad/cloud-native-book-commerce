package com.bookecommerce.product_service.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bookecommerce.product_service.dto.CategoryCreateRequest;
import com.bookecommerce.product_service.exception.ConflictException;
import com.bookecommerce.product_service.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void duplicateCategoryNameIsRejected() {
        when(categoryRepository.existsByName("Programming")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CategoryCreateRequest("Programming", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A category with this name already exists");
    }
}