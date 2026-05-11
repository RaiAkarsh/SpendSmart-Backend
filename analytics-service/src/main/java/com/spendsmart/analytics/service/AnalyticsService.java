package com.spendsmart.analytics.service;

import com.spendsmart.analytics.dto.CategoryBreakdown;
import com.spendsmart.analytics.dto.FinancialHealthScore;
import com.spendsmart.analytics.dto.MonthlySummary;
import com.spendsmart.analytics.dto.SpendingTrend;

import java.util.List;
import java.util.Map;

public interface AnalyticsService {

    // Monthly Summary: income vs expenses, net savings, savings rate
    MonthlySummary getMonthlySummary(int userId, int month, int year);

    // Yearly Summary: aggregate of all 12 months
    Map<String, Object> getYearlySummary(int userId, int year);

    // Category-wise spending breakdown for a given month (pie chart data)
    List<CategoryBreakdown> getCategoryBreakdown(int userId, int month, int year);

    // Spending trends over multiple months (line chart data)
    List<SpendingTrend> getSpendingTrends(int userId, int months);

    // Income vs Expense comparison for a specific month
    Map<String, Object> getIncomeVsExpense(int userId, int month, int year);

    // All-time totals: total income, total expenses, net position
    Map<String, Object> getAllTimeTotals(int userId);

    // Financial Health Score (0-100 with grade)
    FinancialHealthScore getFinancialHealthScore(int userId);
}