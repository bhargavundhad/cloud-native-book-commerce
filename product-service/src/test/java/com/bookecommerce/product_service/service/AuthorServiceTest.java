package com.bookecommerce.product_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bookecommerce.product_service.dto.AuthorCreateRequest;
import com.bookecommerce.product_service.entity.Author;
import com.bookecommerce.product_service.repository.AuthorRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {
    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorService authorService;

    @Test
    void createAuthorReturnsDto() {
        Author author = new Author();
        author.setId(UUID.randomUUID());
        author.setName("Robert C. Martin");
        when(authorRepository.save(org.mockito.ArgumentMatchers.any(Author.class))).thenReturn(author);

        var response = authorService.create(new AuthorCreateRequest("Robert C. Martin", "Author"));

        assertThat(response.id()).isEqualTo(author.getId());
        assertThat(response.name()).isEqualTo("Robert C. Martin");
    }

    @Test
    void getAuthorReturnsDto() {
        UUID id = UUID.randomUUID();
        Author author = new Author();
        author.setId(id);
        author.setName("Author");
        when(authorRepository.findById(id)).thenReturn(java.util.Optional.of(author));

        assertThat(authorService.findById(id).id()).isEqualTo(id);
    }
}