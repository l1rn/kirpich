package org.kirpich.smart_profile.repositories;

import org.kirpich.smart_profile.models.Company;
import org.kirpich.smart_profile.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {
    @Query("select c from Company c")
    Page<Company> findAllPageable(Pageable pageable);

    Company findByOwner(User owner);
}
