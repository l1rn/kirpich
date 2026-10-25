package org.kirpich.smart_profile.repository;

import java.util.Optional;

import org.kirpich.smart_profile.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByInn(String inn);
}
