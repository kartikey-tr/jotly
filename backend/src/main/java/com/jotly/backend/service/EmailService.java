package com.jotly.backend.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Jotly - Email Verification OTP");

        message.setText(
                "Your Jotly verification OTP is: " + otp +
                        "\n\nThis OTP is valid for 5 minutes." +
                        "\n\nIf you did not create a Jotly account, please ignore this email."
        );

        mailSender.send(message);
    }
}