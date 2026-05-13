package com.spendsmart.analytics.dto;

import java.io.Serializable;

public class CategoryBreakdown implements Serializable {

    private static final long serialVersionUID = 1L;

    private int categoryId;
    private String categoryName;
    private double totalSpent;
    private double percentage;  // (totalSpent / totalAllCategories) * 100


    public CategoryBreakdown() {}

    public CategoryBreakdown(int categoryId, String categoryName,
                             double totalSpent, double percentage) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.totalSpent = totalSpent;
        this.percentage = Math.round(percentage * 100.0) / 100.0;
    }


    public int getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public double getTotalSpent() { return totalSpent; }
    public double getPercentage() { return percentage; }


    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    @Override
    public String toString() {
        return "CategoryBreakdown{categoryId=" + categoryId +
               ", name='" + categoryName + "', spent=" + totalSpent +
               ", pct=" + percentage + "%}";
    }
}
