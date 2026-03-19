package com.trinity.hermes.notification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.trinity.hermes.notification.dto.BackendNotificationRequestDTO;
import com.trinity.hermes.notification.dto.NotificationItemDTO;
import com.trinity.hermes.notification.dto.NotificationResponseDTO;
import com.trinity.hermes.notification.entity.NotificationEntity;
import com.trinity.hermes.notification.model.Notification;
import com.trinity.hermes.notification.model.User;
import com.trinity.hermes.notification.model.enums.Channel;
import com.trinity.hermes.notification.repository.NotificationRepository;
import com.trinity.hermes.notification.services.NotificationDispatcher;
import com.trinity.hermes.notification.services.NotificationFacade;
import com.trinity.hermes.notification.services.NotificationService;
import com.trinity.hermes.usermanagement.service.UserManagementService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationFacadeTest {

  @Mock NotificationService notificationService;
  @Mock NotificationDispatcher notificationDispatcher;
  @Mock UserManagementService userManagementService;
  @Mock NotificationRepository notificationRepository;

  NotificationFacade facade;

  @BeforeEach
  void setUp() {
    facade =
        new NotificationFacade(
            notificationService,
            notificationDispatcher,
            userManagementService,
            notificationRepository);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // handleBackendNotification
  // ─────────────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleBackendNotification")
  class HandleBackendNotification {

    @Test
    @DisplayName("NOTIFICATION channel: dispatches SSE and persists to DB")
    void notificationChannel_dispatchesSseAndSaves() {
      BackendNotificationRequestDTO dto = new BackendNotificationRequestDTO();
      dto.setUserId("user-1");
      dto.setSubject("Alert");
      dto.setBody("Something happened");
      dto.setQrid("qr-001");

      Notification notification =
          Notification.builder()
              .subject("Alert")
              .body("Something happened")
              .recipient("user@example.com")
              .channel(Channel.NOTIFICATION)
              .build();

      when(notificationService.createNotification(any(User.class), eq(dto)))
          .thenReturn(Set.of(notification));

      facade.handleBackendNotification(dto);

      verify(notificationDispatcher).dispatchSse(eq("user-1"), eq(notification));
      verify(notificationDispatcher, never()).dispatchMail(any());

      ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
      verify(notificationRepository).save(captor.capture());

      NotificationEntity saved = captor.getValue();
      assertEquals("user-1", saved.getUserId());
      assertEquals("Alert", saved.getSubject());
      assertEquals("Something happened", saved.getBody());
      assertEquals("user@example.com", saved.getRecipient());
      assertEquals(Channel.NOTIFICATION, saved.getChannel());
      assertFalse(saved.isRead());
      assertEquals("qr-001", saved.getQrCodeId());
    }

    @Test
    @DisplayName("EMAIL_AND_NOTIFICATION channel: dispatches both email and SSE, and persists")
    void emailAndNotificationChannel_dispatchesBothAndSaves() {
      BackendNotificationRequestDTO dto = new BackendNotificationRequestDTO();
      dto.setUserId("user-2");
      dto.setSubject("Subject");
      dto.setBody("Body");

      Notification notification =
          Notification.builder()
              .subject("Subject")
              .body("Body")
              .recipient("r@example.com")
              .channel(Channel.EMAIL_AND_NOTIFICATION)
              .build();

      when(notificationService.createNotification(any(User.class), eq(dto)))
          .thenReturn(Set.of(notification));

      facade.handleBackendNotification(dto);

      verify(notificationDispatcher).dispatchMail(eq(notification));
      verify(notificationDispatcher).dispatchSse(eq("user-2"), eq(notification));
      verify(notificationRepository).save(any(NotificationEntity.class));
    }

    @Test
    @DisplayName("EMAIL channel: dispatches email only, does NOT persist to DB")
    void emailChannel_dispatchesEmailOnly_doesNotSave() {
      BackendNotificationRequestDTO dto = new BackendNotificationRequestDTO();
      dto.setUserId("user-3");
      dto.setSubject("Email only");
      dto.setBody("Body");

      Notification notification =
          Notification.builder()
              .subject("Email only")
              .body("Body")
              .recipient("r@example.com")
              .channel(Channel.EMAIL)
              .build();

      when(notificationService.createNotification(any(User.class), eq(dto)))
          .thenReturn(Set.of(notification));

      facade.handleBackendNotification(dto);

      verify(notificationDispatcher).dispatchMail(eq(notification));
      verify(notificationDispatcher, never()).dispatchSse(any(), any());
      verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("null notification set from service: does nothing")
    void nullNotificationSet_doesNothing() {
      BackendNotificationRequestDTO dto = new BackendNotificationRequestDTO();
      dto.setUserId("user-4");

      when(notificationService.createNotification(any(User.class), eq(dto))).thenReturn(null);

      facade.handleBackendNotification(dto);

      verifyNoInteractions(notificationDispatcher);
      verifyNoInteractions(notificationRepository);
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // getAll
  // ─────────────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("getAll")
  class GetAll {

    @Test
    @DisplayName("returns NotificationResponseDTO with correctly mapped items and unread count")
    void returnsResponseWithMappedItemsAndUnreadCount() {
      LocalDateTime now = LocalDateTime.of(2025, 8, 26, 10, 0);

      NotificationEntity entity =
          NotificationEntity.builder()
              .id(42L)
              .userId("user-1")
              .subject("Hello")
              .body("World")
              .recipient("r@example.com")
              .channel(Channel.NOTIFICATION)
              .isRead(false)
              .qrCodeId("qr-999")
              .createdAt(now)
              .build();

      when(notificationRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
          .thenReturn(List.of(entity));
      when(notificationRepository.countByUserIdAndIsReadFalse("user-1")).thenReturn(1L);

      NotificationResponseDTO response = facade.getAll("user-1");

      assertEquals("user-1", response.getUserId());
      assertEquals(1L, response.getTotalCount());
      assertEquals(1, response.getNotifications().size());

      NotificationItemDTO item = response.getNotifications().get(0);
      assertEquals("42", item.getId());
      assertEquals("Hello", item.getSubject());
      assertEquals("World", item.getBody());
      assertEquals("r@example.com", item.getRecipient());
      assertEquals("NOTIFICATION", item.getChannel());
      assertFalse(item.isRead());
      assertEquals("qr-999", item.getQrCodeId());
      assertEquals(now.toString(), item.getTimestamp());
    }

    @Test
    @DisplayName(
        "returns empty notifications list and zero unread count when user has no notifications")
    void noNotifications_returnsEmptyListAndZeroCount() {
      when(notificationRepository.findByUserIdOrderByCreatedAtDesc("user-99"))
          .thenReturn(List.of());
      when(notificationRepository.countByUserIdAndIsReadFalse("user-99")).thenReturn(0L);

      NotificationResponseDTO response = facade.getAll("user-99");

      assertEquals("user-99", response.getUserId());
      assertTrue(response.getNotifications().isEmpty());
      assertEquals(0L, response.getTotalCount());
    }

    @Test
    @DisplayName("null channel on entity maps to null channel string in DTO")
    void nullChannel_mapsToNullChannelString() {
      NotificationEntity entity =
          NotificationEntity.builder()
              .id(1L)
              .userId("user-1")
              .subject("Test")
              .channel(null)
              .createdAt(LocalDateTime.now(ZoneId.of("Europe/Dublin")))
              .build();

      when(notificationRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
          .thenReturn(List.of(entity));
      when(notificationRepository.countByUserIdAndIsReadFalse("user-1")).thenReturn(0L);

      NotificationItemDTO item = facade.getAll("user-1").getNotifications().get(0);

      assertNull(item.getChannel());
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // markAsRead
  // ─────────────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("markAsRead")
  class MarkAsRead {

    @Test
    @DisplayName("returns true and saves entity when notification belongs to user")
    void found_marksReadAndReturnsTrue() {
      NotificationEntity entity =
          NotificationEntity.builder()
              .id(10L)
              .userId("user-1")
              .subject("Test")
              .isRead(false)
              .channel(Channel.NOTIFICATION)
              .createdAt(LocalDateTime.now(ZoneId.of("Europe/Dublin")))
              .build();

      when(notificationRepository.findByIdAndUserId(10L, "user-1")).thenReturn(Optional.of(entity));

      boolean result = facade.markAsRead("user-1", 10L);

      assertTrue(result);
      assertTrue(entity.isRead());
      verify(notificationRepository).save(entity);
    }

    @Test
    @DisplayName("returns false when notification not found for user")
    void notFound_returnsFalse() {
      when(notificationRepository.findByIdAndUserId(99L, "user-1")).thenReturn(Optional.empty());

      boolean result = facade.markAsRead("user-1", 99L);

      assertFalse(result);
      verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("returns false when notification belongs to a different user")
    void differentUser_returnsFalse() {
      // findByIdAndUserId already scopes by userId — returns empty for wrong user
      when(notificationRepository.findByIdAndUserId(10L, "other-user"))
          .thenReturn(Optional.empty());

      boolean result = facade.markAsRead("other-user", 10L);

      assertFalse(result);
      verify(notificationRepository, never()).save(any());
    }
  }
}
