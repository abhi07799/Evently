package com.abhishek.evently.controller;

import com.abhishek.evently.dto.request.UserRequestDTO;
import com.abhishek.evently.dto.response.UserResponseDTO;
import com.abhishek.evently.service.UserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserRegistrationService userRegistrationService;

    // Public endpoint for creating a new Evently account.
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> registerUser(
            @Valid @RequestBody UserRequestDTO request) {

        UserResponseDTO response =
                userRegistrationService.registerUser(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}