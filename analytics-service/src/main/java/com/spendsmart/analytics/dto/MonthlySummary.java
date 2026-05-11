package com.spendsmart.analytics.dto;

import java.io.Serializable;

public class MonthlySummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private int userId;
    private int month;
    private int year;
    private double totalIncome;
    private double totalExpenses;
    private double netSavings;
    private double savingsRate;  // (netSavings / totalIncome) * 100


    public MonthlySummary() {}

    public MonthlySummary(int userId, int month, int year,
                          double totalIncome, double totalExpenses) {
        this.userId = userId;
        this.month = month;
        this.year = year;
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.netSavings = totalIncome - totalExpenses;
        this.savingsRate = totalIncome > 0
                ? Math.round(((totalIncome - totalExpenses) / totalIncome) * 10000.0) / 100.0
                : 0.0;
    }


    public int getUserId() { return userId; }
    public int getMonth() { return month; }
    public int getYear() { return year; }
    public double getTotalIncome() { return totalIncome; }
    public double getTotalExpenses() { return totalExpenses; }
    public double getNetSavings() { return netSavings; }
    public double getSavingsRate() { return savingsRate; }


    public void setUserId(int userId) { this.userId = userId; }
    public void setMonth(int month) { this.month = month; }
    public void setYear(int year) { this.year = year; }
    public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }
    public void setTotalExpenses(double totalExpenses) { this.totalExpenses = totalExpenses; }
    public void setNetSavings(double netSavings) { this.netSavings = netSavings; }
    public void setSavingsRate(double savingsRate) { this.savingsRate = savingsRate; }

    @Override
    public String toString() {
        return "MonthlySummary{userId=" + userId + ", month=" + month +
               ", year=" + year + ", income=" + totalIncome +
               ", expenses=" + totalExpenses + ", savings=" + netSavings + "}";
    }
}
