package org.kirpich.smart_profile.repository;

import java.util.Collection;
import java.util.List;

import org.kirpich.smart_profile.domain.ApplicationStatus;
import org.kirpich.smart_profile.model.CreditApplication;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditApplicationRepository extends JpaRepository<CreditApplication, Long> {

    List<CreditApplication> findByCompanyIdOrderByIdDesc(Long companyId);

    List<CreditApplication> findByStatusInOrderByUpdatedAtDesc(Collection<ApplicationStatus> statuses);

    @Query("select a.id from CreditApplication a where a.status = :status order by a.id")
    List<Long> findIdsByStatus(@Param("status") ApplicationStatus status, Pageable page);
}
