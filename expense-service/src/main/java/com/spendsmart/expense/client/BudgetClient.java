package com.spendsmart.expense.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "expenseBudgetClient", url = "${budget.service.url}")
public interface BudgetClient {

    @GetMapping("/budgets/user/{userId}/category/{categoryId}")
    Map<String, Object> getActiveBudgetByCategory(
            @PathVariable("userId") int userId,
            @PathVariable("categoryId") int categoryId,
            @RequestHeader("Authorization") String authorization
    );

    @PutMapping("/budgets/{budgetId}/spent")
    Map<String, Object> updateSpentAmount(
            @PathVariable("budgetId") int budgetId,
            @RequestBody Map<String, Double> body,
            @RequestHeader("Authorization") String authorization
    );
}
