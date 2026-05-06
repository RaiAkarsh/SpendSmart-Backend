package com.spendsmart.budget.controller;

import com.spendsmart.budget.dto.AmountRequest;
import com.spendsmart.budget.dto.SpentAmountRequest;
import com.spendsmart.budget.entity.Budget;
import com.spendsmart.budget.entity.BudgetProgress;
import com.spendsmart.budget.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/budgets")
@Tag(name = "Budgets", description = "Budget creation, progress, spending sync, and alerts")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @Operation(summary = "Create a new budget")
    @PostMapping
    public ResponseEntity<?> createBudget(@Valid @RequestBody Budget budget) {
        try {
            Budget saved = budgetService.createBudget(budget);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get all budgets")
    @GetMapping
    public ResponseEntity<?> getAllBudgets() {
        return ResponseEntity.ok(budgetService.getAllBudgets());
    }

    @Operation(summary = "Get budgets by user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUser(@PathVariable int userId) {
        List<Budget> budgets = budgetService.getBudgetsByUser(userId);
        return ResponseEntity.ok(budgets);
    }

    @Operation(summary = "Get active budgets by user")
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<?> getActive(@PathVariable int userId) {
        List<Budget> budgets = budgetService.getActiveBudgets(userId);
        return ResponseEntity.ok(budgets);
    }

    @Operation(summary = "Get budget by id")
    @GetMapping("/{budgetId}")
    public ResponseEntity<?> getById(@PathVariable int budgetId) {
        try {
            return ResponseEntity.ok(budgetService.getBudgetById(budgetId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get budget progress")
    @GetMapping("/{budgetId}/progress")
    public ResponseEntity<?> getProgress(@PathVariable int budgetId) {
        try {
            BudgetProgress progress = budgetService.getBudgetProgress(budgetId);
            return ResponseEntity.ok(progress);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get all active budget progress for a user")
    @GetMapping("/user/{userId}/progress")
    public ResponseEntity<?> getAllProgress(@PathVariable int userId) {
        List<BudgetProgress> progressList = budgetService.getAllBudgetProgress(userId);
        return ResponseEntity.ok(progressList);
    }

    @Operation(summary = "Get active budget by user and category")
    @GetMapping("/user/{userId}/category/{categoryId}")
    public ResponseEntity<?> getByCategory(@PathVariable int userId,
                                            @PathVariable int categoryId) {
        try {
            Budget budget = budgetService.getActiveBudgetByCategory(userId, categoryId);
            return ResponseEntity.ok(budget);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Update a budget")
    @PutMapping("/{budgetId}")
    public ResponseEntity<?> updateBudget(@PathVariable int budgetId,
                                          @Valid @RequestBody Budget updatedBudget) {
        try {
            Budget updated = budgetService.updateBudget(budgetId, updatedBudget);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Set budget spent amount")
    @PutMapping("/{budgetId}/spent")
    public ResponseEntity<?> setSpentAmount(@PathVariable int budgetId,
                                             @Valid @RequestBody SpentAmountRequest body) {
        try {
            Budget updated = budgetService.updateSpentAmount(budgetId, body.getSpentAmount());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Add amount to budget spent amount")
    @PutMapping("/{budgetId}/spent/add")
    public ResponseEntity<?> addSpent(@PathVariable int budgetId,
                                      @Valid @RequestBody AmountRequest body) {
        try {
            Budget updated = budgetService.addToSpentAmount(budgetId, body.getAmount());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Subtract amount from budget spent amount")
    @PutMapping("/{budgetId}/spent/subtract")
    public ResponseEntity<?> subtractSpent(@PathVariable int budgetId,
                                           @Valid @RequestBody AmountRequest body) {
        try {
            Budget updated = budgetService.subtractFromSpentAmount(budgetId, body.getAmount());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Deactivate a budget")
    @PutMapping("/{budgetId}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable int budgetId) {
        try {
            budgetService.deactivateBudget(budgetId);
            return ResponseEntity.ok(success("Budget deactivated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Reset budget spent amount")
    @PostMapping("/{budgetId}/reset")
    public ResponseEntity<?> reset(@PathVariable int budgetId) {
        try {
            budgetService.resetBudgetPeriod(budgetId);
            return ResponseEntity.ok(success("Budget period reset  -  spentAmount is now 0"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Delete a budget")
    @DeleteMapping("/{budgetId}")
    public ResponseEntity<?> deleteBudget(@PathVariable int budgetId) {
        try {
            budgetService.deleteBudget(budgetId);
            return ResponseEntity.ok(success("Budget deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("error", message);
        return map;
    }

    private Map<String, String> success(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        return map;
    }
}
