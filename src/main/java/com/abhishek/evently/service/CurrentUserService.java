package com.abhishek.evently.service;

import com.abhishek.evently.entity.UserEntity;
import com.abhishek.evently.exception.ResourceNotFoundException;
import com.abhishek.evently.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentUserService {

    private final UserRepository userRepository;

    /**
     * Resolves the currently authenticated Keycloak user
     * to their corresponding Evently database profile.
     */
    public UserEntity getCurrentUser(Jwt jwt) {

        // Keycloak UUID is stored in the JWT subject (sub) claim.
        String keycloakUserId = jwt.getSubject();

        return userRepository.findByKeycloakId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user profile not found"
                ));
    }
}