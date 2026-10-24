package org.kirpich.smart_profile.controllers;

import lombok.RequiredArgsConstructor;
import org.kirpich.smart_profile.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.security.auth.login.CredentialNotFoundException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getMeInfo(
            @AuthenticationPrincipal UserDetails userDetails){
        try{
            return ResponseEntity.ok(userService.getMeInfo(userDetails));
        } catch(CredentialNotFoundException e){
            return ResponseEntity.status(403).body("Forbidden");
        }
    }
}
