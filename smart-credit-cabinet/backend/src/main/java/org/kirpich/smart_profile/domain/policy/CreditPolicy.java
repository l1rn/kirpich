package org.kirpich.smart_profile.domain.policy;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Кредитная политика из credit-policy.yml: продукты, цели, документы и параметры скоринга. */
@ConfigurationProperties(prefix = "credit")
public record CreditPolicy(
        String version,
        List<Option> purposes,
        List<Option> documents,
        List<Product> products,
        ScoringPolicy scoring) {

    public Optional<Product> findProduct(String code) {
        return products.stream().filter(p -> p.code().equals(code)).findFirst();
    }

    public Product product(String code) {
        return findProduct(code).orElseThrow(() -> new IllegalArgumentException("Неизвестный продукт: " + code));
    }

    public Optional<String> purposeLabel(String code) {
        return purposes.stream().filter(o -> o.code().equals(code)).map(Option::label).findFirst();
    }

    public Optional<String> documentLabel(String code) {
        return documents.stream().filter(o -> o.code().equals(code)).map(Option::label).findFirst();
    }
}
