package org.kirpich.smart_profile.services;

import lombok.RequiredArgsConstructor;
import org.kirpich.smart_profile.dto.PaginatedResponse;
import org.kirpich.smart_profile.dto.application.ApplicationResponse;
import org.kirpich.smart_profile.dto.company.ChangeCompanyRequest;
import org.kirpich.smart_profile.dto.company.CompanyResponse;
import org.kirpich.smart_profile.dto.company.CreateCompanyRequest;
import org.kirpich.smart_profile.exceptions.UserHasNoCompanyException;
import org.kirpich.smart_profile.models.Application;
import org.kirpich.smart_profile.models.Company;
import org.kirpich.smart_profile.models.User;
import org.kirpich.smart_profile.repositories.CompanyRepository;
import org.kirpich.smart_profile.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.CredentialNotFoundException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public void createCompany(CreateCompanyRequest request, String email) throws CredentialNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CredentialNotFoundException("Пользователь с таким email не найден"));

        Company company = Company.builder()
                .name(request.getName())
                .inn(request.getInn())
                .legalAddress(request.getLegalAddress())
                .ogrn(request.getOgrn())
                .owner(user)
                .revenue(request.getRevenue())
                .build();

        companyRepository.save(company);
    }

    public PaginatedResponse<CompanyResponse> getAllCompanies(Pageable p){
        Page<Company> companies = companyRepository.findAllPageable(p);

        List<CompanyResponse> content = companies.stream()
                .filter(a -> a.getInn() != null)
                .filter(a -> a.getName() != null)
                .filter(a -> a.getOgrn() != null)
                .map(CompanyResponse::from)
                .toList();

        return new PaginatedResponse<>(
                content,
                companies.getNumber(),
                companies.getSize(),
                companies.getTotalElements(),
                companies.getTotalPages()
        );
    }

    public CompanyResponse getMyCompany(String email) throws CredentialNotFoundException{
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CredentialNotFoundException("Пользователь с таким email не найден"));
        Company company = companyRepository.findByOwner(user)
                .orElse(null);
        if(company == null){
            return null;
        }
        return new CompanyResponse(
                company.getName(),
                company.getInn(),
                company.getOgrn(),
                company.getLegalAddress(),
                company.getRevenue()
        );
    }

    @Transactional
    public void changeCompany(ChangeCompanyRequest request, String email) throws CredentialNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CredentialNotFoundException("Пользователь с таким email не найден"));

        Company company = companyRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Компания с таким id не найдена"));

        company.setName(request.getName());
        company.setInn(request.getInn());
        company.setLegalAddress(request.getLegalAddress());
        company.setOgrn(request.getOgrn());
        company.setOwner(user);
        company.setRevenue(request.getRevenue());

        companyRepository.save(company);
    }

    @Transactional
    public void removeCompany(Integer companyId){
        companyRepository.deleteById(companyId);
    }
}
