package com.abhishek.evently.dto.response;

import com.abhishek.evently.entity.enums.UserRoleEnum;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO
{
    private Long id;

    private String fullName;

    private String email;

    private String keycloakId;

    private UserRoleEnum userRole;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
