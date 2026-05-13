package com.spendsmart.recurring.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "recurringExpenseClient", url = "${expense.service.url}")
public interface ExpenseClient {

    @PostMapping("/expenses")
    Map<String, Object> createExpense(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authorization
    );

    @PutMapping("/expenses/{expenseId}")
    Map<String, Object> updateExpense(
            @PathVariable("expenseId") int expenseId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authorization
    );

    @DeleteMapping("/expenses/{expenseId}")
    Map<String, String> deleteExpense(
            @PathVariable("expenseId") int expenseId,
            @RequestHeader("Authorization") String authorization
    );
}
