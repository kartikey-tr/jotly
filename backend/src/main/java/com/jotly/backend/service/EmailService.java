package com.jotly.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String otp) {

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8"
            );

            helper.setTo(to);
            helper.setSubject("Your Jotly verification code");

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>

                <body style="
                    margin: 0;
                    padding: 0;
                    background-color: #f6f7f9;
                    font-family: Arial, Helvetica, sans-serif;
                ">

                    <div style="
                        max-width: 520px;
                        margin: 40px auto;
                        background: #ffffff;
                        border-radius: 12px;
                        overflow: hidden;
                        border: 1px solid #e5e7eb;
                    ">

                        <!-- Header -->
                        <div style="
                            padding: 28px 32px;
                            text-align: center;
                            border-bottom: 1px solid #eeeeee;
                        ">
                            <h1 style="
                                margin: 0;
                                font-size: 28px;
                                color: #111827;
                                letter-spacing: -0.5px;
                            ">
                                Jotly
                            </h1>
                        </div>

                        <!-- Content -->
                        <div style="padding: 36px 32px;">

                            <h2 style="
                                margin: 0 0 16px;
                                font-size: 22px;
                                color: #111827;
                            ">
                                Verify your email
                            </h2>

                            <p style="
                                margin: 0 0 24px;
                                font-size: 15px;
                                line-height: 1.6;
                                color: #4b5563;
                            ">
                                Use the verification code below to continue
                                with your Jotly account.
                            </p>

                            <!-- OTP -->
                            <div style="
                                margin: 28px 0;
                                text-align: center;
                            ">
                                <div style="
                                    display: inline-block;
                                    padding: 16px 28px;
                                    background-color: #f3f4f6;
                                    border-radius: 10px;
                                    font-size: 32px;
                                    font-weight: bold;
                                    letter-spacing: 8px;
                                    color: #111827;
                                ">
                                    %s
                                </div>
                            </div>

                            <p style="
                                margin: 24px 0 8px;
                                font-size: 14px;
                                color: #6b7280;
                                text-align: center;
                            ">
                                This code expires in <strong>5 minutes</strong>.
                            </p>

                            <p style="
                                margin: 24px 0 0;
                                font-size: 14px;
                                line-height: 1.6;
                                color: #6b7280;
                            ">
                                If you didn't request this verification code,
                                you can safely ignore this email.
                            </p>

                        </div>

                        <!-- Footer -->
                        <div style="
                            padding: 20px 32px;
                            background-color: #f9fafb;
                            text-align: center;
                        ">
                            <p style="
                                margin: 0;
                                font-size: 12px;
                                color: #9ca3af;
                            ">
                                © 2026 Jotly. All rights reserved.
                            </p>
                        </div>

                    </div>

                </body>
                </html>
                """.formatted(otp);

            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}