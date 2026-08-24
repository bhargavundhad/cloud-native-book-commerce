package com.bookecommerce.product_service.service;

import com.bookecommerce.product_service.dto.LoanCreateRequest;
import com.bookecommerce.product_service.dto.LoanResponse;
import com.bookecommerce.product_service.entity.BookListing;
import com.bookecommerce.product_service.entity.ListingStatus;
import com.bookecommerce.product_service.entity.Loan;
import com.bookecommerce.product_service.entity.LoanStatus;
import com.bookecommerce.product_service.exception.ConflictException;
import com.bookecommerce.product_service.exception.InvalidLoanStateException;
import com.bookecommerce.product_service.exception.ResourceNotFoundException;
import com.bookecommerce.product_service.repository.BookListingRepository;
import com.bookecommerce.product_service.repository.LoanRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoanService {
    private final LoanRepository loanRepository;
    private final BookListingRepository listingRepository;

    public LoanService(LoanRepository loanRepository, BookListingRepository listingRepository) {
        this.loanRepository = loanRepository;
        this.listingRepository = listingRepository;
    }

    public LoanResponse create(LoanCreateRequest request) {
        BookListing listing = getListing(request.listingId());
        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            throw new ConflictException("Listing is not available for borrowing", "LISTING_UNAVAILABLE");
        }
        if (listing.getOwnerId().equals(request.borrowerId())) {
            throw new ConflictException("The listing owner cannot borrow their own book", "OWNER_CANNOT_BORROW");
        }
        Loan loan = new Loan();
        loan.setListing(listing);
        loan.setBorrowerId(request.borrowerId());
        loan.setBorrowFee(listing.getBorrowFee());
        loan.setStatus(LoanStatus.REQUESTED);
        return toResponse(loanRepository.save(loan));
    }

    @Transactional(readOnly = true)
    public LoanResponse findById(UUID id) {
        return toResponse(getLoan(id));
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> findByBorrower(UUID borrowerId) {
        return loanRepository.findByBorrowerId(borrowerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> findByListing(UUID listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new ResourceNotFoundException("Book listing not found", "LISTING_NOT_FOUND");
        }
        return loanRepository.findByListingId(listingId).stream().map(this::toResponse).toList();
    }

    public LoanResponse activate(UUID id) {
        Loan loan = getLoan(id);
        if (loan.getStatus() != LoanStatus.REQUESTED) {
            throw new InvalidLoanStateException("Only requested loans can be activated");
        }
        BookListing listing = loan.getListing();
        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            throw new ConflictException("Listing is not available for activation", "LISTING_UNAVAILABLE");
        }
        LocalDateTime startDate = LocalDateTime.now();
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setStartDate(startDate);
        loan.setDueDate(startDate.plusDays(listing.getBorrowDurationDays()));
        listing.setStatus(ListingStatus.BORROWED);
        listingRepository.save(listing);
        return toResponse(loanRepository.save(loan));
    }

    public LoanResponse returnLoan(UUID id) {
        Loan loan = getLoan(id);
        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new InvalidLoanStateException("Loan has already been returned");
        }
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new InvalidLoanStateException("Only active loans can be returned");
        }
        loan.setReturnedAt(LocalDateTime.now());
        loan.setStatus(LoanStatus.RETURNED);
        loan.getListing().setStatus(ListingStatus.AVAILABLE);
        listingRepository.save(loan.getListing());
        return toResponse(loanRepository.save(loan));
    }

    public LoanResponse cancel(UUID id) {
        Loan loan = getLoan(id);
        if (loan.getStatus() != LoanStatus.REQUESTED) {
            throw new InvalidLoanStateException("Only requested loans can be cancelled");
        }
        loan.setStatus(LoanStatus.CANCELLED);
        return toResponse(loanRepository.save(loan));
    }

    private BookListing getListing(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book listing not found", "LISTING_NOT_FOUND"));
    }

    private Loan getLoan(UUID id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found", "LOAN_NOT_FOUND"));
    }

    private LoanResponse toResponse(Loan loan) {
        return new LoanResponse(loan.getId(), loan.getListing().getId(), loan.getBorrowerId(), loan.getBorrowFee(),
                loan.getStartDate(), loan.getDueDate(), loan.getReturnedAt(), loan.getStatus(), loan.getCreatedAt(), loan.getUpdatedAt());
    }
}