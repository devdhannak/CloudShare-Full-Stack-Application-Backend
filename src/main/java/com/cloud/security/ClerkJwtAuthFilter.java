package com.cloud.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class ClerkJwtAuthFilter extends OncePerRequestFilter {
    @Value("${clerk.issuer}")
    private String clerkIssues;

    private final ClerkJwksProvider jwksProvider;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

//        System.out.println(">>> Clerk Issuer: " + clerkIssues);
        // Bug 1 fixed: added return after doFilter for webhook bypass
        if (request.getRequestURI().contains("/api/v1/webhooks") ||
                request.getRequestURI().contains("/api/v1/files/public") ||
                request.getRequestURI().contains("/api/v1/files/download")


        ) {
            filterChain.doFilter(request, response);
            return; // ← was missing
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Authorization header missing or invalid");
            return;
        }

        try {
            String token = authHeader.substring(7);
            String[] chunks = token.split("\\.");
            if (chunks.length < 3) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid JWT Token Format");
                return;
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(chunks[0]));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode headerNode = mapper.readTree(headerJson);

            if (!headerNode.has("kid")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token header is missing kid");
                return;
            }

            String kid = headerNode.get("kid").asText();
            PublicKey publicKey = jwksProvider.getPublicKey(kid);

            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .clockSkewSeconds(60)
                    .requireIssuer(clerkIssues)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String clerkId = claims.getSubject();
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            clerkId, null,
                            Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    );
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response); // ← only called once, here

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid JWT Token: " + e.getMessage());
        }

    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
//        System.out.println(">>> URI: " + uri);
//        System.out.println(">>> ServletPath: " + request.getServletPath());
//        System.out.println(">>> shouldNotFilter URI: [" + uri + "]");
        return uri.contains("/webhooks") ||
                uri.contains("/register") ||
                uri.contains("/files/public") ||
                uri.contains("/files/download") ||
                uri.contains("/health");
//        System.out.println(">>> shouldNotFilter result: " + skip);

    }

}
