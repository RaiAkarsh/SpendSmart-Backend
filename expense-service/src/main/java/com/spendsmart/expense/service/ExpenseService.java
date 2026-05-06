package com.spendsmart.expense.service;

import com.spendsmart.expense.entity.Expense;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseService {

    Expense addExpense(Expense expense);

    // Get a single expense by its ID
    Expense getExpenseById(int expenseId);

    // Get all expenses for a user (newest first)
    List<Expense> getExpensesByUser(int userId);

    // Get expenses for a user filtered by category
    List<Expense> getExpensesByCategory(int userId, int categoryId);

    // Get expenses between two dates
    List<Expense> getExpensesByDateRange(int userId, LocalDate startDate, LocalDate endDate);

    // Get expenses for a specific month and year
    List<Expense> getExpensesByMonth(int userId, int month, int year);

    // Get expenses filtered by payment method (CASH, CARD, UPI, BANK_TRANSFER, WALLET)
    List<Expense> getExpensesByPaymentMethod(int userId, String paymentMethod);

    // Search expenses by keyword in title or notes
    List<Expense> searchExpenses(int userId, String keyword);

    // Get only recurring expenses for a user
    List<Expense> getRecurringExpenses(int userId);

    Expense updateExpense(int expenseId, Expense updatedExpense);

    // Delete an expense permanently
    void deleteExpense(int expenseId);

    // Get total amount spent by a user across all time
    double getTotalByUser(int userId);

    // Get total spent in a specific category
    double getTotalByCategory(int userId, int categoryId);

    // Get total spent in a specific month
    double getTotalByMonth(int userId, int month, int year);
}