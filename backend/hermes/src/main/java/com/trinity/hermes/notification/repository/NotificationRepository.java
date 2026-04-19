package com.trinity.hermes.notification.repository;

import com.trinity.hermes.notification.entity.NotificationEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

  /** Active (not soft-deleted) notifications for a user, newest first. */
  List<NotificationEntity> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(String userId);

  /** Paginated active notifications for a user, newest first. */
  Page<NotificationEntity> findByUserIdAndDeletedAtIsNull(String userId, Pageable pageable);

  /** Soft-deleted (bin) notifications for a user, newest first. */
  List<NotificationEntity> findByUserIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(String userId);

  /** Count unread, active notifications for a user (drives badge counter). */
  long countByUserIdAndIsReadFalseAndDeletedAtIsNull(String userId);

  Optional<NotificationEntity> findByIdAndUserId(Long id, String userId);

  /** Mark all unread active notifications as read for a user. */
  @Modifying
  @Query(
      "UPDATE NotificationEntity n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false AND n.deletedAt IS NULL")
  int markAllAsReadByUserId(String userId);

  /** Hard-delete bin entries older than 30 days. */
  @Modifying
  @Query("DELETE FROM NotificationEntity n WHERE n.deletedAt IS NOT NULL AND n.deletedAt < :cutoff")
  void hardDeleteExpiredBinEntries(LocalDateTime cutoff);
}
