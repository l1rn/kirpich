package org.kirpich.smart_profile.web;

import org.kirpich.smart_profile.repository.UserRepository;
import org.kirpich.smart_profile.security.CurrentUser;
import org.kirpich.smart_profile.security.JwtService;
import org.kirpich.smart_profile.web.dto.AuthDtos.LoginRequest;
import org.kirpich.smart_profile.web.dto.AuthDtos.TokenResponse;
import org.kirpich.smart_profile.web.dto.AuthDtos.UserView;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Авторизация")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final CurrentUser currentUser;
    private final ApplicationViews views;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwt, CurrentUser currentUser,
            ApplicationViews views) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.currentUser = currentUser;
        this.views = views;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return users.findByLogin(request.login())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .map(user -> new TokenResponse(jwt.issue(user), "Bearer", views.user(user)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль"));
    }

    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal Jwt token) {
        return views.user(currentUser.from(token));
    }
}
