package com.jotly.backend.service;

import com.jotly.backend.dto.*;
import com.jotly.backend.model.User;
import com.jotly.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final EmailService emailService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService;

    @Value("${jotly.default-profile-photo}")
    private String defaultProfilePhoto;

    public AuthService(
            EmailService emailService,
            JwtService jwtService,
            UserRepository userRepository,
            OtpService otpService,
            RefreshTokenService refreshTokenService
    ) {
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.refreshTokenService = refreshTokenService;
    }

    // REGISTER

    public RegisterResponse register(RegisterRequest request) {

        // Check email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Use default profile photo if user didn't provide one
        String profilePhoto = request.getProfilePhoto();

        if (profilePhoto == null || profilePhoto.isBlank()) {
            profilePhoto = defaultProfilePhoto;
        }

        // Store temporary registration data in Redis
        RegistrationData registrationData =
                new RegistrationData(
                        request.getName(),
                        request.getEmail(),
                        profilePhoto
                );

        otpService.saveRegistrationData(registrationData);

        // Generate registration OTP
        String otp = otpService.generateOtp();

        // Store OTP in Redis
        otpService.saveRegistrationOtp(
                request.getEmail(),
                otp
        );

        // Send OTP through email
        emailService.sendOtp(
                request.getEmail(),
                otp
        );

        return new RegisterResponse(
                "Registration OTP sent successfully",
                request.getName()
        );
    }

    // VERIFY REGISTRATION OTP

    public LoginResponse verifyRegistrationOtp(
            String email,
            String otp
    ) {

        boolean valid =
                otpService.verifyRegistrationOtp(
                        email,
                        otp
                );

        if (!valid) {
            throw new RuntimeException(
                    "Invalid or expired OTP"
            );
        }

        // Retrieve temporary registration data
        RegistrationData data =
                otpService.getRegistrationData(email);

        if (data == null) {
            throw new RuntimeException(
                    "Registration expired. Please register again."
            );
        }

        // Create permanent user
        User user = new User();

        user.setName(data.getName());
        user.setEmail(data.getEmail());
        user.setProfilePhoto(data.getProfilePhoto());
        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        // Remove temporary registration data
        otpService.deleteRegistrationData(email);

        // Generate access token
        String accessToken =
                jwtService.generateToken(
                        user.getEmail()
                );

        // Generate refresh token
        String refreshToken =
                refreshTokenService.generateRefreshToken();

        // Generate refresh-token family
        String familyId =
                refreshTokenService.generateFamilyId();

        // Record creation time
        Instant createdAt =
                Instant.now();

        // Hard expiry: 1 year
        Instant absoluteExpiry =
                createdAt.plus(
                        365,
                        ChronoUnit.DAYS
                );

        // Initial sliding expiry: 60 days
        Instant expiry =
                createdAt.plus(
                        60,
                        ChronoUnit.DAYS
                );

        // Store refresh token in Redis
        refreshTokenService.saveRefreshToken(
                refreshToken,
                user.getId(),
                familyId,
                createdAt,
                absoluteExpiry,
                expiry
        );

        return new LoginResponse(
                "Registration successful",
                user.getEmail(),
                accessToken,
                refreshToken
        );
    }

    // SEND LOGIN OTP

    public void sendLoginOtp(String email) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        if (!user.isEmailVerified()) {
            throw new RuntimeException(
                    "Please verify your email first"
            );
        }

        // Generate login OTP
        String otp = otpService.generateOtp();

        // Store login OTP in Redis
        otpService.saveLoginOtp(
                email,
                otp
        );

        // Send OTP
        emailService.sendOtp(
                email,
                otp
        );
    }

    // VERIFY LOGIN OTP

    public LoginResponse verifyLoginOtp(
            String email,
            String otp
    ) {

        boolean valid =
                otpService.verifyLoginOtp(
                        email,
                        otp
                );

        if (!valid) {
            throw new RuntimeException(
                    "Invalid or expired OTP"
            );
        }

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        // Generate access token
        String accessToken =
                jwtService.generateToken(
                        user.getEmail()
                );

        // Generate refresh token
        String refreshToken =
                refreshTokenService.generateRefreshToken();

        // Generate refresh-token family
        String familyId =
                refreshTokenService.generateFamilyId();

        // Record creation time
        Instant createdAt =
                Instant.now();

        // Hard expiry: 1 year
        Instant absoluteExpiry =
                createdAt.plus(
                        365,
                        ChronoUnit.DAYS
                );

        // Initial sliding expiry: 60 days
        Instant expiry =
                createdAt.plus(
                        60,
                        ChronoUnit.DAYS
                );

        // Store refresh token in Redis
        refreshTokenService.saveRefreshToken(
                refreshToken,
                user.getId(),
                familyId,
                createdAt,
                absoluteExpiry,
                expiry
        );

        return new LoginResponse(
                "Login successful",
                user.getEmail(),
                accessToken,
                refreshToken
        );
    }

    // REFRESH ACCESS TOKEN

    public RefreshResponse refresh(String refreshToken) {

        // Find refresh token in Redis
        RefreshTokenService.RefreshTokenData tokenData =
                refreshTokenService.getRefreshTokenData(
                        refreshToken
                );

        // Token does not exist or has expired
        if (tokenData == null) {
            throw new RuntimeException(
                    "Invalid or expired refresh token"
            );
        }

        // Detect reuse of an already-rotated token
        if ("ROTATED".equals(tokenData.status())) {

            // Revoke entire refresh-token family
            refreshTokenService.revokeFamily(
                    tokenData.familyId()
            );

            throw new RuntimeException(
                    "Refresh token reuse detected. Please login again."
            );
        }

        Instant now = Instant.now();

        // Check hard 1-year expiry
        if (now.isAfter(tokenData.absoluteExpiry())) {

            refreshTokenService.revokeFamily(
                    tokenData.familyId()
            );

            throw new RuntimeException(
                    "Refresh token has expired. Please login again."
            );
        }

        // Find user
        User user =
                userRepository.findById(
                                tokenData.userId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        // Rotate old refresh token
        refreshTokenService.markTokenAsRotated(
                refreshToken
        );

        // Generate new access token
        String accessToken =
                jwtService.generateToken(
                        user.getEmail()
                );

        // Generate new refresh token
        String newRefreshToken =
                refreshTokenService.generateRefreshToken();

        // Sliding expiry: 60 days from now
        Instant newExpiry =
                now.plus(
                        60,
                        ChronoUnit.DAYS
                );

        // Never go beyond the original 1-year hard expiry
        if (newExpiry.isAfter(
                tokenData.absoluteExpiry()
        )) {
            newExpiry =
                    tokenData.absoluteExpiry();
        }

        // Store new refresh token
        refreshTokenService.saveRefreshToken(
                newRefreshToken,
                user.getId(),
                tokenData.familyId(),
                tokenData.createdAt(),
                tokenData.absoluteExpiry(),
                newExpiry
        );

        return new RefreshResponse(
                accessToken,
                newRefreshToken
        );
    }
}