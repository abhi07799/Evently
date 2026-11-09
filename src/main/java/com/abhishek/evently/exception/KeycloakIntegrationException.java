package com.abhishek.evently.exception;

// Represents failures while communicating with Keycloak.
public class KeycloakIntegrationException extends RuntimeException {

    public KeycloakIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}