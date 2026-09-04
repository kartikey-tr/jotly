package com.jotly.backend.controller;

import com.jotly.backend.dto.LoginResponse;
import com.jotly.backend.dto.RegisterRequest;
import com.jotly.backend.dto.RegisterResponse;
import com.jotly.backend.dto.VerifyOtpRequest;
import com.jotly.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Register a new user and send OTP
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response =
                authService.register(request);

        return ResponseEntity.ok(response);
    }

    // Verify OTP during registration and create the user
    @PostMapping("/verify-registration-otp")
    public ResponseEntity<LoginResponse> verifyRegistrationOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        LoginResponse response =
                authService.verifyRegistrationOtp(
                        request.getEmail(),
                        request.getOtp()
                );

        return ResponseEntity.ok(response);
    }

    // Send OTP for login
    @PostMapping("/send-login-otp")
    public ResponseEntity<String> sendLoginOtp(
            @RequestParam String email) {

        authService.sendLoginOtp(email);

        return ResponseEntity.ok(
                "Login OTP sent successfully"
        );
    }

    // Verify login OTP and generate JWT
    @PostMapping("/verify-login-otp")
    public ResponseEntity<LoginResponse> verifyLoginOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        LoginResponse response =
                authService.verifyLoginOtp(
                        request.getEmail(),
                        request.getOtp()
                );

        return ResponseEntity.ok(response);
    }
}