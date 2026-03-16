package com.trinity.hermes.usermanagement.repository;

import com.trinity.hermes.usermanagement.entity.PasswordResetTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository
    extends JpaRepository<PasswordResetTokenEntity, Long> {

  Optional<PasswordResetTokenEntity> findByToken(String token);

  void deleteByToken(String token);

  void deleteByKeycloakUserId(String keycloakUserId);
}
