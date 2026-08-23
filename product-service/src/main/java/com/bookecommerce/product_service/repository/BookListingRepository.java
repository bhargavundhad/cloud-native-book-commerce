package com.bookecommerce.product_service.repository;

import com.bookecommerce.product_service.entity.BookListing;
import com.bookecommerce.product_service.entity.ListingStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookListingRepository extends JpaRepository<BookListing, UUID> {
	boolean existsByBookId(UUID bookId);

	Page<BookListing> findByStatus(ListingStatus status, Pageable pageable);

	List<BookListing> findByBookId(UUID bookId);

	List<BookListing> findByOwnerId(UUID ownerId);
}