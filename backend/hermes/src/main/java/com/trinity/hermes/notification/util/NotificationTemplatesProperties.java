package com.trinity.hermes.notification.util;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "notification")
public class NotificationTemplatesProperties {

  private Map<String, Template> templates = new HashMap<>();

  @Data
  public static class Template {
    private Email email;
    private InApp inApp;
  }

  @Data
  public static class Email {
    private String subject;
    private String body;
  }

  @Data
  public static class InApp {
    private String subject;
    private String body;
  }
}
