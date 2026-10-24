package org.kirpich.smart_profile.services;

import lombok.RequiredArgsConstructor;
import org.kirpich.smart_profile.dto.PaginatedResponse;
import org.kirpich.smart_profile.dto.application.ApplicationResponse;
import org.kirpich.smart_profile.dto.application.CreateApplicationRequest;
import org.kirpich.smart_profile.enums.ApplicationStatus;
import org.kirpich.smart_profile.enums.FinalVerdict;
import org.kirpich.smart_profile.exceptions.UserHasNoCompanyException;
import org.kirpich.smart_profile.models.*;
import org.kirpich.smart_profile.repositories.ApplicationRepository;
import org.kirpich.smart_profile.repositories.CompanyRepository;
import org.kirpich.smart_profile.repositories.CreditProductRepository;
import org.kirpich.smart_profile.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.security.auth.login.CredentialNotFoundException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CreditProductRepository creditProductRepository;
    private final VerificationService verificationService;

    public void createApplication(CreateApplicationRequest request, String email){
        Company company = companyRepository.findByOwner_Email(email)
                .orElseThrow(() -> new UserHasNoCompanyException("Для начало юзеру нужно создать компанию"));

        CreditProduct product = creditProductRepository.findById(request.getCreditProductId())
                .filter(CreditProduct::getActive)
                .orElseThrow(() -> new IllegalArgumentException("Кредитный тариф не найден"));

        if(request.getRequestedAmount() < product.getMinAmount() || request.getRequestedAmount() > product.getMaxAmount()){
            throw new IllegalArgumentException("Сумма кредита должна быть между"
                    + product.getMinAmount() + " и " + product.getMaxAmount());
        }

        if(request.getTermMonths() < product.getMinTermMonths() || request.getTermMonths() > product.getMaxTermMonths()){
            throw new IllegalArgumentException("Срок кредит должен находиться от "
                + product.getMinTermMonths() + " и " + product.getMaxTermMonths());
        }

        Verification v = verificationService.createVerification(company, request.getRequestedAmount());

        Application app = applicationRepository.save(Application.builder()
                        .company(company)
                        .creditProduct(product)
                        .requestedAmount(request.getRequestedAmount())
                        .termMonths(request.getTermMonths())
                        .verification(v)
                        .status(v.getFinalVerdict() == FinalVerdict.VERIFICATION_APPROVED ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED)
                        .build());
    }

    public PaginatedResponse<ApplicationResponse> getMyApplicationList(String email, Pageable p) throws CredentialNotFoundException{
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CredentialNotFoundException("Пользователь с таким email не найден"));

        Page<Application> applications = applicationRepository.findAllByCompany_Owner(user, p);
        List<ApplicationResponse> content = applications.stream()
                .map(ApplicationResponse::from)
                .toList();

        return new PaginatedResponse<>(
                content,
                applications.getNumber(),
                applications.getSize(),
                applications.getTotalElements(),
                applications.getTotalPages()
        );
    }

    public PaginatedResponse<ApplicationResponse> getApplicationList(Pageable p){
        Page<Application> applications = applicationRepository.findAllPageable(p);

        List<ApplicationResponse> content = applications.stream()
                .filter(a -> a.getCompany() != null)
                .filter(a -> a.getVerification() != null)
                .map(ApplicationResponse::from)
                .toList();

        return new PaginatedResponse<>(
            content,
            applications.getNumber(),
            applications.getSize(),
            applications.getTotalElements(),
            applications.getTotalPages()
        );
    }

}
