package com.trinity.hermes.recommendation.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinity.hermes.notification.dto.BroadcastNotificationRequestDTO;
import com.trinity.hermes.notification.model.enums.Channel;
import com.trinity.hermes.notification.services.NotificationFacade;
import com.trinity.hermes.recommendation.entity.Recommendation;
import com.trinity.hermes.recommendation.repository.RecommendationRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainRecommendationScheduler {

  private static final String INDICATOR = "train";
  private static final String SUBJECT = "Train Frequency Change Recommendation";
  private static final String BODY =
      "The Recommendation for changing the frequency of the trains and the utilisation of different trains is attached to the email.";
  private static final String PRIORITY = "MEDIUM";

  private final RecommendationRepository recommendationRepository;
  private final NotificationFacade notificationFacade;
  private final ObjectMapper objectMapper;

  @Scheduled(cron = "${recommendation.train.cron:0 0 8 * * *}")
  public void broadcastTrainRecommendations() {
    log.info("Running scheduled Train recommendation broadcast");

    List<Recommendation> recommendations =
        recommendationRepository.findActiveByIndicator(INDICATOR);

    if (recommendations.isEmpty()) {
      log.info("No active Train recommendations found, skipping broadcast");
      return;
    }

    log.info("Found {} active Train recommendation(s), broadcasting...", recommendations.size());

    for (Recommendation rec : recommendations) {
      try {
        BroadcastNotificationRequestDTO request = new BroadcastNotificationRequestDTO();
        request.setQrid(UUID.randomUUID().toString());
        request.setDataIndicator(INDICATOR);
        request.setSubject(SUBJECT);
        request.setBody(BODY);
        request.setPriority(PRIORITY);
        request.setChannel(Channel.EMAIL_AND_NOTIFICATION);
        request.setRecommendation(parseRecommendation(rec.getRecommendation()));
        request.setSimulation(parseRecommendation(rec.getSimulation()));

        notificationFacade.broadcastByIndicator(request);
        log.info("Broadcast sent for recommendation id={}", rec.getId());
      } catch (Exception e) {
        log.error("Failed to broadcast recommendation id={}: {}", rec.getId(), e.getMessage(), e);
      }
    }
  }

  private Map<String, Object> parseRecommendation(String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      log.warn("Could not parse recommendation JSON, wrapping as raw string: {}", e.getMessage());
      return Map.of("raw", json);
    }
  }
}
