package com.spendsmart.budget.serviceimpl;

import com.spendsmart.budget.config.RabbitMqConfig;
import com.spendsmart.budget.entity.Budget;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BudgetAlertPublisher {

    private final RabbitTemplate rabbitTemplate;

    public BudgetAlertPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishIfNeeded(Budget budget) {
        if (!budget.isActive() || budget.getLimitAmount() <= 0) {
            return;
        }

        double percentageUsed = (budget.getSpentAmount() / budget.getLimitAmount()) * 100;
        String alertType = resolveAlertType(budget, percentageUsed);
        if (alertType == null) {
            return;
        }

        Map<String, Object> event = Map.of(
                "userId", budget.getUserId(),
                "budgetId", budget.getBudgetId(),
                "budgetName", budget.getName(),
                "percentageUsed", percentageUsed,
                "alertType", alertType
        );

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.NOTIFICATION_EXCHANGE,
                    RabbitMqConfig.BUDGET_ALERT_ROUTING_KEY,
                    event
            );
        } catch (Exception e) {
            System.err.println("Budget alert publish skipped for budgetId="
                    + budget.getBudgetId() + ": " + e.getMessage());
        }
    }

    private String resolveAlertType(Budget budget, double percentageUsed) {
        if (budget.getSpentAmount() > budget.getLimitAmount()) {
            return "BUDGET_EXCEEDED";
        }
        if (percentageUsed >= budget.getAlertThreshold()) {
            return "BUDGET_WARNING";
        }
        return null;
    }
}
