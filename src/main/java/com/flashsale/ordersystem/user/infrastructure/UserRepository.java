package com.flashsale.ordersystem.user.infrastructure;

import com.flashsale.ordersystem.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKeycloakId(String keycloakId);

    boolean existsByEmail(String email);
}
