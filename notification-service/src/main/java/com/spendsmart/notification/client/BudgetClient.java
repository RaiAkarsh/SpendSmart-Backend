package com.spendsmart.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "notificationBudgetClient", url = "${budget.service.url}")
public interface BudgetClient {

    @GetMapping("/budgets")
    List<Map<String, Object>> getAllBudgets(@RequestHeader("Authorization") String authorization);
}
