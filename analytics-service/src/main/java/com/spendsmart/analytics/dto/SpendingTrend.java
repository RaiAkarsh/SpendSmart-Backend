package com.spendsmart.analytics.dto;

import java.io.Serializable;

public class SpendingTrend implements Serializable {

    private static final long serialVersionUID = 1L;

    private int month;
    private int year;
    private double totalExpenses;
    private double totalIncome;


    public SpendingTrend() {}

    public SpendingTrend(int month, int year, double totalExpenses, double totalIncome) {
        this.month = month;
        this.year = year;
        this.totalExpenses = totalExpenses;
        this.totalIncome = totalIncome;
    }


    public int getMonth() { return month; }
    public int getYear() { return year; }
    public double getTotalExpenses() { return totalExpenses; }
    public double getTotalIncome() { return totalIncome; }


    public void setMonth(int month) { this.month = month; }
    public void setYear(int year) { this.year = year; }
    public void setTotalExpenses(double totalExpenses) { this.totalExpenses = totalExpenses; }
    public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }

    @Override
    public String toString() {
        return "SpendingTrend{month=" + month + ", year=" + year +
               ", expenses=" + totalExpenses + ", income=" + totalIncome + "}";
    }
}
