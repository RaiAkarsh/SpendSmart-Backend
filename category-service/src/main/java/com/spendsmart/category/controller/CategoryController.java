package com.spendsmart.category.controller;

import com.spendsmart.category.dto.BudgetLimitRequest;
import com.spendsmart.category.entity.Category;
import com.spendsmart.category.service.CategoryService;
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
@RequestMapping("/categories")
@Tag(name = "Categories", description = "Expense and income category APIs")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Operation(summary = "Create a category")
    @PostMapping
    public ResponseEntity<?> createCategory(@Valid @RequestBody Category category) {
        try {
            Category saved = categoryService.createCategory(category);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get categories by user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUser(@PathVariable int userId) {
        try {
            List<Category> categories = categoryService.getByUserId(userId);
            return ResponseEntity.ok(categories);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get category by id")
    @GetMapping("/{categoryId}")
    public ResponseEntity<?> getById(@PathVariable int categoryId) {
        try {
            return ResponseEntity.ok(categoryService.getCategoryById(categoryId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get categories by user and type")
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<?> getByType(@PathVariable int userId,
                                       @PathVariable String type) {
        try {
            List<Category> categories = categoryService.getByUserAndType(userId, type);
            return ResponseEntity.ok(categories);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Update a category")
    @PutMapping("/{categoryId}")
    public ResponseEntity<?> updateCategory(@PathVariable int categoryId,
                                            @Valid @RequestBody Category updatedCategory) {
        try {
            Category updated = categoryService.updateCategory(categoryId, updatedCategory);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Update category budget limit")
    @PutMapping("/{categoryId}/budget")
    public ResponseEntity<?> setBudget(@PathVariable int categoryId,
                                       @Valid @RequestBody BudgetLimitRequest body) {
        try {
            categoryService.setCategoryBudget(categoryId, body.getBudgetLimit());
            return ResponseEntity.ok(success("Budget limit updated"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Delete a category")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<?> deleteCategory(@PathVariable int categoryId) {
        try {
            categoryService.deleteCategory(categoryId);
            return ResponseEntity.ok(success("Category deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Create default categories for a user")
    @PostMapping("/init/{userId}")
    public ResponseEntity<?> initDefaults(@PathVariable int userId) {
        try {
            categoryService.initDefaultCategories(userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(success("12 default categories created for userId: " + userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get category count by user")
    @GetMapping("/count/{userId}")
    public ResponseEntity<?> getCount(@PathVariable int userId) {
        int count = categoryService.getCategoryCount(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "totalCategories", count));
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
