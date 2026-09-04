package com.jotly.backend.service;

import com.jotly.backend.dto.LoginRequest;
import com.jotly.backend.dto.LoginResponse;
import com.jotly.backend.dto.RegisterRequest;
import com.jotly.backend.dto.RegisterResponse;
import com.jotly.backend.model.User;
import com.jotly.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {
    final private JwtService jwtService;
    final private UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(JwtService jwtService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ye hmara register krne waala hai
    public RegisterResponse register(RegisterRequest request) {
        // yaha abhi exception handle krna hai jisse direct signin page pe redirect ho
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();

        user.setName(request.getName());
        user.setUsername(request.getUsername());
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        user.setCreatedAt(LocalDateTime.now());
        User savedUser=userRepository.save(user);

        // iske liye Wrapper bana sakte hai lkin dekhte hai aage ke use se
        RegisterResponse response=new RegisterResponse();
        response.setName(request.getName());
        response.setUsername(request.getUsername());

        return response;

    }
    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new RuntimeException("Invalid username or password")
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new RuntimeException("Invalid username or password");
        }

        return new LoginResponse(
                "Login successful",
                user.getUsername()
        );
    }
}
