package com.bookecommerce.product_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookecommerce.product_service.dto.BookListingCreateRequest;
import com.bookecommerce.product_service.dto.BookListingUpdateRequest;
import com.bookecommerce.product_service.entity.Book;
import com.bookecommerce.product_service.entity.BookCondition;
import com.bookecommerce.product_service.entity.BookListing;
import com.bookecommerce.product_service.entity.LoanStatus;
import com.bookecommerce.product_service.exception.ConflictException;
import com.bookecommerce.product_service.exception.ResourceNotFoundException;
import com.bookecommerce.product_service.repository.BookListingRepository;
import com.bookecommerce.product_service.repository.BookRepository;
import com.bookecommerce.product_service.repository.LoanRepository;
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
class BookListingServiceTest {
    @Mock
    private BookListingRepository listingRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private LoanRepository loanRepository;
    @InjectMocks
    private BookListingService listingService;

    private UUID bookId;
    private UUID ownerId;
    private Book book;

    @BeforeEach
    void setUp() {
        bookId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        book = new Book();
        book.setId(bookId);
    }

    @Test
    void createValidListingUsesBookAndAvailableStatus() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(listingRepository.save(any(BookListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = listingService.create(request());

        assertThat(response.bookId()).isEqualTo(bookId);
        assertThat(response.status().name()).isEqualTo("AVAILABLE");
    }

    @Test
    void createForMissingBookIsRejected() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listingService.create(request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book not found");
    }

    @Test
    void getListingReturnsDto() {
        UUID id = UUID.randomUUID();
        BookListing listing = listing(id);
        when(listingRepository.findById(id)).thenReturn(Optional.of(listing));

        assertThat(listingService.findById(id).id()).isEqualTo(id);
    }

    @Test
    void updateDoesNotChangeBookOrOwner() {
        UUID id = UUID.randomUUID();
        BookListing listing = listing(id);
        when(listingRepository.findById(id)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(BookListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listingService.update(id, new BookListingUpdateRequest(BookCondition.LIKE_NEW, new BigDecimal("120.00"), 20, "Updated"));

        assertThat(listing.getBook().getId()).isEqualTo(bookId);
        assertThat(listing.getOwnerId()).isEqualTo(ownerId);
        assertThat(listing.getBorrowFee()).isEqualByComparingTo("120.00");
    }

    @Test
    void deletionWithActiveLoanIsRejected() {
        UUID id = UUID.randomUUID();
        when(listingRepository.findById(id)).thenReturn(Optional.of(listing(id)));
        when(loanRepository.existsByListingIdAndStatus(id, LoanStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> listingService.delete(id))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Listing cannot be deleted while it has an active loan");
    }

    @Test
    void deletionWithoutLoanFlushesRepository() {
        UUID id = UUID.randomUUID();
        when(listingRepository.findById(id)).thenReturn(Optional.of(listing(id)));
        when(loanRepository.existsByListingIdAndStatus(id, LoanStatus.ACTIVE)).thenReturn(false);

        listingService.delete(id);

        verify(listingRepository).flush();
    }

    private BookListingCreateRequest request() {
        return new BookListingCreateRequest(bookId, ownerId, BookCondition.GOOD, new BigDecimal("100.00"), 15, "Good");
    }

    private BookListing listing(UUID id) {
        BookListing listing = new BookListing();
        listing.setId(id);
        listing.setBook(book);
        listing.setOwnerId(ownerId);
        listing.setCondition(BookCondition.GOOD);
        listing.setBorrowFee(new BigDecimal("100.00"));
        listing.setBorrowDurationDays(15);
        return listing;
    }
}