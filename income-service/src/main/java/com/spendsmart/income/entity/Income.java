package com.spendsmart.income.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "incomes")
@Schema(description = "Income transaction")
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private int incomeId;

    @Column(nullable = false)
    @Schema(example = "1")
    private int userId;

    @Column(nullable = false)
    @Schema(example = "9")
    private int categoryId;

    @Column(nullable = false)
    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 100, message = "Title must be 2-100 characters")
    @Schema(example = "April salary")
    private String title;

    @Column(nullable = false)
    @Min(value = 0, message = "Amount must be positive")
    @Schema(example = "75000")
    private double amount;

    @Schema(example = "INR", allowableValues = {"INR", "USD", "EUR", "GBP"})
    private String currency = "INR";

    @Schema(example = "SALARY", allowableValues = {"SALARY", "FREELANCE", "BUSINESS", "INVESTMENT", "GIFT", "OTHER"})
    private String source;

    @Column(nullable = false)
    @NotNull(message = "Date is required")
    @Schema(example = "2026-05-01")
    private LocalDate date;

    @Schema(example = "Monthly salary credited")
    private String notes;

    @Column(name = "is_recurring")
    @Schema(example = "true")
    private boolean isRecurring = false;

    @Schema(example = "MONTHLY", allowableValues = {"DAILY", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"})
    private String recurrencePeriod;

    @Schema(example = "2026-05-05T11:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt = LocalDateTime.now();


    public Income() {}

    public Income(int incomeId, int userId, int categoryId, String title,
                  double amount, String currency, String source, LocalDate date,
                  String notes, boolean isRecurring, String recurrencePeriod,
                  LocalDateTime createdAt) {
        this.incomeId = incomeId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.title = title;
        this.amount = amount;
        this.currency = currency;
        this.source = source;
        this.date = date;
        this.notes = notes;
        this.isRecurring = isRecurring;
        this.recurrencePeriod = recurrencePeriod;
        this.createdAt = createdAt;
    }


    public int getIncomeId() { return incomeId; }
    public int getUserId() { return userId; }
    public int getCategoryId() { return categoryId; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getSource() { return source; }
    public LocalDate getDate() { return date; }
    public String getNotes() { return notes; }
    public boolean isRecurring() { return isRecurring; }
    public String getRecurrencePeriod() { return recurrencePeriod; }
    public LocalDateTime getCreatedAt() { return createdAt; }


    public void setIncomeId(int incomeId) { this.incomeId = incomeId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public void setTitle(String title) { this.title = title; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setSource(String source) { this.source = source; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setRecurring(boolean recurring) { this.isRecurring = recurring; }
    public void setRecurrencePeriod(String recurrencePeriod) { this.recurrencePeriod = recurrencePeriod; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Income{incomeId=" + incomeId + ", userId=" + userId +
               ", title='" + title + "', amount=" + amount +
               ", source='" + source + "', date=" + date + "}";
    }
}
