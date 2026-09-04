package com.jotly.backend.service;

import com.jotly.backend.dto.RegistrationData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private final StringRedisTemplate redisTemplate;

    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateOtp() {

        return String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );
    }

    public void saveOtp(String email, String otp) {

        String key = "otp:registration:" + email;

        redisTemplate.opsForValue().set(
                key,
                otp,
                5,
                TimeUnit.MINUTES
        );
    }

    public boolean verifyOtp(String email, String otp) {

        String key = "otp:registration:" + email;

        String storedOtp =
                redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            return false;
        }

        if (!storedOtp.equals(otp)) {
            return false;
        }

        redisTemplate.delete(key);

        return true;
    }

    public void saveRegistrationData(
            RegistrationData data
    ) {

        String key = "registration:" + data.getEmail();

        redisTemplate.opsForHash().putAll(
                key,
                Map.of(
                        "name", data.getName(),
                        "username", data.getUsername(),
                        "email", data.getEmail(),
                        "passwordHash", data.getPasswordHash()
                )
        );

        redisTemplate.expire(
                key,
                10,
                TimeUnit.MINUTES
        );
    }

    public RegistrationData getRegistrationData(
            String email
    ) {

        String key = "registration:" + email;

        Map<Object, Object> data =
                redisTemplate.opsForHash().entries(key);

        if (data.isEmpty()) {
            return null;
        }

        return new RegistrationData(
                (String) data.get("name"),
                (String) data.get("username"),
                (String) data.get("email"),
                (String) data.get("passwordHash")
        );
    }

    public void deleteRegistrationData(
            String email
    ) {

        String key = "registration:" + email;

        redisTemplate.delete(key);
    }
}