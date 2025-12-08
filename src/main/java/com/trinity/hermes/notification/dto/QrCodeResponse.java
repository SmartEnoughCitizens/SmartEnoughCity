package com.trinity.hermes.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response containing generated QR code information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeResponse {

    private String qrCodeUrl; // URL to access the QR code image
    private String qrCodeBase64; // Base64 encoded QR code image
    private String shareableLink; // The link embedded in the QR code
    private Long disruptionId;
}
