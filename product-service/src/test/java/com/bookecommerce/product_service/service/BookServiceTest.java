package com.bookecommerce.product_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bookecommerce.product_service.dto.BookCreateRequest;
import com.bookecommerce.product_service.entity.Author;
import com.bookecommerce.product_service.entity.Book;
import com.bookecommerce.product_service.entity.Category;
import com.bookecommerce.product_service.exception.ConflictException;
import com.bookecommerce.product_service.exception.ResourceNotFoundException;
import com.bookecommerce.product_service.repository.AuthorRepository;
import com.bookecommerce.product_service.repository.BookRepository;
import com.bookecommerce.product_service.repository.CategoryRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BookService bookService;

    private UUID authorId;
    private UUID categoryId;
    private Author author;
    private Category category;

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        author = new Author();
        author.setId(authorId);
        category = new Category();
        category.setId(categoryId);
    }

    @Test
    void createBookWithValidReferencesReturnsDto() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            book.setId(UUID.randomUUID());
            return book;
        });

        var response = bookService.create(request());

        assertThat(response.id()).isNotNull();
        assertThat(response.authorId()).isEqualTo(authorId);
        assertThat(response.categoryId()).isEqualTo(categoryId);
    }

    @Test
    void missingAuthorIsRejected() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.create(request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Author not found");
    }

    @Test
    void missingCategoryIsRejected() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.create(request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    void duplicateIsbnIsRejected() {
        when(bookRepository.existsByIsbn("9780134494166")).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(request()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A book with this ISBN already exists");
    }

    @Test
    void getBookNotFoundIsRejected() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book not found");
    }

    private BookCreateRequest request() {
        return new BookCreateRequest("9780134494166", "Clean Architecture", "Description",
                authorId, categoryId, new BigDecimal("599.00"));
    }
}