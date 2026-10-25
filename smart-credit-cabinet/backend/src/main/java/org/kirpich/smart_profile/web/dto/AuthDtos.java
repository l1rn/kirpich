package org.kirpich.smart_profile.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank String login, @NotBlank String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, UserView user) {
    }

    public record UserView(Long id, String login, String fullName, String role, CompanyView company) {
    }

    public record CompanyView(
            Long id,
            String inn,
            String name,
            String legalForm,
            String ogrn,
            String okved,
            String directorName,
            LocalDate registeredAt) {
    }
}
