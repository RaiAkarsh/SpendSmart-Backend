package com.spendsmart.budget.entity;

import java.io.Serializable;

public class BudgetProgress implements Serializable {

    private static final long serialVersionUID = 1L;

    private int budgetId;
    private String name;
    private double limitAmount;
    private double spentAmount;
    private double remaining;
    private double percentageUsed;
    private int alertThreshold;
    private boolean alertTriggered;
    private boolean exceeded;
    private String status; // "ON_TRACK", "WARNING", "EXCEEDED"

    public BudgetProgress() {}

    public BudgetProgress(Budget budget) {
        this.budgetId = budget.getBudgetId();
        this.name = budget.getName();
        this.limitAmount = budget.getLimitAmount();
        this.spentAmount = budget.getSpentAmount();
        this.remaining = budget.getLimitAmount() - budget.getSpentAmount();
        this.percentageUsed = budget.getLimitAmount() > 0
                ? (budget.getSpentAmount() / budget.getLimitAmount()) * 100
                : 0.0;
        this.alertThreshold = budget.getAlertThreshold();
        this.alertTriggered = this.percentageUsed >= budget.getAlertThreshold();
        this.exceeded = budget.getSpentAmount() > budget.getLimitAmount();

        if (this.exceeded) {
            this.status = "EXCEEDED";
        } else if (this.alertTriggered) {
            this.status = "WARNING";
        } else {
            this.status = "ON_TRACK";
        }
    }

    // Getters
    public int getBudgetId() { return budgetId; }
    public String getName() { return name; }
    public double getLimitAmount() { return limitAmount; }
    public double getSpentAmount() { return spentAmount; }
    public double getRemaining() { return remaining; }
    public double getPercentageUsed() { return Math.round(percentageUsed * 100.0) / 100.0; }
    public int getAlertThreshold() { return alertThreshold; }
    public boolean isAlertTriggered() { return alertTriggered; }
    public boolean isExceeded() { return exceeded; }
    public String getStatus() { return status; }

    // Setters
    public void setBudgetId(int budgetId) { this.budgetId = budgetId; }
    public void setName(String name) { this.name = name; }
    public void setLimitAmount(double limitAmount) { this.limitAmount = limitAmount; }
    public void setSpentAmount(double spentAmount) { this.spentAmount = spentAmount; }
    public void setRemaining(double remaining) { this.remaining = remaining; }
    public void setPercentageUsed(double percentageUsed) { this.percentageUsed = percentageUsed; }
    public void setAlertThreshold(int alertThreshold) { this.alertThreshold = alertThreshold; }
    public void setAlertTriggered(boolean alertTriggered) { this.alertTriggered = alertTriggered; }
    public void setExceeded(boolean exceeded) { this.exceeded = exceeded; }
    public void setStatus(String status) { this.status = status; }
}
