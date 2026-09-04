package com.jotly.backend.service;

import com.jotly.backend.dto.LoginResponse;
import com.jotly.backend.dto.RegisterRequest;
import com.jotly.backend.dto.RegisterResponse;
import com.jotly.backend.dto.RegistrationData;
import com.jotly.backend.model.User;
import com.jotly.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final EmailService emailService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final OtpService otpService;

    @Value("${jotly.default-profile-photo}")
    private String defaultProfilePhoto;

    public AuthService(
            EmailService emailService,
            JwtService jwtService,
            UserRepository userRepository,
            OtpService otpService
    ) {
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.otpService = otpService;
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

        // Generate JWT
        String token =
                jwtService.generateToken(
                        user.getEmail()
                );

        return new LoginResponse(
                "Registration successful",
                user.getEmail(),
                token
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

        // Generate JWT
        String token =
                jwtService.generateToken(
                        user.getEmail()
                );

        return new LoginResponse(
                "Login successful",
                user.getEmail(),
                token
        );
    }
}