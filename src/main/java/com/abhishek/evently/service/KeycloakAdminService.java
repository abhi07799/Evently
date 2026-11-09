package com.abhishek.evently.service;

import com.abhishek.evently.entity.enums.UserRoleEnum;
import com.abhishek.evently.exception.KeycloakIntegrationException;
import com.abhishek.evently.exception.ResourceAlreadyExistException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakAdminService
{

    private final RestClient restClient;

    public KeycloakAdminService(RestClient.Builder restClientBuilder)
    {
        this.restClient = restClientBuilder.build();
    }

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;



    public String createUser(
            String email,
            String fullName,
            String password,
            UserRoleEnum role) {

        String accessToken;

        // Obtain the service-account token before starting user creation.
        try {
            accessToken = getAdminAccessToken();
        } catch (RestClientException exception) {
            throw new KeycloakIntegrationException(
                    "Unable to authenticate with Keycloak", exception);
        }

        String userId = null;

        try {
            // Use the email prefix as the Keycloak username.
            String userName = email.substring(0, email.indexOf('@'));

            // Create the user in Keycloak.
            Map<String, Object> userPayload = Map.of(
                    "username", userName,
                    "email", email,
                    "enabled", true,
                    "emailVerified", false
            );

            ResponseEntity<Void> response;

            try {
                response = restClient.post()
                        .uri(serverUrl + "/admin/realms/" + realm + "/users")
                        .headers(headers -> headers.setBearerAuth(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(userPayload)
                        .retrieve()
                        .toBodilessEntity();

            } catch (RestClientResponseException exception) {

                // Keycloak HTTP 409 indicates an existing username or email.
                if (exception.getStatusCode().value() == 409) {
                    throw new ResourceAlreadyExistException(
                            "A user with this username or email already exists",
                            exception
                    );
                }

                throw exception;
            }

            // Extract the newly created Keycloak user's UUID.
            URI location = response.getHeaders().getLocation();

            if (location == null) {
                throw new KeycloakIntegrationException(
                        "Keycloak did not return the created user ID", null);
            }

            String path = location.getPath();
            userId = path.substring(path.lastIndexOf('/') + 1);

            // Set the user's password directly in Keycloak.
            Map<String, Object> passwordPayload = Map.of(
                    "type", "password",
                    "value", password,
                    "temporary", false
            );

            restClient.put()
                    .uri(serverUrl + "/admin/realms/" + realm
                            + "/users/" + userId + "/reset-password")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(passwordPayload)
                    .retrieve()
                    .toBodilessEntity();

            // Fetch the selected realm role representation.
            Map<String, Object> roleRepresentation = restClient.get()
                    .uri(serverUrl + "/admin/realms/" + realm
                            + "/roles/" + role.name())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (roleRepresentation == null) {
                throw new KeycloakIntegrationException(
                        "Keycloak role not found: " + role.name(), null);
            }

            // Assign exactly the selected realm role to this user.
            restClient.post()
                    .uri(serverUrl + "/admin/realms/" + realm
                            + "/users/" + userId + "/role-mappings/realm")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(roleRepresentation))
                    .retrieve()
                    .toBodilessEntity();

            return userId;

        } catch (RuntimeException exception) {

            // Remove partially created users if password or role setup fails.
            if (userId != null) {
                try {
                    restClient.delete()
                            .uri(serverUrl + "/admin/realms/" + realm
                                    + "/users/" + userId)
                            .headers(headers -> headers.setBearerAuth(accessToken))
                            .retrieve()
                            .toBodilessEntity();

                } catch (RuntimeException cleanupException) {
                    // Preserve cleanup failure for diagnostics.
                    exception.addSuppressed(cleanupException);
                }
            }

            // Preserve meaningful application exceptions.
            if (exception instanceof ResourceAlreadyExistException
                    || exception instanceof KeycloakIntegrationException) {
                throw exception;
            }

            // Convert Keycloak HTTP and connectivity failures into HTTP 502.
            if (exception instanceof RestClientException) {
                throw new KeycloakIntegrationException(
                        "Keycloak operation failed", exception);
            }

            throw exception;
        }
    }


    // Obtains a short-lived admin token using the service account.
    private String getAdminAccessToken()
    {

        MultiValueMap<String, String> formData =
                new LinkedMultiValueMap<>();

        formData.add("grant_type", "client_credentials");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);

        KeycloakTokenResponse tokenResponse = restClient.post()
                .uri(serverUrl + "/realms/" + realm
                        + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(KeycloakTokenResponse.class);

        if (tokenResponse == null || tokenResponse.accessToken() == null)
        {
            throw new IllegalStateException(
                    "Unable to obtain Keycloak admin access token");
        }

        return tokenResponse.accessToken();
    }

    // Maps Keycloak's snake_case JSON response to a Java record.
    private record KeycloakTokenResponse(
            @JsonProperty("access_token") String accessToken)
    {
    }

    // Deletes a Keycloak user if Evently database persistence fails.
    public void deleteUser(String userId) {
        String accessToken = getAdminAccessToken();

        restClient.delete()
                .uri(serverUrl + "/admin/realms/" + realm + "/users/" + userId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .toBodilessEntity();
    }
}
