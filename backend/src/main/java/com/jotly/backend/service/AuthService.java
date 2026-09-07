package com.jotly.backend.service;

import com.jotly.backend.dto.*;
import com.jotly.backend.model.User;
import com.jotly.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    private final CloudinaryService cloudinaryService;

    public AuthService(
            EmailService emailService,
            JwtService jwtService,
            UserRepository userRepository,
            OtpService otpService,
            RefreshTokenService refreshTokenService,
            CloudinaryService cloudinaryService
    ) {
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.refreshTokenService = refreshTokenService;
        this.cloudinaryService = cloudinaryService;
    }

    // REGISTER

    public RegisterResponse register(
            RegisterRequest request,
            MultipartFile profilePhoto
    ) {

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Upload photo to Cloudinary.
        // If no photo is provided, CloudinaryService
        // returns the default profile photo URL
        // and null public ID.
        CloudinaryService.CloudinaryUploadResult uploadResult =
                cloudinaryService.uploadProfilePhoto(profilePhoto);

        String photoUrl = uploadResult.secureUrl();
        String photoPublicId = uploadResult.publicId();

        // Store temporary registration data in Redis
        RegistrationData registrationData =
                new RegistrationData(
                        request.getName(),
                        request.getEmail(),
                        photoUrl,
                        photoPublicId
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

        // Retrieve temporary registration data from Redis
        RegistrationData data =
                otpService.getRegistrationData(email);

        if (data == null) {
            throw new RuntimeException(
                    "Registration expired. Please register again."
            );
        }

        // CREATE PERMANENT USER

        User user = new User();

        user.setName(data.getName());
        user.setEmail(data.getEmail());

        // Cloudinary/default photo URL
        user.setProfilePhoto(data.getProfilePhoto());

        // Cloudinary public ID
        // This will be null when using the default photo.
        user.setProfilePhotoPublicId(
                data.getProfilePhotoPublicId()
        );

        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        // Remove temporary registration data
        otpService.deleteRegistrationData(email);

        // ACCESS TOKEN

        String accessToken =
                jwtService.generateToken(
                        user.getEmail()
                );

        // REFRESH TOKEN

        String refreshToken =
                refreshTokenService.generateRefreshToken();

        // REFRESH TOKEN FAMILY

        String familyId =
                refreshTokenService.generateFamilyId();

        // TOKEN EXPIRY

        Instant createdAt = Instant.now();

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

        // STORE REFRESH TOKEN IN REDIS

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

        // ACCESS TOKEN

        String accessToken =
                jwtService.generateToken(
                        user.getEmail()
                );

        // REFRESH TOKEN

        String refreshToken =
                refreshTokenService.generateRefreshToken();

        // REFRESH TOKEN FAMILY

        String familyId =
                refreshTokenService.generateFamilyId();

        // TOKEN EXPIRY

        Instant createdAt = Instant.now();

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

        // STORE REFRESH TOKEN IN REDIS


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


    public RefreshResponse refresh(
            String refreshToken
    ) {

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

        // DETECT REFRESH TOKEN REUSE

        if ("ROTATED".equals(tokenData.status())) {

            // Revoke entire refresh-token family
            refreshTokenService.revokeFamily(
                    tokenData.familyId()
            );

            throw new RuntimeException(
                    "Refresh token reuse detected. Please login again."
            );
        }

        // CHECK HARD EXPIRY

        Instant now = Instant.now();

        if (now.isAfter(tokenData.absoluteExpiry())) {

            refreshTokenService.revokeFamily(
                    tokenData.familyId()
            );

            throw new RuntimeException(
                    "Refresh token has expired. Please login again."
            );
        }

        // FIND USER


        User user =
                userRepository.findById(
                                tokenData.userId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        // ROTATE OLD REFRESH TOKEN


        refreshTokenService.markTokenAsRotated(
                refreshToken
        );

        // GENERATE NEW ACCESS TOKEN


        String accessToken =
                jwtService.generateToken(
                        user.getEmail()
                );

        // GENERATE NEW REFRESH TOKEN


        String newRefreshToken =
                refreshTokenService.generateRefreshToken();


        // NEW SLIDING EXPIRY


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

        // STORE NEW REFRESH TOKEN

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