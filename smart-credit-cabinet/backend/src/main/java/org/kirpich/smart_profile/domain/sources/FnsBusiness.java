package org.kirpich.smart_profile.domain.sources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Сведения сервиса ФНС «Прозрачный бизнес» (контракт упрощён). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FnsBusiness(
        String inn,
        boolean taxDebt,
        long taxDebtAmount,
        boolean massAddress,
        boolean massDirector,
        int employees,
        String taxRegime,
        ReportedRevenue reportedRevenue) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReportedRevenue(int year, long amount) {
    }
}
