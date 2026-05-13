package com.spendsmart.budget.serviceimpl;

import com.spendsmart.budget.client.ExpenseClient;
import com.spendsmart.budget.entity.Budget;
import com.spendsmart.budget.entity.BudgetProgress;
import com.spendsmart.budget.repository.BudgetRepository;
import com.spendsmart.budget.service.BudgetService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BudgetServiceImpl implements BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private BudgetAlertPublisher budgetAlertPublisher;

    @Autowired
    private ExpenseClient expenseClient;

    @Value("${expense.service.url}")
    private String expenseServiceUrl;

    @Value("${jwt.secret}")
    private String secret;

    // Creates a new budget.
    // period must be: MONTHLY, WEEKLY, or CUSTOM
    @Override
    @CacheEvict(cacheNames = "budgets", allEntries = true)
    public Budget createBudget(Budget budget) {
        // Normalize period to uppercase
        if (budget.getPeriod() != null) {
            budget.setPeriod(budget.getPeriod().toUpperCase());
        }
        budget.setSpentAmount(fetchCategorySpentAmount(budget.getUserId(), budget.getCategoryId()));
        budget.setActive(true);
        budget.setCreatedAt(LocalDateTime.now());
        Budget saved = budgetRepository.save(budget);
        budgetAlertPublisher.publishIfNeeded(saved);
        return saved;
    }

    @Override
    @Cacheable(cacheNames = "budgets", key = "'id:' + #budgetId")
    public Budget getBudgetById(int budgetId) {
        return budgetRepository.findByBudgetId(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found with id: " + budgetId));
    }

    @Override
    @Cacheable(cacheNames = "budgets", key = "'all'")
    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    @Override
    @Cacheable(cacheNames = "budgets", key = "'user:' + #userId")
    public List<Budget> getBudgetsByUser(int userId) {
        return budgetRepository.findByUserId(userId);
    }

    // Only active budgets shown on the dashboard
    @Override
    @Cacheable(cacheNames = "budgets", key = "'user:' + #userId + ':active'")
    public List<Budget> getActiveBudgets(int userId) {
        return budgetRepository.findByUserIdAndIsActive(userId, true);
    }

    // Updates name, limitAmount, alertThreshold, endDate
    @Override
    @CacheEvict(cacheNames = "budgets", allEntries = true)
    public Budget updateBudget(int budgetId, Budget updatedBudget) {
        Budget existing = getBudgetById(budgetId);
        existing.setName(updatedBudget.getName());
        existing.setLimitAmount(updatedBudget.getLimitAmount());
        existing.setAlertThreshold(updatedBudget.getAlertThreshold());
        existing.setEndDate(updatedBudget.getEndDate());
        if (updatedBudget.getCurrency() != null) {
            existing.setCurrency(updatedBudget.getCurrency());
        }
        Budget saved = budgetRepository.save(existing);
        budgetAlertPublisher.publishIfNeeded(saved);
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "budgets", allEntries = true)
    public void deleteBudget(int budgetId) {
        getBudgetById(budgetId); // verify exists
        budgetRepository.deleteByBudgetId(budgetId);
    }

    // Sets spentAmount to an exact value.
    // Used when expense-service recalculates the total after an edit/delete.
    @Override
    @CacheEvict(cacheNames = "budgets", allEntries = true)
    public Budget updateSpentAmount(int budgetId, double newSpentAmount) {
        Budget budget = getBudgetById(budgetId);
        budget.setSpentAmount(Math.max(0.0, newSpentAmount)); // never go negative
        Budget saved = budgetRepository.save(budget);
        budgetAlertPublisher.publishIfNeeded(saved);
        return saved;
    }

    // Adds to existing spentAmount when a new expense is added.
    @Override
    @CacheEvict(cacheNames = "budgets", allEntries = true)
    public Budget addToSpentAmount(int budgetId, double amount) {
        Budget budget = getBudgetById(budgetId);
        budget.setSpentAmount(budget.getSpentAmount() + amount);
        Budget saved = budgetRepository.save(budget);
        budgetAlertPublisher.publishIfNeeded(saved);
        return saved;
    }

    // Subtracts from spentAmount when an expense is deleted.
    // Never goes below 0.
    @Override
    @CacheEvict(cacheNames = "budgets", allEntries = true)
    public Budget subtractFromSpentAmount(int budgetId, double amount) {
        Budget budget = getBudgetById(budgetId);
        double updated = budget.getSpentAmount() - amount;
        budget.setSpentAmount(Math.max(0.0, updated)); // floor at 0
        Budget saved = budgetRepository.save(budget);
        budgetAlertPublisher.publishIfNeeded(saved);
        return saved;
    }

    // Calculates progress for a single budget:
    //   percentageUsed = spentAmount / limitAmount * 100
    //   remaining      = limitAmount - spentAmount
    //   status         = ON_TRACK / WARNING / EXCEEDED
    @Override
    @Cacheable(cacheNames = "budgets", key = "'progress:' + #budgetId")
    public BudgetProgress getBudgetProgress(int budgetId) {
        Budget budget = getBudgetById(budgetId);
        return new BudgetProgress(budget);
    }

    // Returns progress for ALL active budgets of a user.
    // Used in the dashboard "Budget Overview" widget.
    @Override
    @Cacheable(cacheNames = "budgets", key = "'user:' + #userId + ':progress'")
    public List<BudgetProgress> getAllBudgetProgress(int userId) {
        List<Budget> activeBudgets = budgetRepository.findByUserIdAndIsActive(userId, true);
        return activeBudgets.stream()
                .map(BudgetProgress::new)
                .collect(Collectors.toList());
    }

    // Finds the active budget for a specific category.
    // Called by expense-service to find which budget to update when an
    // expense is added to a category.
    @Override
    @Cacheable(cacheNames = "budgets", key = "'user:' + #userId + ':category:' + #categoryId")
    public Budget getActiveBudgetByCategory(int userId, int categoryId) {
        return budgetRepository
                .findByUserIdAndCategoryIdAndIsActive(userId, categoryId, true)
                .orElseThrow(() -> new RuntimeException(
                    "No active budget found for userId=" + userId +
                    " categoryId=" + categoryId
                ));
    }

    // Closes a budget without deleting it.
    // Historical data is preserved.
    @Override
    @CacheEvict(cacheNames = "budgets", allEntries = true)
    public void deactivateBudget(int budgetId) {
        Budget budget = getBudgetById(budgetId);
        budget.setActive(false);
        budgetRepository.save(budget);
    }

    // Resets spentAmount to 0 for the start of a new budget period.
    // For now: called manually via POST /budgets/{id}/reset
    @Override
    @CacheEvict(cacheNames = "budgets", allEntries = true)
    public void resetBudgetPeriod(int budgetId) {
        Budget budget = getBudgetById(budgetId);
        budget.setSpentAmount(0.0);
        budgetRepository.save(budget);
    }

    private double fetchCategorySpentAmount(int userId, int categoryId) {
        try {
            Map<String, Object> response = expenseClient.getTotalByCategory(
                    userId,
                    categoryId,
                    buildAuthorizationHeader(userId)
            );
            if (response == null) {
                return 0.0;
            }
            Object total = response.get("totalExpenses");
            return total instanceof Number n ? n.doubleValue() : 0.0;
        } catch (Exception e) {
            System.err.println("Spent amount prefill skipped for userId=" + userId
                    + ", categoryId=" + categoryId + ": " + e.getMessage());
            return 0.0;
        }
    }

    private String buildAuthorizationHeader(int userId) {
        return "Bearer " + generateServiceToken(userId);
    }

    private String generateServiceToken(int userId) {
        return Jwts.builder()
                .subject("budget-service")
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
