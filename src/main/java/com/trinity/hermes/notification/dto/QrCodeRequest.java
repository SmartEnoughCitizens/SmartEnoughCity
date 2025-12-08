package com.trinity.hermes.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to generate a QR code for disruption information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeRequest {

    private Long disruptionId;
    private String targetUrl; // URL that QR code will point to
    private String title;
    private String description;
    private Integer sizePixels; // QR code size in pixels
}
