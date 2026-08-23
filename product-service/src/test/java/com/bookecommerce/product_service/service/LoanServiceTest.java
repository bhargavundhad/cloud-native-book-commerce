package com.bookecommerce.product_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookecommerce.product_service.dto.LoanCreateRequest;
import com.bookecommerce.product_service.entity.Book;
import com.bookecommerce.product_service.entity.BookCondition;
import com.bookecommerce.product_service.entity.BookListing;
import com.bookecommerce.product_service.entity.ListingStatus;
import com.bookecommerce.product_service.entity.Loan;
import com.bookecommerce.product_service.entity.LoanStatus;
import com.bookecommerce.product_service.exception.ConflictException;
import com.bookecommerce.product_service.exception.InvalidLoanStateException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.bookecommerce.product_service.repository.BookListingRepository;
import com.bookecommerce.product_service.repository.LoanRepository;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private BookListingRepository listingRepository;
    @InjectMocks
    private LoanService loanService;

    private UUID listingId;
    private UUID ownerId;
    private UUID borrowerId;
    private BookListing listing;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        borrowerId = UUID.randomUUID();
        Book book = new Book();
        book.setId(UUID.randomUUID());
        listing = new BookListing();
        listing.setId(listingId);
        listing.setBook(book);
        listing.setOwnerId(ownerId);
        listing.setCondition(BookCondition.GOOD);
        listing.setBorrowFee(new BigDecimal("100.00"));
        listing.setBorrowDurationDays(15);
        listing.setStatus(ListingStatus.AVAILABLE);
    }

    @Test
    void createLoanSnapshotsListingFee() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            loan.setId(UUID.randomUUID());
            return loan;
        });

        var response = loanService.create(new LoanCreateRequest(listingId, borrowerId));

        assertThat(response.borrowFee()).isEqualByComparingTo("100.00");
        assertThat(response.status()).isEqualTo(LoanStatus.REQUESTED);
    }

    @Test
    void unavailableListingIsRejected() {
        listing.setStatus(ListingStatus.BORROWED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> loanService.create(new LoanCreateRequest(listingId, borrowerId)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Listing is not available for borrowing");
    }

    @Test
    void ownerCannotBorrowOwnListing() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> loanService.create(new LoanCreateRequest(listingId, ownerId)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("The listing owner cannot borrow their own book");
    }

    @Test
    void activateSetsDatesAndBorrowsListing() {
        Loan loan = loan(LoanStatus.REQUESTED);
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = loanService.activate(loan.getId());

        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(response.startDate()).isNotNull();
        assertThat(response.dueDate()).isEqualTo(response.startDate().plusDays(15));
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.BORROWED);
        verify(listingRepository).save(listing);
    }

    @Test
    void returnActiveLoanMakesListingAvailable() {
        Loan loan = loan(LoanStatus.ACTIVE);
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = loanService.returnLoan(loan.getId());

        assertThat(response.status()).isEqualTo(LoanStatus.RETURNED);
        assertThat(response.returnedAt()).isNotNull();
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.AVAILABLE);
    }

    @Test
    void returnedLoanCannotBeReturnedAgain() {
        Loan loan = loan(LoanStatus.RETURNED);
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.returnLoan(loan.getId()))
                .isInstanceOf(InvalidLoanStateException.class)
                .hasMessage("Loan has already been returned");
    }

    @Test
    void requestedLoanCanBeCancelled() {
        Loan loan = loan(LoanStatus.REQUESTED);
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(loanService.cancel(loan.getId()).status()).isEqualTo(LoanStatus.CANCELLED);
    }

    @Test
    void activeLoanCannotBeCancelled() {
        Loan loan = loan(LoanStatus.ACTIVE);
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.cancel(loan.getId()))
                .isInstanceOf(InvalidLoanStateException.class)
                .hasMessage("Only requested loans can be cancelled");
    }

    private Loan loan(LoanStatus status) {
        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setListing(listing);
        loan.setBorrowerId(borrowerId);
        loan.setBorrowFee(listing.getBorrowFee());
        loan.setStatus(status);
        return loan;
    }
}