package com.spendsmart.expense.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Schema(description = "Expense transaction")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private int expenseId;

    @Column(nullable = false)
    @Schema(example = "1")
    private int userId;

    @Column(nullable = false)
    @Schema(example = "1")
    private int categoryId;

    @Column(nullable = false)
    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 100, message = "Title must be 2-100 characters")
    @Schema(example = "Grocery shopping")
    private String title;

    @Column(nullable = false)
    @Min(value = 0, message = "Amount must be positive")
    @Schema(example = "1500")
    private double amount;

    @Schema(example = "INR", allowableValues = {"INR", "USD", "EUR", "GBP"})
    private String currency = "INR";

    @Schema(example = "EXPENSE", allowableValues = {"EXPENSE", "SPLIT"})
    private String type = "EXPENSE";

    @Schema(example = "UPI", allowableValues = {"CASH", "CARD", "UPI", "BANK_TRANSFER", "WALLET"})
    private String paymentMethod;

    @Column(nullable = false)
    @NotNull(message = "Date is required")
    @Schema(example = "2026-05-05")
    private LocalDate date;

    @Schema(example = "Weekly groceries from supermarket")
    private String notes;

    @Schema(example = "https://example.com/receipts/grocery.png")
    private String receiptUrl;

    @Column(name = "is_recurring")
    @Schema(example = "false")
    private boolean isRecurring = false;

    @Column(name = "is_default")
    @Schema(example = "false", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean isDefault = false;

    @Schema(example = "2026-05-05T11:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Schema(example = "2026-05-05T11:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt = LocalDateTime.now();


    public Expense() {}

    public Expense(int expenseId, int userId, int categoryId, String title,
                   double amount, String currency, String type, String paymentMethod,
                   LocalDate date, String notes, String receiptUrl,
                   boolean isRecurring, Boolean isDefault, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.expenseId = expenseId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.title = title;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.paymentMethod = paymentMethod;
        this.date = date;
        this.notes = notes;
        this.receiptUrl = receiptUrl;
        this.isRecurring = isRecurring;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    public int getExpenseId() { return expenseId; }
    public int getUserId() { return userId; }
    public int getCategoryId() { return categoryId; }
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getType() { return type; }
    public String getPaymentMethod() { return paymentMethod; }
    public LocalDate getDate() { return date; }
    public String getNotes() { return notes; }
    public String getReceiptUrl() { return receiptUrl; }
    public boolean isRecurring() { return isRecurring; }
    public boolean isDefault() { return Boolean.TRUE.equals(isDefault); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }


    public void setExpenseId(int expenseId) { this.expenseId = expenseId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public void setTitle(String title) { this.title = title; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setType(String type) { this.type = type; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }
    public void setRecurring(boolean recurring) { this.isRecurring = recurring; }
    public void setDefault(boolean aDefault) { this.isDefault = aDefault; }

    @PrePersist
    @PreUpdate
    private void normalizeFlags() {
        if (isDefault == null) {
            isDefault = false;
        }
    }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Expense{expenseId=" + expenseId + ", userId=" + userId +
               ", title='" + title + "', amount=" + amount + ", date=" + date + "}";
    }
}
