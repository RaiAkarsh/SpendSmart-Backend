package com.spendsmart.recurring.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "recurringIncomeClient", url = "${income.service.url}")
public interface IncomeClient {

    @PostMapping("/incomes")
    Map<String, Object> createIncome(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authorization
    );

    @PutMapping("/incomes/{incomeId}")
    Map<String, Object> updateIncome(
            @PathVariable("incomeId") int incomeId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authorization
    );

    @DeleteMapping("/incomes/{incomeId}")
    Map<String, String> deleteIncome(
            @PathVariable("incomeId") int incomeId,
            @RequestHeader("Authorization") String authorization
    );
}
