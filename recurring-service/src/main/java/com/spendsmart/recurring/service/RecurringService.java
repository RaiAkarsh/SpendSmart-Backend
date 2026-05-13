package com.spendsmart.recurring.service;

import com.spendsmart.recurring.entity.RecurringTransaction;

import java.util.List;

public interface RecurringService {

    // Create a new recurring rule
    RecurringTransaction addRecurring(RecurringTransaction recurring);

    // Get a single rule by ID
    RecurringTransaction getById(int recurringId);

    // All rules for a user
    List<RecurringTransaction> getByUser(int userId);

    // Only active rules for a user
    List<RecurringTransaction> getActiveByUser(int userId);

    // Filter by type: EXPENSE or INCOME
    List<RecurringTransaction> getByUserAndType(int userId, String type);

    // Upcoming due transactions for the current month (dashboard reminder)
    List<RecurringTransaction> getUpcomingThisMonth(int userId);

    RecurringTransaction updateRecurring(int recurringId, RecurringTransaction updated);

    void deactivateRecurring(int recurringId);

    // Resume a paused rule
    void activateRecurring(int recurringId);

    // Permanently delete a rule
    void deleteRecurring(int recurringId);

    // Called by @Scheduled job every day at midnight
    // Finds all rules due today, calls expense/income service, advances nextDueDate
    void processDueTransactions();

    // Count active rules for a user
    int countActiveRules(int userId);
}