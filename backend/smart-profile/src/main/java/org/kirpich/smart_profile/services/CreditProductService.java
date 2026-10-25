package org.kirpich.smart_profile.services;

import lombok.RequiredArgsConstructor;
import org.kirpich.smart_profile.dto.PaginatedResponse;
import org.kirpich.smart_profile.dto.credit_products.CreateCreditProductRequest;
import org.kirpich.smart_profile.dto.credit_products.CreditProductResponse;
import org.kirpich.smart_profile.models.CreditProduct;
import org.kirpich.smart_profile.repositories.CreditProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditProductService {
    private final CreditProductRepository creditProductRepository;

    public void createCreditProduct(CreateCreditProductRequest request){
        CreditProduct creditProduct = CreditProduct.builder()
                .title(request.getTitle())
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .minTermMonths(request.getMinTermMonths())
                .maxTermMonths(request.getMaxTermMonths())
                .interestRate(request.getInterestRate())
                .build();
        creditProductRepository.save(creditProduct);
    }

    public CreditProductResponse getCreditProduct(Integer id){
        return creditProductRepository.findById(id)
                .filter(CreditProduct::getActive)
                .map(CreditProductResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Продукт не найден"));
    }

    public PaginatedResponse<CreditProductResponse> getCreditProducts(Pageable p){
        Page<CreditProduct> creditProducts = creditProductRepository.findAllPageable(p);

        List<CreditProductResponse> content = creditProducts.stream()
                .filter(c -> c.getTitle() != null)
                .map(CreditProductResponse::from)
                .toList();

        return new PaginatedResponse<>(
                content,
                creditProducts.getNumber(),
                creditProducts.getSize(),
                creditProducts.getTotalElements(),
                creditProducts.getTotalPages()
        );
    }
}
