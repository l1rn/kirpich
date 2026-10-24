package org.kirpich.smart_profile.controllers;

import lombok.RequiredArgsConstructor;
import org.kirpich.smart_profile.dto.company.ChangeCompanyRequest;
import org.kirpich.smart_profile.dto.company.CreateCompanyRequest;
import org.kirpich.smart_profile.services.CompanyService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.CredentialNotFoundException;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;

    @PostMapping("/create")
    public ResponseEntity<?> createCompany(
            @RequestBody CreateCompanyRequest request,
            @AuthenticationPrincipal UserDetails userDetails
        ){
        try{
            companyService.createCompany(request, userDetails.getUsername());
            return ResponseEntity.ok("OK");
        } catch (CredentialNotFoundException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/change")
    public ResponseEntity<?> changeCompanyInformation(
            @RequestBody ChangeCompanyRequest changeCompanyRequest,
            @AuthenticationPrincipal UserDetails userDetails){
        try {
            companyService.changeCompany(changeCompanyRequest, userDetails.getUsername());
            return ResponseEntity.ok("OK");
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        } catch (CredentialNotFoundException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public ResponseEntity<?> getCompanies(
            Pageable pageable
    ){
        try{
            return ResponseEntity.ok(companyService.getAllCompanies(pageable));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyCompany(@AuthenticationPrincipal UserDetails userDetails){
        try{
            return ResponseEntity.ok(companyService.getMyCompany(userDetails.getUsername()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        } catch (CredentialNotFoundException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<?> removeCompanyAdmin(
            @PathVariable(name = "id") int id
    ){
        try{
            companyService.removeCompany(id);
            return ResponseEntity.ok("OK");
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
