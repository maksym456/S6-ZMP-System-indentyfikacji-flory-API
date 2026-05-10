package com.ezielnik.api.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@NullMarked
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> buckets;
    private final long capacity;
    private final long refillMinutes;

    public RateLimitFilter(@Value("${app.rate-limit.capacity}") long capacity,
                           @Value("${app.rate-limit.refill-minutes}") long refillMinutes) {
        this.capacity = capacity;
        this.refillMinutes = refillMinutes;
        this.buckets = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofMinutes(100))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientKey = resolveClientKey(request);

        Bucket bucket = buckets.getIfPresent(clientKey);

        if (bucket == null) {
            Bucket newBucket = createBucket();
            Bucket existingBucket = buckets.asMap().putIfAbsent(clientKey, newBucket);
            bucket = existingBucket == null ? newBucket : existingBucket;
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long secondsToWait = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setHeader("Retry-After", String.valueOf(secondsToWait));
        response.getWriter().write("Too many requests. Please try again later.");
    }

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(capacity)
                        .refillIntervally(capacity, Duration.ofMinutes(refillMinutes)))
                .build();
    }

    private String resolveClientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}