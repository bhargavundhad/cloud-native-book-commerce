package com.bookecommerce.product_service.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookecommerce.product_service.dto.BookResponse;
import com.bookecommerce.product_service.exception.GlobalExceptionHandler;
import com.bookecommerce.product_service.exception.ResourceNotFoundException;
import com.bookecommerce.product_service.service.BookService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BookControllerReadinessTest {

    private MockMvc mockMvc;

    @Mock
    private BookService bookService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new BookController(bookService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getBookByIdReturnsRequiredProductInformation() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID authorId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID categoryId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        BookResponse response = new BookResponse(
                id,
                "9780134494166",
                "Clean Architecture",
                "A practical software design book",
                authorId,
                categoryId,
                new BigDecimal("599.00"),
                LocalDateTime.of(2024, 1, 1, 10, 0),
                LocalDateTime.of(2024, 1, 2, 10, 0));

        when(bookService.findById(id)).thenReturn(response);

        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.isbn").value("9780134494166"))
                .andExpect(jsonPath("$.title").value("Clean Architecture"))
                .andExpect(jsonPath("$.description").value("A practical software design book"))
                .andExpect(jsonPath("$.authorId").value(authorId.toString()))
                .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
                .andExpect(jsonPath("$.price").value(599.00));
    }

    @Test
    void getBookByUnknownIdReturnsNotFoundError() throws Exception {
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");

        when(bookService.findById(id))
                .thenThrow(new ResourceNotFoundException("Book not found", "BOOK_NOT_FOUND"));

        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Book not found"))
                .andExpect(jsonPath("$.errorCode").value("BOOK_NOT_FOUND"));
    }

    @Test
    void getBookWithInvalidUuidReturnsInvalidUuidError() throws Exception {
        mockMvc.perform(get("/api/books/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_UUID"));
    }
}
