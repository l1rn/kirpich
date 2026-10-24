package org.kirpich.smart_profile.services;

import lombok.RequiredArgsConstructor;
import org.kirpich.smart_profile.dto.PaginatedResponse;
import org.kirpich.smart_profile.dto.application.ApplicationResponse;
import org.kirpich.smart_profile.models.Application;
import org.kirpich.smart_profile.models.User;
import org.kirpich.smart_profile.repositories.ApplicationRepository;
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

    public void createApplication(Application application){

    }

    public PaginatedResponse<ApplicationResponse> getMyApplicationList(Pageable p, String email) throws CredentialNotFoundException{
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
