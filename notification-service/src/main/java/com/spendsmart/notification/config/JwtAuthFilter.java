package com.spendsmart.notification.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    // Build the HMAC-SHA signing key (same algorithm as auth-service)
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Step 1: Get the Authorization header
        String authHeader = request.getHeader("Authorization");

        // Step 2: If no token or wrong format, skip this filter (let Spring Security handle it)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Step 3: Extract the actual token (remove "Bearer " prefix)
            String token = authHeader.substring(7).trim();

            // Step 4: Validate and parse the token
            // This will throw an exception if the token is expired, tampered, or invalid
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Step 5: Extract user info from token claims
            String email = claims.getSubject();           // set by auth-service during login
            Integer userId = claims.get("userId") != null ? ((Number) claims.get("userId")).intValue() : null; // custom claim

            // Step 6: Tell Spring Security "this user is authenticated"
            // UsernamePasswordAuthenticationToken(principal, credentials, authorities)
            // principal = email, credentials = null (already verified), authorities = empty
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());

            // Store userId in the details so controllers can access it if needed
            authentication.setDetails(userId);

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // Token is invalid, expired, or tampered
            // Clear any existing authentication
            SecurityContextHolder.clearContext();

            // Return 401 Unauthorized with error message
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid or expired JWT token\"}");
            return; // Stop filter chain and return
        }

        filterChain.doFilter(request, response);
    }
}