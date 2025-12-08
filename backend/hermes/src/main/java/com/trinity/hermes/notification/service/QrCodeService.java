package com.trinity.hermes.notification.service;

import com.trinity.hermes.notification.dto.QrCodeRequest;
import com.trinity.hermes.notification.dto.QrCodeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for generating QR codes and shareable links
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeService {

    /**
     * Generate QR code for disruption information
     */
    public QrCodeResponse generateQrCode(QrCodeRequest request) {
        log.info("[QR CODE] Generating QR code for disruption: {}", request.getDisruptionId());

        // TODO: Implement actual QR code generation
        // For thin slice: return mock response
        String qrCodeUrl = String.format("https://hermes.trinity.com/qr/%d.png", request.getDisruptionId());
        String shareableLink = request.getTargetUrl();

        QrCodeResponse response = new QrCodeResponse();
        response.setQrCodeUrl(qrCodeUrl);
        response.setQrCodeBase64("base64-encoded-qr-code-placeholder");
        response.setShareableLink(shareableLink);
        response.setDisruptionId(request.getDisruptionId());

        log.info("[QR CODE] Generated: {}", qrCodeUrl);
        return response;
    }

    /**
     * Generate shareable link for disruption
     */
    public String generateShareableLink(Long disruptionId, String title) {
        String link = String.format("https://hermes.trinity.com/disruption/%d/share", disruptionId);
        log.info("[SHAREABLE LINK] Generated: {}", link);
        return link;
    }
}
