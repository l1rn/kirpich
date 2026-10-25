package org.kirpich.smart_profile.web;

import java.util.List;

import org.kirpich.smart_profile.domain.InnValidator;
import org.kirpich.smart_profile.domain.sources.EgrulCompany;
import org.kirpich.smart_profile.integration.RegistryGateway;
import org.kirpich.smart_profile.integration.SourceNotFoundException;
import org.kirpich.smart_profile.integration.SourceUnavailableException;
import org.kirpich.smart_profile.service.CatalogService;
import org.kirpich.smart_profile.service.NotFoundException;
import org.kirpich.smart_profile.service.ValidationFailedException;
import org.kirpich.smart_profile.web.dto.CatalogDtos.CatalogView;
import org.kirpich.smart_profile.web.dto.CatalogDtos.RecommendationRequest;
import org.kirpich.smart_profile.web.dto.CatalogDtos.RecommendationView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Продукты и подбор")
public class CatalogController {

    private final CatalogService catalog;
    private final RegistryGateway registry;

    public CatalogController(CatalogService catalog, RegistryGateway registry) {
        this.catalog = catalog;
        this.registry = registry;
    }

    @GetMapping("/products")
    public CatalogView products() {
        return catalog.catalog();
    }

    @PostMapping("/products/recommendations")
    public List<RecommendationView> recommend(@Valid @RequestBody RecommendationRequest request) {
        return catalog.recommend(request);
    }

    /** Автозаполнение анкеты: сведения о компании из ЕГРЮЛ/ЕГРИП по ИНН. */
    @GetMapping("/companies/lookup")
    public EgrulCompany lookup(@RequestParam String inn) {
        if (!InnValidator.isValid(inn)) {
            throw new ValidationFailedException("ИНН указан с ошибкой — проверьте цифры");
        }
        try {
            return registry.egrulCompany(inn);
        } catch (SourceNotFoundException e) {
            throw new NotFoundException("Компания с ИНН %s не найдена в ЕГРЮЛ/ЕГРИП".formatted(inn));
        } catch (SourceUnavailableException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ЕГРЮЛ временно недоступен, попробуйте позже");
        }
    }
}
