package com.spendsmart.recurring.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_transactions")
@Schema(description = "Recurring expense or income rule")
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private int recurringId;

    @Column(nullable = false)
    @Schema(example = "1")
    private int userId;

    @Column(nullable = false)
    @Schema(example = "1")
    private int categoryId;

    @Column(nullable = false)
    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 100, message = "Title must be 2-100 characters")
    @Schema(example = "Monthly Netflix")
    private String title;

    @Column(nullable = false)
    @Min(value = 0, message = "Amount must be positive")
    @Schema(example = "649")
    private double amount;

    @Column(nullable = false)
    @NotBlank(message = "Type is required (EXPENSE or INCOME)")
    @Schema(example = "EXPENSE", allowableValues = {"EXPENSE", "INCOME"})
    private String type;

    @Column(nullable = false)
    @NotBlank(message = "Frequency is required (DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY)")
    @Schema(example = "MONTHLY", allowableValues = {"DAILY", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"})
    private String frequency;

    @Schema(example = "INR", allowableValues = {"INR", "USD", "EUR", "GBP"})
    private String currency = "INR";

    @Column(nullable = false)
    @NotNull(message = "Start date is required")
    @Schema(example = "2026-05-05")
    private LocalDate startDate;

    @Schema(example = "2026-12-31")
    private LocalDate endDate;

    @Column(nullable = false)
    @Schema(example = "2026-05-05")
    private LocalDate nextDueDate;

    @Column(name = "is_active")
    @Schema(example = "true")
    private boolean isActive = true;

    @Schema(example = "Netflix subscription auto payment")
    private String description;

    @Schema(example = "CARD", allowableValues = {"CASH", "CARD", "UPI", "BANK_TRANSFER", "WALLET"})
    private String paymentMethod;

    @Schema(example = "SALARY", allowableValues = {"SALARY", "FREELANCE", "BUSINESS", "INVESTMENT", "GIFT", "OTHER"})
    private String source;

    @Schema(example = "2026-05-05T11:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Schema(example = "14", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer lastGeneratedExpenseId;

    @Schema(example = "9", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer lastGeneratedIncomeId;

    @Schema(example = "2026-05-05", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDate lastGeneratedDate;


    public RecurringTransaction() {}

    public RecurringTransaction(int recurringId, int userId, int categoryId,
                                String title, double amount, String type,
                                String frequency, String currency,
                                LocalDate startDate, LocalDate endDate,
                                LocalDate nextDueDate, boolean isActive,
                                String description, String paymentMethod,
                                String source, LocalDateTime createdAt,
                                Integer lastGeneratedExpenseId,
                                Integer lastGeneratedIncomeId,
                                LocalDate lastGeneratedDate) {
        this.recurringId = recurringId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.frequency = frequency;
        this.currency = currency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.nextDueDate = nextDueDate;
        this.isActive = isActive;
        this.description = description;
        this.paymentMethod = paymentMethod;
        this.source = source;
        this.createdAt = createdAt;
        this.lastGeneratedExpenseId = lastGeneratedExpenseId;
        this.lastGeneratedIncomeId = lastGeneratedIncomeId;
        this.lastGeneratedDate = lastGeneratedDate;
    }


    public int getRecurringId() { return recurringId; }
    public int getUserId() { return userId; }
    public int getCategoryId() { return categoryId; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public String getFrequency() { return frequency; }
    public String getCurrency() { return currency; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDate getNextDueDate() { return nextDueDate; }
    public boolean isActive() { return isActive; }
    public String getDescription() { return description; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getSource() { return source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Integer getLastGeneratedExpenseId() { return lastGeneratedExpenseId; }
    public Integer getLastGeneratedIncomeId() { return lastGeneratedIncomeId; }
    public LocalDate getLastGeneratedDate() { return lastGeneratedDate; }


    public void setRecurringId(int recurringId) { this.recurringId = recurringId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public void setTitle(String title) { this.title = title; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setType(String type) { this.type = type; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setDescription(String description) { this.description = description; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setSource(String source) { this.source = source; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setLastGeneratedExpenseId(Integer lastGeneratedExpenseId) { this.lastGeneratedExpenseId = lastGeneratedExpenseId; }
    public void setLastGeneratedIncomeId(Integer lastGeneratedIncomeId) { this.lastGeneratedIncomeId = lastGeneratedIncomeId; }
    public void setLastGeneratedDate(LocalDate lastGeneratedDate) { this.lastGeneratedDate = lastGeneratedDate; }

    @Override
    public String toString() {
        return "RecurringTransaction{id=" + recurringId + ", title='" + title +
               "', type=" + type + ", freq=" + frequency +
               ", nextDue=" + nextDueDate + ", active=" + isActive + "}";
    }
}
