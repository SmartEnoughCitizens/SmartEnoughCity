package com.trinity.hermes.email.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response after email sending attempt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {

    private Boolean success;
    private String messageId;
    private String status; // QUEUED, SENT, FAILED
    private String errorMessage;
    private LocalDateTime sentAt;
    private Integer recipientCount;
}
