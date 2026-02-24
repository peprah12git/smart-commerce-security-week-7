package com.smartcommerce.security;

import com.smartcommerce.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class   JwtUtil {

    private final SecretKey key = Keys.hmacShaKeyFor("SmartCommerceSecretKeyForJWTTokenGeneration12345".getBytes());
    private final long expiration = 86400000; // 24 hours

    public String generateToken(User user) {
        return generateToken(user.getUserId(), user.getEmail(), user.getRole());
    }

    public String generateToken(int userId, String email, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role) // role claim for authorization checks
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    public int getUserIdFromToken(String token) {
        return Integer.parseInt(getClaims(token).getSubject());
    }
}
