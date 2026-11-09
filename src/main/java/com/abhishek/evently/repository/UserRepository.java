package com.abhishek.evently.repository;

import com.abhishek.evently.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long>
{

    // Find a user using their Keycloak identity.
    Optional<UserEntity> findByKeycloakId(String keycloakId);

    // Check email uniqueness before registration.
    boolean existsByEmailIgnoreCase(String email);
}
