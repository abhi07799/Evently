package com.abhishek.evently.config;



import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI eventlyOpenAPI() {

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("keycloak",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.OAUTH2)
                                        .flows(new OAuthFlows()
                                                .authorizationCode(new OAuthFlow()
                                                        .authorizationUrl(
                                                                "http://localhost:9090/realms/Evently/protocol/openid-connect/auth"
                                                        )
                                                        .tokenUrl(
                                                                "http://localhost:9090/realms/Evently/protocol/openid-connect/token"
                                                        )
                                                        .scopes(new Scopes()
                                                                .addString("openid", "OpenID Connect")
                                                                .addString("profile", "User profile")
                                                                .addString("email", "User email")
                                                        )
                                                )
                                        )
                        )
                )
                // Apply OAuth2 security to documented endpoints by default.
                .addSecurityItem(new SecurityRequirement().addList("keycloak"));
    }
}
