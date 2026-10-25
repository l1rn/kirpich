package org.kirpich.smart_profile.web.dto;

import java.util.List;

import org.kirpich.smart_profile.domain.policy.Option;
import org.kirpich.smart_profile.domain.policy.Product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CatalogView(List<Product> products, List<Option> purposes, List<Option> documents) {
    }

    public record RecommendationRequest(@NotBlank String purpose, @Positive long amount, boolean urgent) {
    }

    public record RecommendationView(Product product, boolean suitable, List<String> reasons, Long estimatedMonthlyPayment) {
    }
}
