package com.spendsmart.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    // Stores BCrypt hash
    @Column(nullable = false)
    private String passwordHash;

    // INR, USD, EUR, GBP
    private String currency = "INR";

    // Asia/Kolkata, America/New_York, etc.
    private String timezone = "Asia/Kolkata";

    private String avatarUrl;

    // LOCAL = email+password | GOOGLE = OAuth2
    private String provider = "LOCAL";


    @Column(name = "is_active")
    private boolean isActive = true;

    private double monthlyBudget;

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
