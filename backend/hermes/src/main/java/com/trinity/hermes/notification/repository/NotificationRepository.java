package com.trinity.hermes.notification.repository;

import com.trinity.hermes.notification.entity.NotificationEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

  /** Active (not soft-deleted) notifications for a user, newest first. */
  List<NotificationEntity> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(String userId);

  /** Soft-deleted (bin) notifications for a user, newest first. */
  List<NotificationEntity> findByUserIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(String userId);

  /** Count unread, active notifications for a user (drives badge counter). */
  long countByUserIdAndIsReadFalseAndDeletedAtIsNull(String userId);

  Optional<NotificationEntity> findByIdAndUserId(Long id, String userId);

  /** Hard-delete bin entries older than 30 days. */
  @Modifying
  @Query("DELETE FROM NotificationEntity n WHERE n.deletedAt IS NOT NULL AND n.deletedAt < :cutoff")
  void hardDeleteExpiredBinEntries(LocalDateTime cutoff);
}
