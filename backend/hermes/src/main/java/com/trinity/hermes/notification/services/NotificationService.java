package com.trinity.hermes.notification.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinity.hermes.notification.model.Notification;
import com.trinity.hermes.notification.model.User;
import com.trinity.hermes.notification.model.enums.Channel;
import com.trinity.hermes.notification.model.enums.ServiceType;
import com.trinity.hermes.notification.util.NotificationTemplatesProperties;
import com.trinity.hermes.notification.util.QrCodeUtil;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

import static com.trinity.hermes.notification.model.enums.Channel.*;
import static com.trinity.hermes.notification.util.Constants.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationTemplatesProperties notificationTemplatesProperties;
    private final ObjectMapper objectMapper;
    private final QrCodeUtil qrCodeUtil;

    /**
     * Create a notification for a user
     * @param user - helps define the recipient mail.
     * @param payloads - dynamic data to be included in the notification.
     * @return notification object
     */
    public Set<Notification> createNotification(
            User user,
            Object... payloads
    ) {
        try  {
            Map<String, Object> data = buildPayloadMap(payloads);
            byte[] qrCode = null;
            if (data.containsKey(QR_ID) && StringUtils.isNotBlank(Objects.toString(data.get(QR_ID), EMPTY_STRING))) {

                log.info("Generating QR Code for ID: {}", data.get(QR_ID));
                qrCode = qrCodeUtil.generateQrCode(UriComponentsBuilder
                        .fromUriString(DUMMY_ENDPOINT)
                        .pathSegment(Objects.toString(data.get(QR_ID)))
                        .build()
                        .toUriString(), BASE_WIDTH, BASE_HEIGHT);

            }
            // TODO: Add logic to retrieve recepient email Id from the user for now use hardcoded recepient email id, for now use hardcoded user.
            if (data.containsKey("subject") && data.containsKey("body") &&
                    StringUtils.isNotBlank(Objects.toString(data.get(SUBJECT), EMPTY_STRING)) &&
                    StringUtils.isNotBlank(Objects.toString(data.get(BODY), EMPTY_STRING))) {
                return createCustomNotification(DUMMY_EMAIL, data, qrCode);
            }
            return createFromTemplate(DUMMY_EMAIL, data, qrCode);
        } catch (Exception e) {
            log.error("Error while creating notification -", e);
            return null;
        }
    }

    private Set<Notification> createFromTemplate(
            String recipient,
            Map<String, Object> data,
            byte[] qrCode
    ) {
        try {
            String templateKey = Objects.toString(data.getOrDefault(TEMPLATE_KEY, EMPTY_STRING), EMPTY_STRING);
            if(StringUtils.isEmpty(templateKey)) {
                log.warn("Template key is empty, using default template");
                templateKey = DEFAULT_TEMPLATE;
            }
            log.info("Creating notification for template: {}", templateKey);
            NotificationTemplatesProperties.Template template =
                    notificationTemplatesProperties
                            .getTemplates()
                            .getOrDefault(
                                    templateKey,
                                    notificationTemplatesProperties.getTemplates().get(DEFAULT_TEMPLATE)
                            );
            if (Objects.isNull(template)) {
                log.error("No template '{}' and no default template available. Returning empty set.", templateKey);
                return Set.of();
            }
            Set<Notification> notifications = new HashSet<>();
            if (Objects.nonNull(template.getEmail())) {
                String subject = render(template.getEmail().getSubject(), data);
                String body    = render(template.getEmail().getBody(), data);
                //TODO abstract in a builder outside this function to avoid repetition of code
                notifications.add(
                        Notification.builder()
                                .recipient(recipient)
                                .subject(subject)
                                .body(body)
                                .channel(EMAIL)
                                .qrCode(qrCode)
                                .build()
                );
            }
            if (Objects.nonNull(template.getInApp())) {
                String subject = render(template.getInApp().getSubject(), data);
                String body    = render(template.getInApp().getBody(), data);
                notifications.add(
                        Notification.builder()
                                .recipient(recipient)
                                .subject(subject)
                                .body(body)
                                .channel(NOTIFICATION)
                                .qrCode(qrCode)
                                .build()
                );
            }
            return notifications;


        } catch (Exception e) {
            log.error("Error while creating notification from template -", e);
            return null;
        }
    }

    /**
     * Create a custom notification when subject and body are provided in the data map
     * @param recepient - email id of the recipient
     * @param data - dynamic data map
     * @return notification object
     */
    private Set<Notification> createCustomNotification(
            String recepient,
            Map<String, Object> data,
            byte[] qrCode
    ) {
        log.info("Creating notification for recepient: {}", recepient);
        Notification notification = Notification.builder()
                .recipient(recepient)
                .subject(data.getOrDefault(SUBJECT, EMPTY_STRING).toString())
                .body(data.getOrDefault(BODY, EMPTY_STRING).toString())
                .channel(EMAIL_AND_NOTIFICATION)
                .qrCode(qrCode)
                .build();
        return Set.of(notification);
    }

    /**
     * Build a payload map from various payload objects
     * @param payloads - dynamic data objects
     * @return map of payload data
     */
    private Map<String, Object> buildPayloadMap(Object... payloads) {
        Map<String, Object> result = new HashMap<>();
        log.info("Building payload map from {} payloads", payloads.length);
        // Always expose some basic user fields
        for (Object p : payloads) {
            if (Objects.isNull(p)) {
                continue;
            }
            if (p instanceof Map<?, ?> m) {
                m.forEach((k, v) -> result.put(k.toString(), v));
            } else {
                Map<String, Object> dtoMap =
                        objectMapper.convertValue(p, new TypeReference<Map<String, Object>>() {});
                result.putAll(dtoMap);
            }
        }

        return result;
    }


    /**
     *
     * @param template -  key for the template to be used
     * @param datapoints - all the datapoints that need to be populated in the template
     * @return
     */
    private String render(String template, Map<String, Object> datapoints) {
        if (template == null) {
            return null;
        }
        String result = template;
        for (var entry : datapoints.entrySet()) {
            String key = "{" + entry.getKey() + "}";
            result = result.replace(key, Objects.toString(entry.getValue(), ""));
        }
        return result;
    }

}
