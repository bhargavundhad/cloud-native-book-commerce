package com.bookecommerce.product_service.controller;

import com.bookecommerce.product_service.dto.BookListingCreateRequest;
import com.bookecommerce.product_service.dto.BookListingResponse;
import com.bookecommerce.product_service.dto.BookListingUpdateRequest;
import com.bookecommerce.product_service.service.BookListingService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/book-listings")
public class BookListingController {
    private final BookListingService listingService;

    public BookListingController(BookListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    public ResponseEntity<BookListingResponse> create(@Valid @RequestBody BookListingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(listingService.create(request));
    }

    @GetMapping
    public Page<BookListingResponse> findAvailable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return listingService.findAvailable(pageable);
    }

    @GetMapping("/{id}")
    public BookListingResponse findById(@PathVariable UUID id) {
        return listingService.findById(id);
    }

    @GetMapping("/book/{bookId}")
    public List<BookListingResponse> findByBook(@PathVariable UUID bookId) {
        return listingService.findByBook(bookId);
    }

    @GetMapping("/owner/{ownerId}")
    public List<BookListingResponse> findByOwner(@PathVariable UUID ownerId) {
        return listingService.findByOwner(ownerId);
    }

    @PutMapping("/{id}")
    public BookListingResponse update(@PathVariable UUID id, @Valid @RequestBody BookListingUpdateRequest request) {
        return listingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        listingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}