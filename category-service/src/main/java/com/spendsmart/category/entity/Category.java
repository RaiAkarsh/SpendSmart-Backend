package com.spendsmart.category.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "categories")
@Schema(description = "Expense or income category")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private int categoryId;

    @Column(nullable = false)
    @Schema(example = "1")
    private int userId;

    @Column(nullable = false)
    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 50, message = "Name must be 2-50 characters")
    @Schema(example = "Food")
    private String name;

    @Column(nullable = false)
    @NotBlank(message = "Category type is required (EXPENSE or INCOME)")
    @Schema(example = "EXPENSE", allowableValues = {"EXPENSE", "INCOME"})
    private String type;

    @Schema(example = "restaurant")
    private String icon;

    @Schema(example = "#22C55E")
    private String colorCode;

    @Min(value = 0, message = "Budget limit cannot be negative")
    @Schema(example = "5000")
    private double budgetLimit = 0.0;

    @Column(name = "is_default")
    @Schema(example = "false", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean isDefault = false;

    @Schema(example = "2026-05-05", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDate createdAt = LocalDate.now();

    public Category() {
    }

    public Category(int categoryId, int userId, String name, String type,
                    String icon, String colorCode, double budgetLimit,
                    boolean isDefault, LocalDate createdAt) {
        this.categoryId = categoryId;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.icon = icon;
        this.colorCode = colorCode;
        this.budgetLimit = budgetLimit;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
    }


    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public double getBudgetLimit() {
        return budgetLimit;
    }

    public void setBudgetLimit(double budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
}
