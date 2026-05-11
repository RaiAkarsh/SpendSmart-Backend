package com.spendsmart.recurring.controller;

import com.spendsmart.recurring.entity.RecurringTransaction;
import com.spendsmart.recurring.service.RecurringService;
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
@RequestMapping("/recurring")
@Tag(name = "Recurring", description = "Recurring expense/income rules and manual processing")
public class RecurringController {

    @Autowired
    private RecurringService recurringService;

    @Operation(summary = "Create a recurring expense or income rule")
    @PostMapping
    public ResponseEntity<?> addRecurring(@Valid @RequestBody RecurringTransaction recurring) {
        try {
            RecurringTransaction saved = recurringService.addRecurring(recurring);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get recurring rules by user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUser(@PathVariable int userId) {
        List<RecurringTransaction> rules = recurringService.getByUser(userId);
        return ResponseEntity.ok(rules);
    }

    @Operation(summary = "Get active recurring rules by user")
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<?> getActive(@PathVariable int userId) {
        List<RecurringTransaction> rules = recurringService.getActiveByUser(userId);
        return ResponseEntity.ok(rules);
    }

    @Operation(summary = "Get recurring rule by id")
    @GetMapping("/{recurringId}")
    public ResponseEntity<?> getById(@PathVariable int recurringId) {
        try {
            return ResponseEntity.ok(recurringService.getById(recurringId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get recurring rules by type")
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<?> getByType(@PathVariable int userId,
                                        @PathVariable String type) {
        List<RecurringTransaction> rules = recurringService.getByUserAndType(userId, type);
        return ResponseEntity.ok(rules);
    }

    @Operation(summary = "Get upcoming recurring rules")
    @GetMapping("/user/{userId}/upcoming")
    public ResponseEntity<?> getUpcoming(@PathVariable int userId) {
        List<RecurringTransaction> rules = recurringService.getUpcomingThisMonth(userId);
        return ResponseEntity.ok(rules);
    }

    @Operation(summary = "Count active recurring rules")
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<?> getCount(@PathVariable int userId) {
        int count = recurringService.countActiveRules(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "activeRules", count));
    }

    @Operation(summary = "Update a recurring rule")
    @PutMapping("/{recurringId}")
    public ResponseEntity<?> updateRecurring(@PathVariable int recurringId,
                                              @Valid @RequestBody RecurringTransaction updated) {
        try {
            RecurringTransaction result = recurringService.updateRecurring(recurringId, updated);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Deactivate a recurring rule")
    @PutMapping("/{recurringId}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable int recurringId) {
        try {
            recurringService.deactivateRecurring(recurringId);
            return ResponseEntity.ok(success("Recurring rule paused  -  no more auto-generation"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Activate a recurring rule")
    @PutMapping("/{recurringId}/activate")
    public ResponseEntity<?> activate(@PathVariable int recurringId) {
        try {
            recurringService.activateRecurring(recurringId);
            return ResponseEntity.ok(success("Recurring rule activated  -  will resume auto-generation"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Manually process due recurring transactions", description = "Use this to test the scheduler immediately. Rules due today will generate expense/income entries and advance nextDueDate.")
    @PostMapping("/process")
    public ResponseEntity<?> triggerProcessing() {
        try {
            recurringService.processDueTransactions();
            return ResponseEntity.ok(success(
                "Processing complete  -  due transactions generated and nextDueDate advanced"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Delete a recurring rule")
    @DeleteMapping("/{recurringId}")
    public ResponseEntity<?> deleteRecurring(@PathVariable int recurringId) {
        try {
            recurringService.deleteRecurring(recurringId);
            return ResponseEntity.ok(success("Recurring rule deleted"));
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
