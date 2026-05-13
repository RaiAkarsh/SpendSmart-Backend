package com.spendsmart.notification.listener;

import com.spendsmart.notification.config.RabbitMqConfig;
import com.spendsmart.notification.repository.NotificationRepository;
import com.spendsmart.notification.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BudgetAlertListener {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    public BudgetAlertListener(NotificationService notificationService,
                               NotificationRepository notificationRepository) {
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
    }

    @RabbitListener(queues = RabbitMqConfig.BUDGET_ALERT_QUEUE)
    public void handleBudgetAlert(Map<String, Object> event) {
        if (event == null || event.get("alertType") == null) {
            return;
        }

        int userId = ((Number) event.get("userId")).intValue();
        int budgetId = ((Number) event.get("budgetId")).intValue();
        String budgetName = (String) event.get("budgetName");
        double percentageUsed = ((Number) event.get("percentageUsed")).doubleValue();
        String alertType = (String) event.get("alertType");

        boolean exists = notificationRepository.existsActiveNotification(
                userId,
                budgetId,
                "BUDGET",
                alertType
        );
        if (exists) {
            return;
        }

        if ("BUDGET_EXCEEDED".equals(alertType)) {
            notificationService.createBudgetExceeded(
                    userId,
                    budgetId,
                    budgetName,
                    percentageUsed
            );
            return;
        }

        if ("BUDGET_WARNING".equals(alertType)) {
            notificationService.createBudgetWarning(
                    userId,
                    budgetId,
                    budgetName,
                    percentageUsed
            );
        }
    }
}
