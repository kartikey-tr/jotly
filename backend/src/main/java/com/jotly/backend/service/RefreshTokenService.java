package com.jotly.backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final long SLIDING_EXPIRY_DAYS = 60;
    private static final long HARD_EXPIRY_DAYS = 365;

    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Generate a cryptographically secure refresh token
    public String generateRefreshToken() {

        byte[] randomBytes = new byte[64];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    // Generate a unique refresh-token family/session ID
    public String generateFamilyId() {
        return UUID.randomUUID().toString();
    }

    // Store refresh token in Redis
    public void saveRefreshToken(
            String refreshToken,
            Integer userId,
            String familyId,
            Instant createdAt,
            Instant absoluteExpiry,
            Instant expiry
    ) {

        String key = "refresh:" + refreshToken;

        String value =
                userId + "|" +
                        familyId + "|" +
                        createdAt.toEpochMilli() + "|" +
                        absoluteExpiry.toEpochMilli() + "|ACTIVE";

        long ttlSeconds =
                Duration.between(
                        Instant.now(),
                        expiry
                ).getSeconds();

        redisTemplate.opsForValue().set(
                key,
                value,
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    // Get refresh-token information
    public RefreshTokenData getRefreshTokenData(
            String refreshToken
    ) {

        String key = "refresh:" + refreshToken;

        String value =
                redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        String[] parts = value.split("\\|");

        if (parts.length != 5) {
            return null;
        }

        return new RefreshTokenData(
                Integer.parseInt(parts[0]),
                parts[1],
                Instant.ofEpochMilli(
                        Long.parseLong(parts[2])
                ),
                Instant.ofEpochMilli(
                        Long.parseLong(parts[3])
                ),
                parts[4]
        );
    }

    // Mark token as rotated
    public void markTokenAsRotated(
            String refreshToken
    ) {

        String key = "refresh:" + refreshToken;

        String value =
                redisTemplate.opsForValue().get(key);

        if (value == null) {
            return;
        }

        String[] parts = value.split("\\|");

        if (parts.length != 5) {
            return;
        }

        parts[4] = "ROTATED";

        String updatedValue =
                String.join("|", parts);

        Long ttl =
                redisTemplate.getExpire(
                        key,
                        TimeUnit.SECONDS
                );

        if (ttl != null && ttl > 0) {

            redisTemplate.opsForValue().set(
                    key,
                    updatedValue,
                    ttl,
                    TimeUnit.SECONDS
            );
        }
    }

    // Check whether token has already been rotated
    public boolean isTokenRotated(
            String refreshToken
    ) {

        RefreshTokenData data =
                getRefreshTokenData(refreshToken);

        return data != null &&
                "ROTATED".equals(data.status());
    }

    // Revoke an entire refresh-token family
    public void revokeFamily(
            String familyId
    ) {

        String pattern = "refresh:*";

        var keys =
                redisTemplate.keys(pattern);

        if (keys == null) {
            return;
        }

        for (String key : keys) {

            String value =
                    redisTemplate.opsForValue().get(key);

            if (value == null) {
                continue;
            }

            String[] parts =
                    value.split("\\|");

            if (parts.length >= 2 &&
                    parts[1].equals(familyId)) {

                redisTemplate.delete(key);
            }
        }
    }

    // Delete a specific refresh token
    public void deleteRefreshToken(
            String refreshToken
    ) {

        redisTemplate.delete(
                "refresh:" + refreshToken
        );
    }

    // Data stored for a refresh token
    public record RefreshTokenData(
            Integer userId,
            String familyId,
            Instant createdAt,
            Instant absoluteExpiry,
            String status
    ) {}
}