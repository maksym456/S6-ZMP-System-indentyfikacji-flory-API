package com.ezielnik.api.security;

import com.ezielnik.api.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.rate-limit.capacity=2", "app.rate-limit.refill-minutes=60"}
)
class RateLimitIT extends IntegrationTestBase {

    @BeforeEach
    void useNonRetryingRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        RestTemplate rt = new RestTemplate(factory);
        rt.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(@org.jspecify.annotations.NonNull ClientHttpResponse response) {
                return false;
            }
        });
        DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory("http://localhost:" + port);
        uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        rt.setUriTemplateHandler(uriFactory);
        restTemplate = rt;
    }

    @Test
    void rateLimiter_decrementsRemainingHeader_and_blocks429WithRetryAfter() {
        ResponseEntity<String> first = restTemplate.exchange(
                "/herbaria/public", HttpMethod.GET, noAuth(), String.class
        );
        assertThat(first.getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(first.getHeaders().getFirst("X-Rate-Limit-Remaining")).isEqualTo("1");

        ResponseEntity<String> second = restTemplate.exchange(
                "/herbaria/public", HttpMethod.GET, noAuth(), String.class
        );
        assertThat(second.getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(second.getHeaders().getFirst("X-Rate-Limit-Remaining")).isEqualTo("0");

        ResponseEntity<String> blocked = restTemplate.exchange(
                "/herbaria/public", HttpMethod.GET, noAuth(), String.class
        );
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getHeaders().getFirst("Retry-After")).isNotNull();
    }
}
