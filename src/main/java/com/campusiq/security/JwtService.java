package com.campusiq.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // Creates a JWT after successful login
    public String generateToken(UserDetails userDetails) {

        Date currentTime = new Date();

        Date expirationTime =
                new Date(currentTime.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(currentTime)
                .expiration(expirationTime)
                .signWith(getSigningKey())
                .compact();
    }

    // Gets username from JWT
    public String extractUsername(String token) {

        return extractAllClaims(token).getSubject();
    }

    // Checks username and expiration
    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

        String username = extractUsername(token);

        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    // Checks whether JWT has expired
    private boolean isTokenExpired(String token) {

        Date expirationDate =
                extractAllClaims(token).getExpiration();

        return expirationDate.before(new Date());
    }

    // Reads all information stored inside JWT
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Converts Base64 secret into a secure signing key
    private SecretKey getSigningKey() {

        byte[] keyBytes =
                Decoders.BASE64.decode(jwtSecret);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}