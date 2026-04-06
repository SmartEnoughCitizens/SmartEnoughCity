package com.trinity.hermes.approval.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReviewApprovalRequestDTO {
  /** "APPROVED" or "DENIED" */
  private String status;
  private String reviewNote;
}
