package com.abhishek.evently.dto.request;

import com.abhishek.evently.entity.enums.UserRoleEnum;
import jakarta.validation.constraints.*;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO
{
    @NotBlank(message = "Full name is required")
    @NotNull
    @NotEmpty
    @Size(max = 200)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    @NotNull
    private UserRoleEnum userRole;
}
