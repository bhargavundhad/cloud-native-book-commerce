package com.bookecommerce.product_service.controller;

import com.bookecommerce.product_service.dto.BookCreateRequest;
import com.bookecommerce.product_service.dto.BookResponse;
import com.bookecommerce.product_service.dto.BookUpdateRequest;
import com.bookecommerce.product_service.service.BookService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.create(request));
    }

    @GetMapping
    public Page<BookResponse> findAll(
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("title").ascending());
        return bookService.findAll(authorId, categoryId, pageable);
    }

    @GetMapping("/{id}")
    public BookResponse findById(@PathVariable UUID id) {
        return bookService.findById(id);
    }

    @PutMapping("/{id}")
    public BookResponse update(@PathVariable UUID id, @Valid @RequestBody BookUpdateRequest request) {
        return bookService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}