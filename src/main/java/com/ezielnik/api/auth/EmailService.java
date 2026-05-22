package com.ezielnik.api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private final RestClient restClient;
    private final String appBaseUrl;
    private final String mailFrom;

    public EmailService(@Value("${app.base-url}") String appBaseUrl,
                        @Value("${app.mail.from}") String mailFrom,
                        @Value("${brevo.api-key}") String brevoApiKey) {
        this.appBaseUrl = appBaseUrl.endsWith("/")
                ? appBaseUrl.substring(0, appBaseUrl.length() - 1)
                : appBaseUrl;
        this.mailFrom = mailFrom;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", brevoApiKey)
                .build();
    }

    public void sendVerificationEmail(String toEmail, String verificationToken) {
        String verificationLink = appBaseUrl + "/users/verify?token=" + verificationToken;
        send(toEmail, "Verify your email",
                "Welcome!\n\nClick the link below to verify your email:\n" +
                verificationLink + "\n\nThis link expires in 15 minutes.");
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = appBaseUrl + "/users/reset-password?token=" + resetToken;
        send(toEmail, "Reset your password",
                "Hello,\n\nClick the link below to reset your password:\n" +
                resetLink + "\n\nThis link expires in 15 minutes.\n\n" +
                "If you did not request a password reset, you can ignore this email.");
    }

    public void sendTwoFactorCode(String toEmail, String code) {
        send(toEmail, "Your login verification code",
                "Your verification code is: " + code + "\n\nThis code expires in 10 minutes.\n\n" +
                "If you did not attempt to log in, please secure your account immediately.");
    }

    public void sendAdminWarningEmail(String toEmail, String subject, String warningMessage) {
        send(toEmail, subject,
                "Hello,\n\n" + warningMessage +
                "\n\nThis is an administrative warning regarding your account.\n\neZielnik Team");
    }

    private void send(String toEmail, String subject, String text) {
        restClient.post()
                .uri("/smtp/email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "sender", Map.of("email", mailFrom),
                        "to", List.of(Map.of("email", toEmail)),
                        "subject", subject,
                        "textContent", text
                ))
                .retrieve()
                .toBodilessEntity();
    }
}
