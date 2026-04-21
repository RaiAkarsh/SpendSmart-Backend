package com.spendsmart.auth.controller;

import com.spendsmart.auth.entity.User;
import com.spendsmart.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    //  POST /auth/register
       @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User saved = authService.register(user);
            saved.setPasswordHash(null);  // never return the hash to client
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }
    //  POST /auth/login

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String token = authService.login(
                    credentials.get("email"),
                    credentials.get("password")
            );
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }
    //  GET /auth/profile/{userId}
    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable int userId) {
        try {
            User user = authService.getUserById(userId);
            user.setPasswordHash(null);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(error("User not found with id: " + userId));
        }
    }
    //  PUT /auth/profile/{userId}
    @PutMapping("/profile/{userId}")
    public ResponseEntity<?> updateProfile(@PathVariable int userId,
                                           @RequestBody User updatedUser) {
        try {
            User updated = authService.updateProfile(userId, updatedUser);
            updated.setPasswordHash(null);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }
    //  PUT /auth/password/{userId}
    @PutMapping("/password/{userId}")
    public ResponseEntity<?> changePassword(@PathVariable int userId,
                                            @RequestBody Map<String, String> body) {
        try {
            authService.changePassword(
                    userId,
                    body.get("currentPassword"),
                    body.get("newPassword")
            );
            return ResponseEntity.ok(success("Password changed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }


    //  PUT /auth/currency/{userId}
    @PutMapping("/currency/{userId}")
    public ResponseEntity<?> updateCurrency(@PathVariable int userId,
                                            @RequestBody Map<String, String> body) {
        try {
            authService.updateCurrency(userId, body.get("currency"));
            return ResponseEntity.ok(success("Currency updated to " + body.get("currency")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }


    //  PUT /auth/budget/{userId}

    @PutMapping("/budget/{userId}")
    public ResponseEntity<?> updateMonthlyBudget(@PathVariable int userId,
                                                 @RequestBody Map<String, Double> body) {
        try {
            authService.updateMonthlyBudget(userId, body.get("monthlyBudget"));
            return ResponseEntity.ok(success("Monthly budget updated"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }
    //  DELETE /auth/deactivate/{userId}
    //  Soft-deletes account (isActive=false, data preserved in DB)
    @DeleteMapping("/deactivate/{userId}")
    public ResponseEntity<?> deactivateAccount(@PathVariable int userId) {
        try {
            authService.deactivateAccount(userId);
            return ResponseEntity.ok(success("Account deactivated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    //helpers

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
