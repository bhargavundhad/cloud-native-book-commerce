package com.bookecommerce.product_service.service;

import com.bookecommerce.product_service.dto.BookListingCreateRequest;
import com.bookecommerce.product_service.dto.BookListingResponse;
import com.bookecommerce.product_service.dto.BookListingUpdateRequest;
import com.bookecommerce.product_service.entity.Book;
import com.bookecommerce.product_service.entity.BookListing;
import com.bookecommerce.product_service.entity.ListingStatus;
import com.bookecommerce.product_service.exception.ConflictException;
import com.bookecommerce.product_service.exception.ResourceNotFoundException;
import com.bookecommerce.product_service.repository.BookListingRepository;
import com.bookecommerce.product_service.repository.BookRepository;
import com.bookecommerce.product_service.repository.LoanRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookListingService {
    private final BookListingRepository listingRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public BookListingService(BookListingRepository listingRepository, BookRepository bookRepository,
            LoanRepository loanRepository) {
        this.listingRepository = listingRepository;
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    public BookListingResponse create(BookListingCreateRequest request) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found", "BOOK_NOT_FOUND"));
        BookListing listing = new BookListing();
        listing.setBook(book);
        listing.setOwnerId(request.ownerId());
        listing.setCondition(request.condition());
        listing.setBorrowFee(request.borrowFee());
        listing.setBorrowDurationDays(request.borrowDurationDays());
        listing.setDescription(request.description());
        listing.setStatus(ListingStatus.AVAILABLE);
        return toResponse(listingRepository.save(listing));
    }

    @Transactional(readOnly = true)
    public BookListingResponse findById(UUID id) {
        return toResponse(getListing(id));
    }

    @Transactional(readOnly = true)
    public Page<BookListingResponse> findAvailable(Pageable pageable) {
        return listingRepository.findByStatus(ListingStatus.AVAILABLE, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<BookListingResponse> findByBook(UUID bookId) {
        ensureBookExists(bookId);
        return listingRepository.findByBookId(bookId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BookListingResponse> findByOwner(UUID ownerId) {
        return listingRepository.findByOwnerId(ownerId).stream().map(this::toResponse).toList();
    }

    public BookListingResponse update(UUID id, BookListingUpdateRequest request) {
        BookListing listing = getListing(id);
        listing.setCondition(request.condition());
        listing.setBorrowFee(request.borrowFee());
        listing.setBorrowDurationDays(request.borrowDurationDays());
        listing.setDescription(request.description());
        return toResponse(listingRepository.save(listing));
    }

    public void delete(UUID id) {
        BookListing listing = getListing(id);
        if (loanRepository.existsByListingIdAndStatus(id, com.bookecommerce.product_service.entity.LoanStatus.ACTIVE)) {
            throw new ConflictException("Listing cannot be deleted while it has an active loan", "LISTING_HAS_ACTIVE_LOAN");
        }
        try {
            listingRepository.delete(listing);
            listingRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Listing cannot be deleted while loan records reference it", "LISTING_HAS_LOANS");
        }
    }

    private BookListing getListing(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book listing not found", "LISTING_NOT_FOUND"));
    }

    private void ensureBookExists(UUID bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book not found", "BOOK_NOT_FOUND");
        }
    }

    private BookListingResponse toResponse(BookListing listing) {
        return new BookListingResponse(listing.getId(), listing.getBook().getId(), listing.getOwnerId(),
                listing.getCondition(), listing.getBorrowFee(), listing.getBorrowDurationDays(), listing.getStatus(),
                listing.getDescription(), listing.getCreatedAt(), listing.getUpdatedAt());
    }
}