package com.trinity.hermes.notification.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {

  private String userId;
  private List<NotificationItemDTO> notifications;
  private long totalCount; // unread count — drives the badge counter on the frontend
  private long totalItems; // total non-deleted notifications (for pagination)
  private int page; // current 0-based page
  private int pageSize; // items per page
}
