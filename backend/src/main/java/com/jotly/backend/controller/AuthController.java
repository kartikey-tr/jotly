package com.jotly.backend.controller;

import com.jotly.backend.service.CloudinaryService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import com.jotly.backend.dto.LoginResponse;
import com.jotly.backend.dto.RefreshRequest;
import com.jotly.backend.dto.RefreshResponse;
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
    private final CloudinaryService cloudinaryService;

    public AuthController(AuthService authService, CloudinaryService cloudinaryService) {
        this.authService = authService;
        this.cloudinaryService = cloudinaryService;
    }

    // Register a new user and send OTP
    @PostMapping(
            value = "/register",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<RegisterResponse> register(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam(value = "profilePhoto", required = false)
            MultipartFile profilePhoto
    ) {

        System.out.println("========== PROFILE PHOTO DEBUG ==========");

        if (profilePhoto == null) {
            System.out.println("PROFILE PHOTO: NULL");
        } else {
            System.out.println("PROFILE PHOTO NAME: "
                    + profilePhoto.getOriginalFilename());

            System.out.println("PROFILE PHOTO SIZE: "
                    + profilePhoto.getSize());

            System.out.println("PROFILE PHOTO TYPE: "
                    + profilePhoto.getContentType());

            System.out.println("PROFILE PHOTO EMPTY: "
                    + profilePhoto.isEmpty());
        }

        System.out.println("=========================================");

        RegisterRequest request = new RegisterRequest();
        request.setName(name);
        request.setEmail(email);

        RegisterResponse response =
                authService.register(request, profilePhoto);

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

    // Verify login OTP and generate access + refresh tokens
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

    // Refresh access token
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {

        RefreshResponse response =
                authService.refresh(
                        request.getRefreshToken()
                );

        return ResponseEntity.ok(response);
    }
}