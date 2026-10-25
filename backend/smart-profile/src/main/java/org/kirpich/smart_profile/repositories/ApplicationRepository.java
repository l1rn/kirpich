package org.kirpich.smart_profile.repositories;

import org.kirpich.smart_profile.models.Application;
import org.kirpich.smart_profile.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    @Query("select a from Application a")
    Page<Application> findAllPageable(Pageable pageable);

    Page<Application> findAllByCompany_Owner_Email(String companyOwnerEmail, Pageable pageable);
}
