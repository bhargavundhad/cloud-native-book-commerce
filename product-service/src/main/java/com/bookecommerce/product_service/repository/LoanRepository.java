package com.bookecommerce.product_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookecommerce.product_service.entity.Loan;
import com.bookecommerce.product_service.entity.LoanStatus;

public interface LoanRepository extends JpaRepository<Loan, UUID> {
	boolean existsByListingIdAndStatus(UUID listingId, LoanStatus status);

	boolean existsByListingId(UUID listingId);

	List<Loan> findByBorrowerId(UUID borrowerId);

	List<Loan> findByListingId(UUID listingId);
}