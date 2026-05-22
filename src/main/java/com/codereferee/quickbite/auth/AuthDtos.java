package com.codereferee.quickbite.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record SignupRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 2, max = 120) String displayName,
            @NotBlank @Size(min = 10, max = 120) String password
    ) {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, Instant expiresAt, UserView user) {
    }

    public record UserView(Long id, String email, String displayName, String role) {
    }
}
