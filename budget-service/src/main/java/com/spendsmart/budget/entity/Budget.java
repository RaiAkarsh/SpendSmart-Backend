package com.spendsmart.budget.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "budgets")
@Schema(description = "Category budget")
public class Budget implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private int budgetId;

    @Column(nullable = false)
    @Schema(example = "1")
    private int userId;

    @Column(nullable = false)
    @Schema(example = "1")
    private int categoryId;

    @Column(nullable = false)
    @NotBlank(message = "Budget name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    @Schema(example = "May Food Budget")
    private String name;

    @Column(nullable = false)
    @Min(value = 1, message = "Limit amount must be at least 1")
    @Schema(example = "5000")
    private double limitAmount;

    @Schema(example = "INR", allowableValues = {"INR", "USD", "EUR", "GBP"})
    private String currency = "INR";

    @Column(nullable = false)
    @NotBlank(message = "Period is required (MONTHLY, WEEKLY, CUSTOM)")
    @Schema(example = "MONTHLY", allowableValues = {"MONTHLY", "WEEKLY", "CUSTOM"})
    private String period;

    @Column(nullable = false)
    @NotNull(message = "Start date is required")
    @Schema(example = "2026-05-01")
    private LocalDate startDate;

    @Schema(example = "2026-05-31")
    private LocalDate endDate;

    @Schema(example = "1500")
    private double spentAmount = 0.0;

    @Min(value = 1, message = "Alert threshold must be between 1 and 100")
    @Max(value = 100, message = "Alert threshold must be between 1 and 100")
    @Schema(example = "80")
    private int alertThreshold = 80;

    @Column(name = "is_active")
    @Schema(example = "true")
    private boolean isActive = true;

    @Schema(example = "2026-05-05T11:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt = LocalDateTime.now();


    public Budget() {}

    public Budget(int budgetId, int userId, int categoryId, String name,
                  double limitAmount, String currency, String period,
                  LocalDate startDate, LocalDate endDate, double spentAmount,
                  int alertThreshold, boolean isActive, LocalDateTime createdAt) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.name = name;
        this.limitAmount = limitAmount;
        this.currency = currency;
        this.period = period;
        this.startDate = startDate;
        this.endDate = endDate;
        this.spentAmount = spentAmount;
        this.alertThreshold = alertThreshold;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }


    public int getBudgetId() { return budgetId; }
    public int getUserId() { return userId; }
    public int getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public double getLimitAmount() { return limitAmount; }
    public String getCurrency() { return currency; }
    public String getPeriod() { return period; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public double getSpentAmount() { return spentAmount; }
    public int getAlertThreshold() { return alertThreshold; }
    public boolean isActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }


    public void setBudgetId(int budgetId) { this.budgetId = budgetId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public void setName(String name) { this.name = name; }
    public void setLimitAmount(double limitAmount) { this.limitAmount = limitAmount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setPeriod(String period) { this.period = period; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setSpentAmount(double spentAmount) { this.spentAmount = spentAmount; }
    public void setAlertThreshold(int alertThreshold) { this.alertThreshold = alertThreshold; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Budget{budgetId=" + budgetId + ", name='" + name + "'" +
               ", spent=" + spentAmount + "/" + limitAmount + ", period=" + period + "}";
    }
}
