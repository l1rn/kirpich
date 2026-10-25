package org.kirpich.smart_profile.dto.application;

import org.kirpich.smart_profile.models.Application;
import org.kirpich.smart_profile.models.Verification;

import java.time.LocalDateTime;

public record ApplicationResponse(
        Long id,
        String status,
        String companyName,
        String productTitle,
        Long requestedAmount,
        Integer termMonths,
        String bkiStatus,
        String egrulStatus,
        String reasons,
        LocalDateTime createdAt) {

    public static ApplicationResponse from(Application a) {
        Verification v = a.getVerification();
        return new ApplicationResponse(
                a.getId(),
                a.getStatus().name(),
                a.getCompany().getName(),
                a.getCreditProduct().getTitle(),
                a.getRequestedAmount(),
                a.getTermMonths(),
                v.getBkiStatus().name(),
                v.getEgrulStatus().name(),
                v.getReasons(),
                a.getCreatedAt());
    }
}
