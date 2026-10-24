package org.kirpich.smart_profile.controllers;

import lombok.RequiredArgsConstructor;
import org.kirpich.smart_profile.dto.application.CreateApplicationRequest;
import org.kirpich.smart_profile.exceptions.UserHasNoCompanyException;
import org.kirpich.smart_profile.services.ApplicationService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.CredentialNotFoundException;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;

    @PostMapping("/create")
    public ResponseEntity<?> create(
            @RequestBody CreateApplicationRequest request,
            @AuthenticationPrincipal UserDetails userDetails){
        try {
            applicationService.createApplication(request, userDetails.getUsername());
            return ResponseEntity.ok("OK");
        }
        catch (IllegalArgumentException | UserHasNoCompanyException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> my(@AuthenticationPrincipal UserDetails userDetails, Pageable p){
        try {
            return ResponseEntity.ok(applicationService.getMyApplicationList(userDetails.getUsername(), p));
        }
        catch (IllegalArgumentException | CredentialNotFoundException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public ResponseEntity<?> all(Pageable p){
        try {
            return ResponseEntity.ok(applicationService.getApplicationList(p));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
