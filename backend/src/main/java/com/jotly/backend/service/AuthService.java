package com.jotly.backend.service;

import com.jotly.backend.dto.LoginRequest;
import com.jotly.backend.dto.LoginResponse;
import com.jotly.backend.dto.RegisterRequest;
import com.jotly.backend.dto.RegisterResponse;
import com.jotly.backend.dto.RegistrationData;
import com.jotly.backend.model.User;
import com.jotly.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final EmailService emailService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public AuthService(EmailService emailService, JwtService jwtService, UserRepository userRepository, PasswordEncoder passwordEncoder, OtpService otpService) {
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    // Register user -> generate OTP -> store temporary data in Redis
    public RegisterResponse register(RegisterRequest request) {

        // Check username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Hash password BEFORE storing it temporarily
        String passwordHash =
                passwordEncoder.encode(request.getPassword());

        // Store registration information temporarily in Redis
        RegistrationData registrationData =
                new RegistrationData(
                        request.getName(),
                        request.getUsername(),
                        request.getEmail(),
                        passwordHash
                );

        otpService.saveRegistrationData(registrationData);

        // Generate OTP
        String otp = otpService.generateOtp();

        // Store OTP in Redis for 5 minutes
        otpService.saveOtp(
                request.getEmail(),
                otp
        );

        // TEMPORARY
        // Later we will send this through email
        emailService.sendOtp(
                request.getEmail(),
                otp
        );

        RegisterResponse response = new RegisterResponse();

        response.setName(request.getName());
        response.setUsername(request.getUsername());

        return response;
    }
    public RegisterResponse verifyOtp(
            String email,
            String otp
    ) {

        // 1. Verify OTP
        boolean valid = otpService.verifyOtp(email, otp);

        if (!valid) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // 2. Get temporary registration data from Redis
        RegistrationData data =
                otpService.getRegistrationData(email);

        if (data == null) {
            throw new RuntimeException(
                    "Registration expired. Please register again."
            );
        }

        // 3. Create actual User
        User user = new User();

        user.setName(data.getName());
        user.setUsername(data.getUsername());
        user.setEmail(data.getEmail());
        user.setPassword(data.getPasswordHash());

        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());

        // 4. Save user permanently in PostgreSQL
        userRepository.save(user);

        // 5. Remove temporary registration data from Redis
        otpService.deleteRegistrationData(email);

        // 6. Return response
        RegisterResponse response = new RegisterResponse();

        response.setName(user.getName());
        response.setUsername(user.getUsername());

        return response;
    }

    // Login
    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invalid username or password"
                        )
                );

        // Check password
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new RuntimeException(
                    "Invalid username or password"
            );
        }

        // Check email verification
        if (!user.isEmailVerified()) {
            throw new RuntimeException(
                    "Please verify your email first"
            );
        }

        // Generate JWT
        String token =
                jwtService.generateToken(user.getUsername());

        return new LoginResponse(
                "Login successful",
                user.getUsername(),
                token
        );
    }
}