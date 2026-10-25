package org.kirpich.smart_profile.repository;

import org.kirpich.smart_profile.model.Decision;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionRepository extends JpaRepository<Decision, Long> {
}
