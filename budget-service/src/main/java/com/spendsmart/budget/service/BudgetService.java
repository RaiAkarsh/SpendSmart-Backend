package com.spendsmart.budget.service;

import com.spendsmart.budget.entity.Budget;
import com.spendsmart.budget.entity.BudgetProgress;

import java.util.List;

public interface BudgetService {

    Budget createBudget(Budget budget);

    Budget getBudgetById(int budgetId);

    List<Budget> getAllBudgets();

    List<Budget> getBudgetsByUser(int userId);

    List<Budget> getActiveBudgets(int userId);

    Budget updateBudget(int budgetId, Budget updatedBudget);

    void deleteBudget(int budgetId);

    Budget updateSpentAmount(int budgetId, double newSpentAmount);

    // Add to existing spentAmount (called when expense is added)
    Budget addToSpentAmount(int budgetId, double amount);

    // Subtract from spentAmount (called when expense is deleted)
    Budget subtractFromSpentAmount(int budgetId, double amount);

    // Returns progress info: percentageUsed, remaining, status, alertTriggered
    BudgetProgress getBudgetProgress(int budgetId);

    // All progress for all active budgets of a user (dashboard view)
    List<BudgetProgress> getAllBudgetProgress(int userId);

    // Find the active budget for a specific category
    Budget getActiveBudgetByCategory(int userId, int categoryId);

    // Deactivate a budget (close the period without deleting)
    void deactivateBudget(int budgetId);

    // Reset spentAmount to 0 (start of new period)
    void resetBudgetPeriod(int budgetId);
}