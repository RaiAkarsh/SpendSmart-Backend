package com.spendsmart.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "users")
@Schema(description = "SpendSmart user profile")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private int userId;

    @Column(nullable = false)
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be 2-100 characters")
    @Schema(example = "Rahul Sharma")
    private String fullName;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(example = "rahul@example.com")
    private String email;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Schema(description = "Use plain password while registering. The service stores it as a BCrypt hash.", example = "Password@123")
    private String passwordHash;

    @Schema(example = "INR", allowableValues = {"INR", "USD", "EUR", "GBP"})
    private String currency = "INR";

    @Schema(example = "Asia/Kolkata")
    private String timezone = "Asia/Kolkata";

    @Schema(example = "https://example.com/avatar.png")
    private String avatarUrl;

    @Schema(example = "LOCAL", allowableValues = {"LOCAL", "GOOGLE"})
    private String provider = "LOCAL";


    @Column(name = "is_active")
    @Schema(example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean isActive = true;

    @Min(value = 0, message = "Monthly budget cannot be negative")
    @Schema(example = "25000")
    private double monthlyBudget;

    @Schema(example = "2026-05-05T11:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors

    public User() {}

    public User(int userId, String fullName, String email, String passwordHash,
                String currency, String timezone, String avatarUrl, String provider,
                boolean isActive, double monthlyBudget, LocalDateTime createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.currency = currency;
        this.timezone = timezone;
        this.avatarUrl = avatarUrl;
        this.provider = provider;
        this.isActive = isActive;
        this.monthlyBudget = monthlyBudget;
        this.createdAt = createdAt;
    }

    // Getters

    public int getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getCurrency() { return currency; }
    public String getTimezone() { return timezone; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getProvider() { return provider; }
    public boolean isActive() { return isActive; }
    public double getMonthlyBudget() { return monthlyBudget; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    //Setters

    public void setUserId(int userId) { this.userId = userId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setMonthlyBudget(double monthlyBudget) { this.monthlyBudget = monthlyBudget; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return "User{userId=" + userId + ", fullName='" + fullName + "', email='" + email +
               "', currency='" + currency + "', isActive=" + isActive + "}";
    }
}
