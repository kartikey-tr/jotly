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

    // Generate a 6-digit OTP
    public String generateOtp() {
        return String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );
    }

    // Save registration OTP for 5 minutes
    public void saveRegistrationOtp(String email, String otp) {

        String key = "otp:registration:" + email;

        redisTemplate.opsForValue().set(
                key,
                otp,
                5,
                TimeUnit.MINUTES
        );
    }

    // Verify registration OTP
    public boolean verifyRegistrationOtp(String email, String otp) {

        String key = "otp:registration:" + email;

        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            return false;
        }

        if (!storedOtp.equals(otp)) {
            return false;
        }

        // Delete OTP after successful verification
        redisTemplate.delete(key);

        return true;
    }

    // Save login OTP for 5 minutes
    public void saveLoginOtp(String email, String otp) {

        String key = "otp:login:" + email;

        redisTemplate.opsForValue().set(
                key,
                otp,
                5,
                TimeUnit.MINUTES
        );
    }

    // Verify login OTP
    public boolean verifyLoginOtp(String email, String otp) {

        String key = "otp:login:" + email;

        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            return false;
        }

        if (!storedOtp.equals(otp)) {
            return false;
        }

        // Delete OTP after successful verification
        redisTemplate.delete(key);

        return true;
    }

    // Save temporary registration data in Redis
    public void saveRegistrationData(RegistrationData data) {

        String key = "registration:" + data.getEmail();

        redisTemplate.opsForHash().put(
                key,
                "name",
                data.getName()
        );

        redisTemplate.opsForHash().put(
                key,
                "email",
                data.getEmail()
        );

        redisTemplate.opsForHash().put(
                key,
                "profilePhoto",
                data.getProfilePhoto()
        );

        // Public ID is null when the user uses the default photo
        if (data.getProfilePhotoPublicId() != null) {
            redisTemplate.opsForHash().put(
                    key,
                    "profilePhotoPublicId",
                    data.getProfilePhotoPublicId()
            );
        }

        // Registration data expires after 10 minutes
        redisTemplate.expire(
                key,
                10,
                TimeUnit.MINUTES
        );
    }

    // Get temporary registration data from Redis
    public RegistrationData getRegistrationData(String email) {

        String key = "registration:" + email;

        Map<Object, Object> data =
                redisTemplate.opsForHash().entries(key);

        if (data.isEmpty()) {
            return null;
        }

        return new RegistrationData(
                (String) data.get("name"),
                (String) data.get("email"),
                (String) data.get("profilePhoto"),
                (String) data.get("profilePhotoPublicId")
        );
    }

    // Delete temporary registration data
    public void deleteRegistrationData(String email) {

        String key = "registration:" + email;

        redisTemplate.delete(key);
    }
}