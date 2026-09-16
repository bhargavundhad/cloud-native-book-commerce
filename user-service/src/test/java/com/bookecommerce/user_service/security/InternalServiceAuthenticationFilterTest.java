package com.bookecommerce.user_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class InternalServiceAuthenticationFilterTest {

    @Test
    void acceptsValidInternalSecret() throws Exception {
        InternalServiceAuthenticationFilter filter = new InternalServiceAuthenticationFilter(new ObjectMapper());
        ReflectionTestUtils.setField(filter, "expectedSecret", "test-secret");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/api/internal/users/123/exists");
        when(request.getHeader("X-Internal-Service-Secret")).thenReturn("test-secret");

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("ROLE_INTERNAL_SERVICE", SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority());
        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsMissingInternalSecret() throws Exception {
        InternalServiceAuthenticationFilter filter = new InternalServiceAuthenticationFilter(new ObjectMapper());
        ReflectionTestUtils.setField(filter, "expectedSecret", "test-secret");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        java.io.PrintWriter writer = mock(java.io.PrintWriter.class);

        when(request.getRequestURI()).thenReturn("/api/internal/users/123/exists");
        when(request.getHeader("X-Internal-Service-Secret")).thenReturn(null);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void rejectsIncorrectInternalSecret() throws Exception {
        InternalServiceAuthenticationFilter filter = new InternalServiceAuthenticationFilter(new ObjectMapper());
        ReflectionTestUtils.setField(filter, "expectedSecret", "test-secret");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        java.io.PrintWriter writer = mock(java.io.PrintWriter.class);

        when(request.getRequestURI()).thenReturn("/api/internal/users/123/exists");
        when(request.getHeader("X-Internal-Service-Secret")).thenReturn("wrong-secret");
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(401);
        verify(chain, never()).doFilter(request, response);
    }
}
