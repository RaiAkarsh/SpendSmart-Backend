package com.spendsmart.auth.controller;

import com.spendsmart.auth.dto.ChangePasswordRequest;
import com.spendsmart.auth.dto.CurrencyUpdateRequest;
import com.spendsmart.auth.dto.GoogleLoginRequest;
import com.spendsmart.auth.dto.LoginRequest;
import com.spendsmart.auth.dto.MonthlyBudgetUpdateRequest;
import com.spendsmart.auth.entity.User;
import com.spendsmart.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authentication and user profile APIs")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "Register a new user", description = "Use passwordHash as the plain password in this request. The service stores it securely as BCrypt.")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user) {
        try {
            User saved = authService.register(user);
            saved.setPasswordHash(null);  // never return the hash to client
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }
    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest credentials) {
        try {
            String token = authService.login(
                    credentials.getEmail(),
                    credentials.getPassword()
            );
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Login with Google ID token")
    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            String token = authService.loginWithGoogle(request.getIdToken());
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Google login successful");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }
    @Operation(summary = "Get user profile by user id")
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
    @Operation(summary = "Update user profile")
    @PutMapping("/profile/{userId}")
    public ResponseEntity<?> updateProfile(@PathVariable int userId,
                                           @Valid @RequestBody User updatedUser) {
        try {
            User updated = authService.updateProfile(userId, updatedUser);
            updated.setPasswordHash(null);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }
    @Operation(summary = "Change user password")
    @PutMapping("/password/{userId}")
    public ResponseEntity<?> changePassword(@PathVariable int userId,
                                            @Valid @RequestBody ChangePasswordRequest body) {
        try {
            authService.changePassword(
                    userId,
                    body.getCurrentPassword(),
                    body.getNewPassword()
            );
            return ResponseEntity.ok(success("Password changed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Update preferred currency")
    @PutMapping("/currency/{userId}")
    public ResponseEntity<?> updateCurrency(@PathVariable int userId,
                                            @Valid @RequestBody CurrencyUpdateRequest body) {
        try {
            authService.updateCurrency(userId, body.getCurrency());
            return ResponseEntity.ok(success("Currency updated to " + body.getCurrency()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Update monthly budget")
    @PutMapping("/budget/{userId}")
    public ResponseEntity<?> updateMonthlyBudget(@PathVariable int userId,
                                                 @Valid @RequestBody MonthlyBudgetUpdateRequest body) {
        try {
            authService.updateMonthlyBudget(userId, body.getMonthlyBudget());
            return ResponseEntity.ok(success("Monthly budget updated"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }
    @Operation(summary = "Deactivate account", description = "Soft-deletes account by setting isActive=false. Existing data stays in the database.")
    @DeleteMapping("/deactivate/{userId}")
    public ResponseEntity<?> deactivateAccount(@PathVariable int userId) {
        try {
            authService.deactivateAccount(userId);
            return ResponseEntity.ok(success("Account deactivated successfully"));
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
