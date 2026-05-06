package com.spendsmart.budget.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "budgetExpenseClient", url = "${expense.service.url}")
public interface ExpenseClient {

    @GetMapping("/expenses/user/{userId}/total/category/{categoryId}")
    Map<String, Object> getTotalByCategory(
            @PathVariable("userId") int userId,
            @PathVariable("categoryId") int categoryId,
            @RequestHeader("Authorization") String authorization
    );
}
