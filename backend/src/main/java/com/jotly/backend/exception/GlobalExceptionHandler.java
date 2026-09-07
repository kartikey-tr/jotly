//package com.jotly.backend.exception;
//
//import jakarta.validation.ConstraintViolationException;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//import java.time.LocalDateTime;
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    private ResponseEntity<ApiError> buildError(
//            HttpStatus status,
//            String code,
//            String message
//    ) {
//        ApiError error = new ApiError(
//                code,
//                message,
//                LocalDateTime.now()
//        );
//
//        return ResponseEntity.status(status).body(error);
//    }
//
//    // Email already exists
//    @ExceptionHandler(EmailAlreadyExistsException.class)
//    public ResponseEntity<ApiError> handleEmailAlreadyExists(
//            EmailAlreadyExistsException ex
//    ) {
//        return buildError(
//                HttpStatus.CONFLICT,
//                "EMAIL_ALREADY_EXISTS",
//                ex.getMessage()
//        );
//    }
//
//    // User not found
//    @ExceptionHandler(UserNotFoundException.class)
//    public ResponseEntity<ApiError> handleUserNotFound(
//            UserNotFoundException ex
//    ) {
//        return buildError(
//                HttpStatus.NOT_FOUND,
//                "USER_NOT_FOUND",
//                ex.getMessage()
//        );
//    }
//
//    // Invalid OTP
//    @ExceptionHandler(InvalidOtpException.class)
//    public ResponseEntity<ApiError> handleInvalidOtp(
//            InvalidOtpException ex
//    ) {
//        return buildError(
//                HttpStatus.BAD_REQUEST,
//                "INVALID_OTP",
//                ex.getMessage()
//        );
//    }
//
//    // Registration expired
//    @ExceptionHandler(RegistrationExpiredException.class)
//    public ResponseEntity<ApiError> handleRegistrationExpired(
//            RegistrationExpiredException ex
//    ) {
//        return buildError(
//                HttpStatus.BAD_REQUEST,
//                "REGISTRATION_EXPIRED",
//                ex.getMessage()
//        );
//    }
//
//    // Invalid refresh token
//    @ExceptionHandler(InvalidRefreshTokenException.class)
//    public ResponseEntity<ApiError> handleInvalidRefreshToken(
//            InvalidRefreshTokenException ex
//    ) {
//        return buildError(
//                HttpStatus.UNAUTHORIZED,
//                "INVALID_REFRESH_TOKEN",
//                ex.getMessage()
//        );
//    }
//
//    // Validation errors
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ApiError> handleValidation(
//            MethodArgumentNotValidException ex
//    ) {
//
//        String message = ex.getBindingResult()
//                .getFieldErrors()
//                .stream()
//                .findFirst()
//                .map(error -> error.getDefaultMessage())
//                .orElse("Invalid request");
//
//        return buildError(
//                HttpStatus.BAD_REQUEST,
//                "VALIDATION_ERROR",
//                message
//        );
//    }
//
//    // Constraint violations
//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<ApiError> handleConstraintViolation(
//            ConstraintViolationException ex
//    ) {
//        return buildError(
//                HttpStatus.BAD_REQUEST,
//                "VALIDATION_ERROR",
//                ex.getMessage()
//        );
//    }
//}