package com.trinity.hermes.notification.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinity.hermes.notification.dto.BackendNotificationRequestDTO;
import com.trinity.hermes.notification.dto.NotificationItemDTO;
import com.trinity.hermes.notification.dto.NotificationResponseDTO;
import com.trinity.hermes.notification.services.NotificationFacade;
import com.trinity.hermes.notification.util.SseManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@Import(NotificationControllerTest.TestSecurityConfig.class)
public class NotificationControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean NotificationFacade notificationFacade;
  @MockitoBean SseManager sseManager;

  @TestConfiguration
  static class TestSecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      return http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
      return token -> {
        throw new JwtException("Not used in tests");
      };
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // POST /api/notification/v1
  // ─────────────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("POST /api/notification/v1")
  class PostNotification {

    @Test
    @DisplayName("200 with status=accepted and delegates to facade")
    void receiveNotification_callsFacadeAndReturnsAccepted() throws Exception {
      BackendNotificationRequestDTO dto = new BackendNotificationRequestDTO();
      dto.setUserId("user-1");
      dto.setSubject("Test");
      dto.setBody("Hello");

      doNothing().when(notificationFacade).handleBackendNotification(any());

      mockMvc
          .perform(
              post("/api/notification/v1")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(dto)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("accepted"));

      verify(notificationFacade)
          .handleBackendNotification(any(BackendNotificationRequestDTO.class));
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // GET /api/notification/v1/{userId}
  // ─────────────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("GET /api/notification/v1/{userId}")
  class GetUserNotifications {

    @Test
    @DisplayName("200 with NotificationResponseDTO from facade")
    void getUserNotifications_returnsResponseFromFacade() throws Exception {
      NotificationItemDTO item =
          NotificationItemDTO.builder()
              .id("42")
              .subject("Hello")
              .body("World")
              .channel("NOTIFICATION")
              .read(false)
              .qrCodeId("qr-001")
              .build();

      NotificationResponseDTO response =
          NotificationResponseDTO.builder()
              .userId("user-1")
              .notifications(List.of(item))
              .totalCount(1L)
              .build();

      when(notificationFacade.getAll("user-1")).thenReturn(response);

      mockMvc
          .perform(get("/api/notification/v1/user-1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.userId").value("user-1"))
          .andExpect(jsonPath("$.totalCount").value(1))
          .andExpect(jsonPath("$.notifications[0].id").value("42"))
          .andExpect(jsonPath("$.notifications[0].subject").value("Hello"))
          .andExpect(jsonPath("$.notifications[0].channel").value("NOTIFICATION"))
          .andExpect(jsonPath("$.notifications[0].read").value(false))
          .andExpect(jsonPath("$.notifications[0].qrCodeId").value("qr-001"));
    }

    @Test
    @DisplayName("200 with empty list when user has no notifications")
    void getUserNotifications_emptyList() throws Exception {
      NotificationResponseDTO response =
          NotificationResponseDTO.builder()
              .userId("user-99")
              .notifications(List.of())
              .totalCount(0L)
              .build();

      when(notificationFacade.getAll("user-99")).thenReturn(response);

      mockMvc
          .perform(get("/api/notification/v1/user-99"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.notifications").isEmpty())
          .andExpect(jsonPath("$.totalCount").value(0));
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // PATCH /api/notification/v1/{userId}/{notificationId}/read
  // ─────────────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("PATCH /api/notification/v1/{userId}/{notificationId}/read")
  class MarkAsRead {

    @Test
    @DisplayName("200 with status=updated when notification belongs to user")
    void markAsRead_found_returns200() throws Exception {
      when(notificationFacade.markAsRead("user-1", 42L)).thenReturn(true);

      mockMvc
          .perform(patch("/api/notification/v1/user-1/42/read"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("updated"));
    }

    @Test
    @DisplayName("404 when notification not found or belongs to a different user")
    void markAsRead_notFound_returns404() throws Exception {
      when(notificationFacade.markAsRead("user-1", 99L)).thenReturn(false);

      mockMvc
          .perform(patch("/api/notification/v1/user-1/99/read"))
          .andExpect(status().isNotFound());
    }
  }
}
