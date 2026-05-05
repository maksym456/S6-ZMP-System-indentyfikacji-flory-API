package com.ezielnik.api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String appBaseUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.base-url}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.appBaseUrl = appBaseUrl.endsWith("/")
                ? appBaseUrl.substring(0, appBaseUrl.length() - 1)
                : appBaseUrl;
    }

    public void sendVerificationEmail(String toEmail, String verificationToken) {
        String verificationLink = appBaseUrl + "/users/verify?token=" + verificationToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Verify your email");
        message.setText(
                "Welcome!\n\n" +
                        "Click the link below to verify your email:\n" +
                        verificationLink + "\n\n" +
                        "This link expires in 15 minutes."
        );

        mailSender.send(message);
    }
}