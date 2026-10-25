package org.kirpich.smart_profile.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.kirpich.smart_profile.domain.scoring.ScoringEngine.FactorResult;
import org.kirpich.smart_profile.service.ApplicationWorkflow.NextAction;
import org.kirpich.smart_profile.service.InfoItem;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    public record ApplicationRequest(
            @NotBlank String productCode,
            @NotBlank String purpose,
            @NotNull @Positive Long amount,
            @NotNull @Positive @Max(120) Integer termMonths,
            @PositiveOrZero Long revenue12m,
            @PositiveOrZero Long existingMonthlyPayments) {
    }

    /** Частичное обновление черновика: null — поле не меняется. */
    public record ApplicationPatch(
            String productCode,
            String purpose,
            @Positive Long amount,
            @Positive @Max(120) Integer termMonths,
            @PositiveOrZero Long revenue12m,
            @PositiveOrZero Long existingMonthlyPayments) {
    }

    public record SubmitRequest(boolean bkiConsent) {
    }

    public record InfoResponseRequest(Map<String, String> answers) {
    }

    public record SignRequest(@NotBlank String code) {
    }

    /** В демо код подтверждения показывается на экране; в банке он пришёл бы в push внутри приложения. */
    public record AcceptOfferResponse(String signingCode, String message) {
    }

    public record SummaryView(
            Long id,
            String number,
            String status,
            String statusLabel,
            String productCode,
            String productName,
            String purpose,
            String purposeLabel,
            long amount,
            int termMonths,
            Instant createdAt,
            Instant updatedAt,
            NextAction nextAction) {
    }

    public record DetailView(
            SummaryView summary,
            int stageIndex,
            List<String> stages,
            Long revenue12m,
            long existingMonthlyPayments,
            Instant bkiConsentAt,
            Instant submittedAt,
            Instant decidedAt,
            List<CheckView> checks,
            OfferView offer,
            InfoRequestView infoRequest,
            List<DocumentView> documents,
            List<EventView> events,
            ClientDecisionView decision) {
    }

    public record CheckView(String step, String label, String status, int attempts, String error, Instant updatedAt) {
    }

    public record EventView(Long id, String type, String fromStatus, String toStatus, String actorType, String message, Instant createdAt) {
    }

    public record OfferView(
            Long id,
            String kind,
            String productCode,
            String productName,
            long amount,
            int termMonths,
            double rate,
            long monthlyPayment,
            String status,
            Instant createdAt,
            Instant acceptedAt,
            Instant signedAt) {
    }

    public record InfoRequestView(
            Long id,
            String requestedBy,
            List<InfoItem> items,
            String note,
            String status,
            Map<String, String> answers,
            Instant createdAt,
            Instant answeredAt) {
    }

    public record DocumentView(Long id, String docType, String docLabel, String filename, long sizeBytes, Instant createdAt) {
    }

    /** Объяснение решения для клиента: причины и факторы скоринга. */
    public record ClientDecisionView(String outcome, List<String> reasons, Integer score, String grade, List<FactorResult> factors) {
    }
}
