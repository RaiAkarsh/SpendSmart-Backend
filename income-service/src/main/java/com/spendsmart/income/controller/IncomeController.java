package com.spendsmart.income.controller;

import com.spendsmart.income.entity.Income;
import com.spendsmart.income.service.IncomeService;
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
@RequestMapping("/incomes")
@Tag(name = "Incomes", description = "Income CRUD, filters, recurring income, and totals")
public class IncomeController {

    @Autowired
    private IncomeService incomeService;

    @Operation(summary = "Add a new income entry")
    @PostMapping
    public ResponseEntity<?> addIncome(@Valid @RequestBody Income income) {
        try {
            Income saved = incomeService.addIncome(income);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get incomes by user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUser(@PathVariable int userId) {
        List<Income> incomes = incomeService.getIncomesByUser(userId);
        return ResponseEntity.ok(incomes);
    }

    @Operation(summary = "Get income by id")
    @GetMapping("/{incomeId}")
    public ResponseEntity<?> getById(@PathVariable int incomeId) {
        try {
            return ResponseEntity.ok(incomeService.getIncomeById(incomeId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get incomes by source")
    @GetMapping("/user/{userId}/source/{source}")
    public ResponseEntity<?> getBySource(@PathVariable int userId,
                                          @PathVariable String source) {
        List<Income> incomes = incomeService.getIncomesBySource(userId, source);
        return ResponseEntity.ok(incomes);
    }

    @Operation(summary = "Get incomes by category")
    @GetMapping("/user/{userId}/category/{categoryId}")
    public ResponseEntity<?> getByCategory(@PathVariable int userId,
                                            @PathVariable int categoryId) {
        List<Income> incomes = incomeService.getIncomesByCategory(userId, categoryId);
        return ResponseEntity.ok(incomes);
    }

    @Operation(summary = "Get incomes by month")
    @GetMapping("/user/{userId}/month")
    public ResponseEntity<?> getByMonth(@PathVariable int userId,
                                         @RequestParam int month,
                                         @RequestParam int year) {
        List<Income> incomes = incomeService.getIncomesByMonth(userId, month, year);
        return ResponseEntity.ok(incomes);
    }

    @Operation(summary = "Get incomes by date range")
    @GetMapping("/user/{userId}/range")
    public ResponseEntity<?> getByDateRange(@PathVariable int userId,
                                             @RequestParam String start,
                                             @RequestParam String end) {
        try {
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);
            List<Income> incomes = incomeService.getIncomesByDateRange(userId, startDate, endDate);
            return ResponseEntity.ok(incomes);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Search incomes")
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<?> search(@PathVariable int userId,
                                    @RequestParam String keyword) {
        List<Income> incomes = incomeService.searchIncomes(userId, keyword);
        return ResponseEntity.ok(incomes);
    }

    @Operation(summary = "Get recurring incomes")
    @GetMapping("/user/{userId}/recurring")
    public ResponseEntity<?> getRecurring(@PathVariable int userId) {
        List<Income> incomes = incomeService.getRecurringIncomes(userId);
        return ResponseEntity.ok(incomes);
    }

    @Operation(summary = "Get total income by user")
    @GetMapping("/user/{userId}/total")
    public ResponseEntity<?> getTotalByUser(@PathVariable int userId) {
        double total = incomeService.getTotalByUser(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "totalIncome", total));
    }

    @Operation(summary = "Get total income by month")
    @GetMapping("/user/{userId}/total/month")
    public ResponseEntity<?> getTotalByMonth(@PathVariable int userId,
                                              @RequestParam int month,
                                              @RequestParam int year) {
        double total = incomeService.getTotalByMonth(userId, month, year);
        return ResponseEntity.ok(Map.of("userId", userId, "month", month,
                                        "year", year, "totalIncome", total));
    }

    @Operation(summary = "Get total income by source")
    @GetMapping("/user/{userId}/total/source/{source}")
    public ResponseEntity<?> getTotalBySource(@PathVariable int userId,
                                               @PathVariable String source) {
        double total = incomeService.getTotalBySource(userId, source);
        return ResponseEntity.ok(Map.of("userId", userId, "source", source.toUpperCase(),
                                        "totalIncome", total));
    }

    @Operation(summary = "Update an income entry")
    @PutMapping("/{incomeId}")
    public ResponseEntity<?> updateIncome(@PathVariable int incomeId,
                                          @Valid @RequestBody Income updatedIncome) {
        try {
            Income updated = incomeService.updateIncome(incomeId, updatedIncome);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Delete an income entry")
    @DeleteMapping("/{incomeId}")
    public ResponseEntity<?> deleteIncome(@PathVariable int incomeId) {
        try {
            incomeService.deleteIncome(incomeId);
            return ResponseEntity.ok(success("Income entry deleted successfully"));
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
