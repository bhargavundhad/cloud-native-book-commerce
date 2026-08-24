package com.bookecommerce.product_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookecommerce.product_service.dto.AuthorCreateRequest;
import com.bookecommerce.product_service.dto.AuthorResponse;
import com.bookecommerce.product_service.dto.AuthorUpdateRequest;
import com.bookecommerce.product_service.entity.Author;
import com.bookecommerce.product_service.exception.ConflictException;
import com.bookecommerce.product_service.exception.ResourceNotFoundException;
import com.bookecommerce.product_service.repository.AuthorRepository;
import com.bookecommerce.product_service.repository.BookRepository;

@Service
@Transactional
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    public AuthorService(AuthorRepository authorRepository, BookRepository bookRepository) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
    }

    public AuthorResponse create(AuthorCreateRequest request) {
        Author author = new Author();
        author.setName(request.name());
        author.setBiography(request.biography());
        return toResponse(authorRepository.save(author));
    }

    @Transactional(readOnly = true)
    public List<AuthorResponse> findAll() {
        return authorRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AuthorResponse findById(UUID id) {
        return toResponse(getAuthor(id));
    }

    public AuthorResponse update(UUID id, AuthorUpdateRequest request) {
        Author author = getAuthor(id);
        author.setName(request.name());
        author.setBiography(request.biography());
        return toResponse(authorRepository.save(author));
    }

    public void delete(UUID id) {
        Author author = getAuthor(id);
        if (bookRepository.existsByAuthorId(id)) {
            throw new ConflictException("Author cannot be deleted while books reference it", "AUTHOR_IN_USE");
        }
        try {
            authorRepository.delete(author);
            authorRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Author cannot be deleted while books reference it", "AUTHOR_IN_USE");
        }
    }

    private Author getAuthor(UUID id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found", "AUTHOR_NOT_FOUND"));
    }

    private AuthorResponse toResponse(Author author) {
        return new AuthorResponse(author.getId(), author.getName(), author.getBiography(), author.getCreatedAt());
    }
}