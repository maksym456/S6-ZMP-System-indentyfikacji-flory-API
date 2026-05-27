package com.ezielnik.api.config;

import com.ezielnik.api.user.User;
import com.ezielnik.api.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.UUID;

@Component
public class AccountStatusInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    public AccountStatusInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws IOException {

        String path = request.getRequestURI();

        if (isPublicPath(path)) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return true;
        }

        String purpose = jwtAuthentication.getToken().getClaimAsString("purpose");
        if ("pre_auth".equals(purpose)) {
            if (path.equals("/users/verify-2fa") || path.equals("/users/2fa/send-email-code")) {
                return true;
            }
            writeError(response, HttpStatus.FORBIDDEN, "Full authentication required");
            return false;
        }

        if (path.equals("/users/verify-2fa") || path.equals("/users/2fa/send-email-code")) {
            writeError(response, HttpStatus.FORBIDDEN, "Pre-authentication required");
            return false;
        }

        UUID userId = UUID.fromString(jwtAuthentication.getToken().getSubject());

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            writeError(response, HttpStatus.UNAUTHORIZED, "User not found");
            return false;
        }

        if (!user.isActive()) {
            writeError(response, HttpStatus.FORBIDDEN, "User account is inactive");
            return false;
        }

        if (path.equals("/users/me")) {
            return true;
        }

        if (!user.isVerified()) {
            writeError(response, HttpStatus.FORBIDDEN, "Please verify your email first");
            return false;
        }

        return true;
    }

    private boolean isPublicPath(String path) {
        return path.equals("/users/register")
                || path.equals("/users/login")
                || path.equals("/users/verify")
                || path.equals("/users/resend-verification")
                || path.equals("/users/forgot-password")
                || path.equals("/users/reset-password")
                || path.equals("/herbaria/public")
                || path.startsWith("/photos/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.equals("/error");
    }

    private void writeError(HttpServletResponse response,
                            HttpStatus status,
                            String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().write(message);
    }
}