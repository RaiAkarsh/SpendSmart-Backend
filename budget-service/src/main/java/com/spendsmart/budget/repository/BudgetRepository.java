package com.spendsmart.budget.repository;

import com.spendsmart.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Integer> {

    // All budgets for a user
    List<Budget> findByUserId(int userId);

    // All ACTIVE budgets for a user (shown on dashboard)
    List<Budget> findByUserIdAndIsActive(int userId, boolean isActive);

    // Budget by its own ID
    Optional<Budget> findByBudgetId(int budgetId);

    // All budgets for a specific category (one user can have multiple periods)
    List<Budget> findByUserIdAndCategoryId(int userId, int categoryId);

    // Filter by period type: MONTHLY, WEEKLY, CUSTOM
    List<Budget> findByUserIdAndPeriod(int userId, String period);

    // Count budgets for a user
    int countByUserId(int userId);

    // Delete by budgetId (used with @Transactional)
    void deleteByBudgetId(int budgetId);

    // Find active budget for a specific category (most common query)
    Optional<Budget> findByUserIdAndCategoryIdAndIsActive(int userId, int categoryId, boolean isActive);
}