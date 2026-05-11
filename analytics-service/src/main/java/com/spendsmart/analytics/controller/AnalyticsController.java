package com.spendsmart.analytics.controller;

import com.spendsmart.analytics.dto.CategoryBreakdown;
import com.spendsmart.analytics.dto.FinancialHealthScore;
import com.spendsmart.analytics.dto.MonthlySummary;
import com.spendsmart.analytics.dto.SpendingTrend;
import com.spendsmart.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
@Tag(name = "Analytics", description = "Dashboard summaries, category breakdowns, trends, and health score")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @Operation(summary = "Get monthly financial summary")
    @GetMapping("/user/{userId}/summary/month")
    public ResponseEntity<?> getMonthlySummary(@Parameter(example = "1") @PathVariable int userId,
                                               @Parameter(example = "5") @RequestParam int month,
                                               @Parameter(example = "2026") @RequestParam int year) {
        try {
            MonthlySummary summary = analyticsService.getMonthlySummary(userId, month, year);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get yearly financial summary")
    @GetMapping("/user/{userId}/summary/year")
    public ResponseEntity<?> getYearlySummary(@Parameter(example = "1") @PathVariable int userId,
                                               @Parameter(example = "2026") @RequestParam int year) {
        try {
            Map<String, Object> summary = analyticsService.getYearlySummary(userId, year);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get category-wise expense breakdown")
    @GetMapping("/user/{userId}/categories")
    public ResponseEntity<?> getCategoryBreakdown(@Parameter(example = "1") @PathVariable int userId,
                                                    @Parameter(example = "5") @RequestParam int month,
                                                    @Parameter(example = "2026") @RequestParam int year) {
        try {
            List<CategoryBreakdown> breakdown =
                analyticsService.getCategoryBreakdown(userId, month, year);
            return ResponseEntity.ok(breakdown);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get spending trends")
    @GetMapping("/user/{userId}/trends")
    public ResponseEntity<?> getSpendingTrends(@Parameter(example = "1") @PathVariable int userId,
                                                @Parameter(example = "6") @RequestParam(defaultValue = "6") int months) {
        try {
            List<SpendingTrend> trends = analyticsService.getSpendingTrends(userId, months);
            return ResponseEntity.ok(trends);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get income vs expense comparison")
    @GetMapping("/user/{userId}/compare")
    public ResponseEntity<?> getIncomeVsExpense(@Parameter(example = "1") @PathVariable int userId,
                                                 @Parameter(example = "5") @RequestParam int month,
                                                 @Parameter(example = "2026") @RequestParam int year) {
        try {
            Map<String, Object> comparison =
                analyticsService.getIncomeVsExpense(userId, month, year);
            return ResponseEntity.ok(comparison);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get all-time totals")
    @GetMapping("/user/{userId}/totals")
    public ResponseEntity<?> getAllTimeTotals(@Parameter(example = "1") @PathVariable int userId) {
        try {
            Map<String, Object> totals = analyticsService.getAllTimeTotals(userId);
            return ResponseEntity.ok(totals);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get financial health score")
    @GetMapping("/user/{userId}/health")
    public ResponseEntity<?> getFinancialHealthScore(@Parameter(example = "1") @PathVariable int userId) {
        try {
            FinancialHealthScore score = analyticsService.getFinancialHealthScore(userId);
            return ResponseEntity.ok(score);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("error", message);
        return map;
    }
}
