package com.bookecommerce.product_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookecommerce.product_service.entity.Author;

public interface AuthorRepository extends JpaRepository<Author, UUID> {
	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, UUID id);

}