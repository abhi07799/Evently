package com.abhishek.evently.service;

import com.abhishek.evently.dto.request.UserRequestDTO;
import com.abhishek.evently.dto.response.UserResponseDTO;
import com.abhishek.evently.entity.UserEntity;
import com.abhishek.evently.exception.ResourceAlreadyExistException;
import com.abhishek.evently.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;

    // Coordinates identity creation in Keycloak and profile persistence in Evently.
    public UserResponseDTO registerUser(
            UserRequestDTO request) {

        // Check email before making any external Keycloak API calls.
        // Reject duplicate email before creating a Keycloak identity.
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ResourceAlreadyExistException(
                    "An account with this email already exists");
        }

        // Create the identity, password and selected role in Keycloak.
        String keycloakUserId = keycloakAdminService.createUser(
                request.getEmail(),
                request.getFullName(),
                request.getPassword(),
                request.getUserRole()
        );

        try {
            // Build the Evently profile; password is intentionally excluded.
            UserEntity user = new UserEntity();

            user.setKeycloakId(keycloakUserId);
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());

            // Flush immediately so database errors can trigger compensation.
            UserEntity savedUser = userRepository.saveAndFlush(user);

            // Return safe user details; never expose the password.
            return UserResponseDTO.builder()
                    .id(savedUser.getId())
                    .keycloakId(savedUser.getKeycloakId())
                    .fullName(savedUser.getFullName())
                    .email(savedUser.getEmail())
                    .userRole(request.getUserRole())
                    .build();

        } catch (RuntimeException exception) {

            // Remove the Keycloak identity if Evently profile persistence fails.
            try {
                keycloakAdminService.deleteUser(keycloakUserId);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            // Convert database uniqueness violations into a meaningful conflict response.
            if (exception instanceof DataIntegrityViolationException) {
                throw new ResourceAlreadyExistException(
                        "An account with this email or identity already exists",
                        exception
                );
            }

            throw exception;
        }
    }
}
