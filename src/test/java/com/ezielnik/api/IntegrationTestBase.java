package com.ezielnik.api;

import com.ezielnik.api.auth.EmailService;
import com.ezielnik.api.auth.LoginRequest;
import com.ezielnik.api.auth.RegisterRequest;
import com.ezielnik.api.auth.RegisterResponse;
import com.ezielnik.api.auth.LoginResponse;
import com.ezielnik.api.photo.PhotoStorageService;
import com.ezielnik.api.plant.PlantIdentificationService;
import com.ezielnik.api.user.User;
import com.ezielnik.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", postgres::getJdbcUrl);
        registry.add("DB_USERNAME", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
    }

    @MockitoBean
    protected EmailService emailService;

    @MockitoBean
    protected PlantIdentificationService plantIdentificationService;

    @MockitoBean
    protected PhotoStorageService photoStorageService;

    protected RestTemplate restTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected UserRepository userRepository;

    @LocalServerPort
    protected int port;

    @BeforeEach
    void resetDatabase() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
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

        jdbcTemplate.execute(
                "TRUNCATE TABLE plant_photos, plants, herbaria, friendships, " +
                "notifications, two_factor_codes, refresh_tokens, device_tokens, users CASCADE"
        );

        Mockito.doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());
        Mockito.doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());
        Mockito.doNothing().when(emailService).sendTwoFactorCode(anyString(), anyString());
        Mockito.doNothing().when(emailService).sendAdminWarningEmail(anyString(), anyString(), anyString());

        Mockito.when(plantIdentificationService.identify(any()))
                .thenReturn(PlantIdentificationService.IdentificationResult.empty());

        Mockito.when(photoStorageService.save(any()))
                .thenAnswer(inv -> "/photos/test-" + UUID.randomUUID() + ".jpeg");
        Mockito.when(photoStorageService.savePending(anyString(), any()))
                .thenAnswer(inv -> "pending-" + inv.getArgument(0) + ".jpeg");
        Mockito.when(photoStorageService.moveToPermanent(anyString()))
                .thenAnswer(inv -> "/photos/test-" + UUID.randomUUID() + ".jpeg");
        Mockito.doNothing().when(photoStorageService).delete(anyString());
        Mockito.doNothing().when(photoStorageService).deletePendingFile(anyString());
    }

    protected RegisterResponse register(String username, String email, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setEmail(email);
        req.setPassword(password);
        ResponseEntity<RegisterResponse> resp = restTemplate.postForEntity("/users/register", req, RegisterResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    protected void verifyUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setVerified(true);
        userRepository.save(user);
    }

    protected User registerAndVerify(String username, String email, String password) {
        RegisterResponse resp = register(username, email, password);
        verifyUser(resp.getId());
        return userRepository.findById(resp.getId()).orElseThrow();
    }

    protected LoginResponse login(String login, String password) {
        LoginRequest req = new LoginRequest();
        req.setLogin(login);
        req.setPassword(password);
        ResponseEntity<LoginResponse> resp = restTemplate.postForEntity("/users/login", req, LoginResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    protected String loginAndGetToken(String login, String password) {
        return login(login, password).getToken();
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    protected <T> HttpEntity<T> withAuth(T body, String token) {
        return new HttpEntity<>(body, authHeaders(token));
    }

    protected HttpEntity<Void> withAuth(String token) {
        return new HttpEntity<>(authHeaders(token));
    }

    protected HttpEntity<Void> noAuth() {
        return new HttpEntity<>(null, HttpHeaders.EMPTY);
    }
}
