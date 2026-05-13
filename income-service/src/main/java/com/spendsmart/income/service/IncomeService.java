package com.spendsmart.income.service;

import com.spendsmart.income.entity.Income;

import java.time.LocalDate;
import java.util.List;

public interface IncomeService {

    // Add a new income entry
    Income addIncome(Income income);

    // Get a single income entry by ID
    Income getIncomeById(int incomeId);

    // Get all income entries for a user (newest first)
    List<Income> getIncomesByUser(int userId);

    // Filter by source: SALARY, FREELANCE, BUSINESS, INVESTMENT, GIFT, OTHER
    List<Income> getIncomesBySource(int userId, String source);

    // Filter by category (e.g. all entries under "Salary" category)
    List<Income> getIncomesByCategory(int userId, int categoryId);

    // Get income entries between two dates
    List<Income> getIncomesByDateRange(int userId, LocalDate startDate, LocalDate endDate);

    // Get income entries for a specific month and year
    List<Income> getIncomesByMonth(int userId, int month, int year);

    // Search income entries by keyword in title or notes
    List<Income> searchIncomes(int userId, String keyword);

    // Get only recurring income entries for a user
    List<Income> getRecurringIncomes(int userId);

    // Update an existing income entry
    Income updateIncome(int incomeId, Income updatedIncome);

    // Delete an income entry permanently
    void deleteIncome(int incomeId);

    // Total income across all time for a user
    double getTotalByUser(int userId);

    // Total income for a specific month
    double getTotalByMonth(int userId, int month, int year);

    // Total income from a specific source (e.g. total SALARY received)
    double getTotalBySource(int userId, String source);
}