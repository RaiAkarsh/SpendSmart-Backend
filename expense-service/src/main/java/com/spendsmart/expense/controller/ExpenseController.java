package com.spendsmart.expense.controller;

import com.spendsmart.expense.entity.Expense;
import com.spendsmart.expense.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expenses")
@Tag(name = "Expenses", description = "Expense CRUD, filters, totals, and budget sync")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Operation(summary = "Add a new expense")
    @PostMapping
    public ResponseEntity<?> addExpense(@Valid @RequestBody Expense expense) {
        try {
            Expense saved = expenseService.addExpense(expense);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get expenses by user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUser(@PathVariable int userId) {
        List<Expense> expenses = expenseService.getExpensesByUser(userId);
        return ResponseEntity.ok(expenses);
    }

    @Operation(summary = "Get expense by id")
    @GetMapping("/{expenseId}")
    public ResponseEntity<?> getById(@PathVariable int expenseId) {
        try {
            return ResponseEntity.ok(expenseService.getExpenseById(expenseId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get expenses by category")
    @GetMapping("/user/{userId}/category/{categoryId}")
    public ResponseEntity<?> getByCategory(@PathVariable int userId,
                                            @PathVariable int categoryId) {
        List<Expense> expenses = expenseService.getExpensesByCategory(userId, categoryId);
        return ResponseEntity.ok(expenses);
    }

    @Operation(summary = "Get expenses by month")
    @GetMapping("/user/{userId}/month")
    public ResponseEntity<?> getByMonth(@PathVariable int userId,
                                         @RequestParam int month,
                                         @RequestParam int year) {
        List<Expense> expenses = expenseService.getExpensesByMonth(userId, month, year);
        return ResponseEntity.ok(expenses);
    }

    @Operation(summary = "Get expenses by date range")
    @GetMapping("/user/{userId}/range")
    public ResponseEntity<?> getByDateRange(@PathVariable int userId,
                                             @RequestParam String start,
                                             @RequestParam String end) {
        try {
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);
            List<Expense> expenses = expenseService.getExpensesByDateRange(userId, startDate, endDate);
            return ResponseEntity.ok(expenses);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get expenses by payment method")
    @GetMapping("/user/{userId}/payment/{method}")
    public ResponseEntity<?> getByPaymentMethod(@PathVariable int userId,
                                                 @PathVariable String method) {
        List<Expense> expenses = expenseService.getExpensesByPaymentMethod(userId, method);
        return ResponseEntity.ok(expenses);
    }

    @Operation(summary = "Search expenses")
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<?> search(@PathVariable int userId,
                                    @RequestParam String keyword) {
        List<Expense> expenses = expenseService.searchExpenses(userId, keyword);
        return ResponseEntity.ok(expenses);
    }

    @Operation(summary = "Get total expenses by user")
    @GetMapping("/user/{userId}/total")
    public ResponseEntity<?> getTotalByUser(@PathVariable int userId) {
        double total = expenseService.getTotalByUser(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "totalExpenses", total));
    }

    @Operation(summary = "Get total expenses by month")
    @GetMapping("/user/{userId}/total/month")
    public ResponseEntity<?> getTotalByMonth(@PathVariable int userId,
                                              @RequestParam int month,
                                              @RequestParam int year) {
        double total = expenseService.getTotalByMonth(userId, month, year);
        return ResponseEntity.ok(Map.of("userId", userId, "month", month,
                                        "year", year, "totalExpenses", total));
    }

    @Operation(summary = "Get total expenses by category")
    @GetMapping("/user/{userId}/total/category/{categoryId}")
    public ResponseEntity<?> getTotalByCategory(@PathVariable int userId,
                                                 @PathVariable int categoryId) {
        double total = expenseService.getTotalByCategory(userId, categoryId);
        return ResponseEntity.ok(Map.of("userId", userId, "categoryId", categoryId,
                                        "totalExpenses", total));
    }

    @Operation(summary = "Update an expense")
    @PutMapping("/{expenseId}")
    public ResponseEntity<?> updateExpense(@PathVariable int expenseId,
                                           @Valid @RequestBody Expense updatedExpense) {
        try {
            Expense updated = expenseService.updateExpense(expenseId, updatedExpense);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Delete an expense")
    @DeleteMapping("/{expenseId}")
    public ResponseEntity<?> deleteExpense(@PathVariable int expenseId) {
        try {
            expenseService.deleteExpense(expenseId);
            return ResponseEntity.ok(success("Expense deleted successfully"));
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
