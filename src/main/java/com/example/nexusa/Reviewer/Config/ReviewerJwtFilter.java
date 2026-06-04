package com.example.nexusa.Reviewer.Config;

import com.example.nexusa.Repository.ReviewerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ReviewerJwtFilter extends OncePerRequestFilter {

    private final ReviewerJwtService reviewerJwtService;
    private final ReviewerRepository reviewerRepository;

    public ReviewerJwtFilter(ReviewerJwtService reviewerJwtService,
                             ReviewerRepository reviewerRepository) {
        this.reviewerJwtService  = reviewerJwtService;
        this.reviewerRepository  = reviewerRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.startsWith("/reviewer")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (!reviewerJwtService.isValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String email = reviewerJwtService.extractEmail(token);
        String role  = reviewerJwtService.extractRole(token);

        if (!"REVIEWER".equals(role)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Verify reviewer exists in reviewers table — not users table
        reviewerRepository.findByEmail(email).ifPresent(reviewer -> {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_REVIEWER"))
                    );
            SecurityContextHolder.getContext().setAuthentication(auth);
        });

        filterChain.doFilter(request, response);
    }
}