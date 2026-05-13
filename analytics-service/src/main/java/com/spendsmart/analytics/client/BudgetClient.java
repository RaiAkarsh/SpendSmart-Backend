package com.spendsmart.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "analyticsBudgetClient", url = "${budget.service.url}")
public interface BudgetClient {

    @GetMapping("/budgets/user/{userId}/active")
    List<Map<String, Object>> getActiveBudgets(
            @PathVariable("userId") int userId,
            @RequestHeader("Authorization") String authorization
    );
}
