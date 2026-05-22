package com.ezielnik.api.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Value("${app.base-url}")
    private String baseUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("eZielnik API")
                        .version("1.0")
                        .description("REST API for the eZielnik flora identification app. Provides user authentication (JWT, email 2FA), herbarium and plant management, photo upload with identification via PlantNet, social features (friends, notifications), and admin panel.")
                        .contact(new Contact().name("Ezielnik API"))
                        .license(new License().name("Apache 2.0")))
                .servers(List.of(new Server().url(baseUrl)))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
