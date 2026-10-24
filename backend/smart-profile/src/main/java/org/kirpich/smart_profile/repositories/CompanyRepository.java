package org.kirpich.smart_profile.repositories;

import org.kirpich.smart_profile.models.Company;
import org.kirpich.smart_profile.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {
    @Query("select c from Company c")
    Page<Company> findAllPageable(Pageable pageable);

    Optional<Company> findByOwner(User owner);

    Optional<Company> findByOwner_Email(String ownerEmail);
}
