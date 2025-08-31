package org.example.moliyaapp.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String KEY = "wgZLShDJ98aQVgQFtdQ0+sYrV5zqK6+fJiOLtU1YZKxYEdUghMFq93sdUNhU+aWoJlD80I5gKn+Rz8UhDZQhzA==";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(KEY));

    private static final long ACCESS_TOKEN_EXPIRATION_MS = 1000L * 60 * 60 * 8; // 8 h
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 1000L * 60 * 60 * 8; // 8 h

    public String generateAccessToken(String phone) {
        return buildToken(new HashMap<>(), phone, ACCESS_TOKEN_EXPIRATION_MS);
    }

    public String generateRefreshToken(String phone) {
        return buildToken(new HashMap<>(), phone, REFRESH_TOKEN_EXPIRATION_MS);
    }

    public String buildToken(Map<String, Object> claims, String subject, long expirationMs) {
        return Jwts
                .builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();
    }


    public String generateToken(String phone) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, phone);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 8)) // 8 h
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();
    }

    public Boolean validateToken(String token, String phone) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(phone) && !isTokenExpired(token));
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .setAllowedClockSkewSeconds(30)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public Date getAccessTokenExpiredDate(String accessToken) {
        return extractAllClaims(accessToken).getExpiration();
    }

    public Date getRefreshTokenExpiredDate(String refreshToken) {
        return extractAllClaims(refreshToken).getExpiration();
    }

    public String getSecretKeyAsString() {
        return Base64.getEncoder().encodeToString(SECRET_KEY.getEncoded());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())// Agar audience tekshirish kerak bo'lsa
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject(); // Username - "subject" maydonida bo'ladi
    }

    private Key getSigningKey() {
        return SECRET_KEY;
    }

}