package com.bookecommerce.product_service.service;

import com.bookecommerce.product_service.dto.BookCreateRequest;
import com.bookecommerce.product_service.dto.BookResponse;
import com.bookecommerce.product_service.dto.BookUpdateRequest;
import com.bookecommerce.product_service.entity.Author;
import com.bookecommerce.product_service.entity.Book;
import com.bookecommerce.product_service.entity.Category;
import com.bookecommerce.product_service.exception.ConflictException;
import com.bookecommerce.product_service.exception.ResourceNotFoundException;
import com.bookecommerce.product_service.repository.AuthorRepository;
import com.bookecommerce.product_service.repository.BookListingRepository;
import com.bookecommerce.product_service.repository.BookRepository;
import com.bookecommerce.product_service.repository.CategoryRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookService {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final BookListingRepository bookListingRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository,
            CategoryRepository categoryRepository, BookListingRepository bookListingRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.bookListingRepository = bookListingRepository;
    }

    public BookResponse create(BookCreateRequest request) {
        ensureIsbnAvailable(request.isbn(), null);
        Book book = new Book();
        apply(book, request.isbn(), request.title(), request.description(), request.authorId(), request.categoryId(), request.price());
        return toResponse(bookRepository.save(book));
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> findAll(UUID authorId, UUID categoryId, Pageable pageable) {
        Page<Book> books;
        if (authorId != null && categoryId != null) {
            books = bookRepository.findByAuthorIdAndCategoryId(authorId, categoryId, pageable);
        } else if (authorId != null) {
            books = bookRepository.findByAuthorId(authorId, pageable);
        } else if (categoryId != null) {
            books = bookRepository.findByCategoryId(categoryId, pageable);
        } else {
            books = bookRepository.findAll(pageable);
        }
        return books.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BookResponse findById(UUID id) {
        return toResponse(getBook(id));
    }

    public BookResponse update(UUID id, BookUpdateRequest request) {
        Book book = getBook(id);
        ensureIsbnAvailable(request.isbn(), id);
        apply(book, request.isbn(), request.title(), request.description(), request.authorId(), request.categoryId(), request.price());
        return toResponse(bookRepository.save(book));
    }

    public void delete(UUID id) {
        Book book = getBook(id);
        if (bookListingRepository.existsByBookId(id)) {
            throw new ConflictException("Book cannot be deleted while listings reference it", "BOOK_IN_USE");
        }
        try {
            bookRepository.delete(book);
            bookRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Book cannot be deleted while related records reference it", "BOOK_IN_USE");
        }
    }

    private void apply(Book book, String isbn, String title, String description, UUID authorId,
            UUID categoryId, java.math.BigDecimal price) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found", "AUTHOR_NOT_FOUND"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found", "CATEGORY_NOT_FOUND"));
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setDescription(description);
        book.setAuthor(author);
        book.setCategory(category);
        book.setPrice(price);
    }

    private void ensureIsbnAvailable(String isbn, UUID id) {
        if (id == null ? bookRepository.existsByIsbn(isbn) : bookRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw new ConflictException("A book with this ISBN already exists", "ISBN_EXISTS");
        }
    }

    private Book getBook(UUID id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found", "BOOK_NOT_FOUND"));
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getDescription(),
                book.getAuthor().getId(), book.getCategory().getId(), book.getPrice(), book.getCreatedAt(), book.getUpdatedAt());
    }
}