package com.spendsmart.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "analyticsIncomeClient", url = "${income.service.url}")
public interface IncomeClient {

    @GetMapping("/incomes/user/{userId}/total/month")
    Map<String, Object> getTotalByMonth(
            @PathVariable("userId") int userId,
            @RequestParam("month") int month,
            @RequestParam("year") int year,
            @RequestHeader("Authorization") String authorization
    );

    @GetMapping("/incomes/user/{userId}/total")
    Map<String, Object> getTotalByUser(
            @PathVariable("userId") int userId,
            @RequestHeader("Authorization") String authorization
    );
}
