package com.trinity.hermes.notification.services;

import com.trinity.hermes.notification.dto.BackendNotificationRequestDTO;
import com.trinity.hermes.notification.model.Notification;
import com.trinity.hermes.notification.model.User;
import com.trinity.hermes.notification.model.enums.Channel;
import com.trinity.hermes.recommendation.dto.CreateRecommendationRequest;
import com.trinity.hermes.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificationService notificationService;
    private final RecommendationService recommendationService;
    private final NotificationDispatcher notificationDispatcher;

    public void handleBackendNotification(BackendNotificationRequestDTO backendNotificationRequestDTO) {
        //TODO:  Add code for schema validations that need to be performed via networkNT
        //TODO: Add code for user retreival
        //TODO:  fix code to have facade work better
        User user = User.builder().build();
        Set<Notification> notificationSet = notificationService.createNotification(user, backendNotificationRequestDTO);
        for (Notification notification : notificationSet) {
            if (Objects.nonNull(notification) && notification.getChannel() == Channel.EMAIL || notification.getChannel() == Channel.EMAIL_AND_NOTIFICATION) {
                notificationDispatcher.dispatchMail(notification);
            }
            if (Objects.nonNull(notification) && notification.getChannel() == Channel.NOTIFICATION || notification.getChannel() == Channel.EMAIL_AND_NOTIFICATION) {
                notificationDispatcher.dispatchSse(notification);
            }

            recommendationService.createRecommendation(new CreateRecommendationRequest());




        }


//        Notification notification = notificationService.createNotification(
//                user.getId(),
//                serviceType,
//                title,
//                req.getMessage(),
//                priority
//        );





    }


}
