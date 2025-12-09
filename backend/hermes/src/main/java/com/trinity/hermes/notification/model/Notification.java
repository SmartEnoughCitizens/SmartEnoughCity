package com.trinity.hermes.notification.model;

import com.trinity.hermes.notification.model.enums.Channel;
import com.trinity.hermes.notification.model.enums.NotificationStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder(toBuilder = true)
public class Notification {
    private String recipient;
    private String subject;
    private String body;
    private Channel channel;
    private byte[] qrCode;
//    private String templateKey;
//    private Map<String, Object> data; // original data if you want
}
