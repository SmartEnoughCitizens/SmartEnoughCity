package com.trinity.hermes.notification.repository;

import com.trinity.hermes.notification.entity.NotificationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

  /** Fetch all notifications for a user, newest first. */
  List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(String userId);

  /** Count unread notifications for a user (drives badge counter). */
  long countByUserIdAndIsReadFalse(String userId);

  /**
   * Find a notification by ID and userId together — ensures a user can only mark their own
   * notifications as read.
   */
  Optional<NotificationEntity> findByIdAndUserId(Long id, String userId);
}
