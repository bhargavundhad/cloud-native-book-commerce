package com.bookecommerce.product_service.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bookecommerce.product_service.entity.Book;

public interface BookRepository extends JpaRepository<Book, UUID> {
	boolean existsByIsbn(String isbn);

	boolean existsByIsbnAndIdNot(String isbn, UUID id);

	boolean existsByAuthorId(UUID authorId);

	boolean existsByCategoryId(UUID categoryId);

	Page<Book> findByAuthorId(UUID authorId, Pageable pageable);

	Page<Book> findByCategoryId(UUID categoryId, Pageable pageable);

	Page<Book> findByAuthorIdAndCategoryId(UUID authorId, UUID categoryId, Pageable pageable);
}