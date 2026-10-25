package org.kirpich.smart_profile.domain.sources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Выписка ЕГРЮЛ/ЕГРИП (контракт упрощён, в демо отдаётся WireMock). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EgrulCompany(
        String inn,
        String ogrn,
        String fullName,
        String shortName,
        /** OOO или IP. */
        String legalForm,
        /** ACTIVE, LIQUIDATING, LIQUIDATED, BANKRUPT. */
        String status,
        String registrationDate,
        String address,
        Okved okved,
        Director director,
        List<Founder> founders) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Okved(String code, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Director(String fullName, String inn, boolean disqualified) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Founder(String name, double sharePercent) {
    }
}
