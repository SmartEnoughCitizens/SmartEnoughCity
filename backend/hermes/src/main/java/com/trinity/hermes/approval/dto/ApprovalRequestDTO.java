package com.trinity.hermes.approval.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApprovalRequestDTO {
  private Long id;
  private String indicator;
  private String requestedBy;
  private String status;
  private String payloadJson;
  private String summary;
  private String reviewedBy;
  private String reviewNote;
  private String actionUrl;
  private String createdAt;
  private String updatedAt;
}
