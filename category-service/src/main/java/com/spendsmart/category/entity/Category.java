package com.spendsmart.category.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int categoryId;

    @Column(nullable = false)
    private int userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    private String icon;

    private String colorCode;

    private double budgetLimit = 0.0;

    @Column(name = "is_default")
    private boolean isDefault = false;

    private LocalDate createdAt = LocalDate.now();

    // 🔹 No-args constructor
    public Category() {
    }

    // 🔹 All-args constructor
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

    // 🔹 Getters and Setters

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