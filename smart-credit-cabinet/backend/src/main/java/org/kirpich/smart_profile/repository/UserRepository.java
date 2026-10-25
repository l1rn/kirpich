package org.kirpich.smart_profile.repository;

import java.util.Optional;

import org.kirpich.smart_profile.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByLogin(String login);
}
