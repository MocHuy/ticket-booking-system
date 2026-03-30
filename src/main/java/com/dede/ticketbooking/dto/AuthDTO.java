package com.dede.ticketbooking.dto;

import lombok.*;
import jakarta.validation.constraints.*;

public class AuthDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;
        @NotBlank(message = "Password is required")
        private String password;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank @Size(min = 3, max = 50) private String username;
        @NotBlank @Size(min = 6) private String password;
        @Email @NotBlank private String email;
        @NotBlank private String fullName;
        private String phoneNumber;
        private String gender;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AuthResponse {
        private String accessToken;
        private String tokenType;
        private Long userId;
        private String username;
        private String fullName;
        private String role;
    }
}
