package com.spendsmart.category.controller;

import com.spendsmart.category.entity.Category;
import com.spendsmart.category.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;


    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Category category) {
        try {
            Category saved = categoryService.createCategory(category);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    //GET /categories/user/{userId}

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUser(@PathVariable int userId) {
        try {
            List<Category> categories = categoryService.getByUserId(userId);
            return ResponseEntity.ok(categories);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    // GET /categories/{categoryId} 
    @GetMapping("/{categoryId}")
    public ResponseEntity<?> getById(@PathVariable int categoryId) {
        try {
            return ResponseEntity.ok(categoryService.getCategoryById(categoryId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    // GET /categories/user/{userId}/type/{type}
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

    //PUT /categories/{categoryId}
    @PutMapping("/{categoryId}")
    public ResponseEntity<?> updateCategory(@PathVariable int categoryId,
                                            @RequestBody Category updatedCategory) {
        try {
            Category updated = categoryService.updateCategory(categoryId, updatedCategory);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    //PUT /categories/{categoryId}/budget
    @PutMapping("/{categoryId}/budget")
    public ResponseEntity<?> setBudget(@PathVariable int categoryId,
                                       @RequestBody Map<String, Double> body) {
        try {
            categoryService.setCategoryBudget(categoryId, body.get("budgetLimit"));
            return ResponseEntity.ok(success("Budget limit updated"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    //DELETE /categories/{categoryId}
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<?> deleteCategory(@PathVariable int categoryId) {
        try {
            categoryService.deleteCategory(categoryId);
            return ResponseEntity.ok(success("Category deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    //POST /categories/init/{userId}
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

    //GET /categories/count/{userId}
    @GetMapping("/count/{userId}")
    public ResponseEntity<?> getCount(@PathVariable int userId) {
        int count = categoryService.getCategoryCount(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "totalCategories", count));
    }

    //private helpers
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
