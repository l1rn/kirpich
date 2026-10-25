package org.kirpich.smart_profile.web.dto;

import java.time.Instant;
import java.util.List;

import org.kirpich.smart_profile.domain.decision.DecisionEngine.OfferProposal;
import org.kirpich.smart_profile.domain.scoring.ScoringEngine.FactorResult;
import org.kirpich.smart_profile.domain.scoring.ScoringEngine.StopFactor;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.DetailView;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.InfoRequestView;
import org.kirpich.smart_profile.web.dto.AuthDtos.CompanyView;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public final class BankDtos {

    private BankDtos() {
    }

    public enum BankOutcome {
        /** Одобрить на условиях заявки. */
        APPROVE,
        /** Одобрить на других условиях: пустые поля берутся из рекомендации движка. */
        COUNTER_OFFER,
        DECLINE
    }

    public record BankDecisionRequest(
            @NotNull BankOutcome outcome,
            String productCode,
            @Positive Long amount,
            @Positive Integer termMonths,
            @Positive Double rate,
            String reason,
            String note) {
    }

    public record BankInfoRequest(@NotEmpty List<@Valid Item> items, String note) {

        public record Item(@NotBlank String label, @NotNull @Pattern(regexp = "TEXT|FILE") String type) {
        }
    }

    public record RowView(
            Long id,
            String number,
            String status,
            String statusLabel,
            String companyName,
            String companyInn,
            String productCode,
            String productName,
            long amount,
            int termMonths,
            Integer score,
            String grade,
            String assignee,
            Instant updatedAt) {
    }

    public record BankDetailView(
            DetailView application,
            CompanyView company,
            String assignee,
            List<SourceView> sources,
            List<DecisionView> decisions,
            List<InfoRequestView> infoRequests,
            long version) {
    }

    /** Шаг конвейера с сырым ответом источника — андеррайтер видит, на каких данных принято решение. */
    public record SourceView(String step, String label, String status, int attempts, String error, Object data) {
    }

    public record DecisionView(
            Long id,
            String source,
            String outcome,
            Integer score,
            String grade,
            List<FactorResult> factors,
            List<StopFactor> stopFactors,
            List<String> reasons,
            OfferProposal proposal,
            String note,
            String policyVersion,
            Instant createdAt) {
    }
}
