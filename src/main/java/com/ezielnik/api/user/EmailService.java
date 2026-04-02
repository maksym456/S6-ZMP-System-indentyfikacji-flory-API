package com.ezielnik.api.user;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String verificationToken) {
        String verificationLink = "http://localhost:8080/users/verify?token=" + verificationToken;

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