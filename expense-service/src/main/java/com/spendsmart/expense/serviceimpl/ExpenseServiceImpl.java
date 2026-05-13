package com.spendsmart.expense.serviceimpl;

import com.spendsmart.expense.client.BudgetClient;
import com.spendsmart.expense.entity.Expense;
import com.spendsmart.expense.repository.ExpenseRepository;
import com.spendsmart.expense.service.ExpenseService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetClient budgetClient;

    @Value("${budget.service.url}")
    private String budgetServiceUrl;

    @Value("${jwt.secret}")
    private String secret;

    // Saves a new expense to MySQL.
    // expenseId is assigned by MySQL AUTO_INCREMENT.
    // createdAt and updatedAt are set to now() in the entity defaults,
    // but we explicitly set them here for clarity.
    @Override
    public Expense addExpense(Expense expense) {
        expense.setCreatedAt(LocalDateTime.now());
        expense.setUpdatedAt(LocalDateTime.now());
        Expense saved = expenseRepository.save(expense);
        syncBudgetSpentAmount(saved.getUserId(), saved.getCategoryId());
        return saved;
    }

    @Override
    public Expense getExpenseById(int expenseId) {
        return expenseRepository.findByExpenseId(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + expenseId));
    }

    // Returns all expenses for a user, newest first.
    // Used in the main expense list screen.
    @Override
    public List<Expense> getExpensesByUser(int userId) {
        return expenseRepository.findByUserIdOrderByDateDesc(userId);
    }

    @Override
    public List<Expense> getExpensesByCategory(int userId, int categoryId) {
        return expenseRepository.findByUserIdAndCategoryId(userId, categoryId);
    }

    // Returns expenses between startDate and endDate inclusive.
    // Used for custom date range filtering.
    @Override
    public List<Expense> getExpensesByDateRange(int userId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("startDate cannot be after endDate");
        }
        return expenseRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
    }

    // Returns all expenses for a specific month/year.
    // Used for the Monthly Summary Dashboard.
    @Override
    public List<Expense> getExpensesByMonth(int userId, int month, int year) {
        return expenseRepository.findByUserIdAndMonth(userId, month, year);
    }

    // Filters by payment method: CASH, CARD, UPI, BANK_TRANSFER, WALLET
    @Override
    public List<Expense> getExpensesByPaymentMethod(int userId, String paymentMethod) {
        return expenseRepository.findByUserIdAndPaymentMethod(
                userId, paymentMethod.toUpperCase()
        );
    }

    // Full-text search across title and notes fields.
    @Override
    public List<Expense> searchExpenses(int userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getExpensesByUser(userId);
        }
        return expenseRepository.searchByKeyword(userId, keyword.trim());
    }

    // Returns only expenses marked as recurring.
    // Used by recurring-service to find templates for auto-generation.
    @Override
    public List<Expense> getRecurringExpenses(int userId) {
        return expenseRepository.findByUserIdAndIsRecurring(userId, true);
    }

    // Updates all editable fields of an expense.
    // After updating, updatedAt is refreshed to now().
    // This timestamp is used by analytics-service to detect recent changes.
    @Override
    public Expense updateExpense(int expenseId, Expense updatedExpense) {
        Expense existing = getExpenseById(expenseId);
        int previousCategoryId = existing.getCategoryId();

        existing.setCategoryId(updatedExpense.getCategoryId());
        existing.setTitle(updatedExpense.getTitle());
        existing.setAmount(updatedExpense.getAmount());
        existing.setCurrency(updatedExpense.getCurrency());
        existing.setPaymentMethod(updatedExpense.getPaymentMethod());
        existing.setDate(updatedExpense.getDate());
        existing.setNotes(updatedExpense.getNotes());
        existing.setReceiptUrl(updatedExpense.getReceiptUrl());
        existing.setRecurring(updatedExpense.isRecurring());
        existing.setUpdatedAt(LocalDateTime.now());   // always refresh this

        Expense saved = expenseRepository.save(existing);
        syncBudgetSpentAmount(saved.getUserId(), previousCategoryId);
        if (previousCategoryId != saved.getCategoryId()) {
            syncBudgetSpentAmount(saved.getUserId(), saved.getCategoryId());
        }
        return saved;
    }

    // Permanently removes the expense from the database.
    @Override
    @Transactional
    public void deleteExpense(int expenseId) {
        Expense existing = getExpenseById(expenseId);
        if (existing.isDefault()) {
            throw new RuntimeException("Default expenses cannot be deleted.");
        }
        expenseRepository.deleteByExpenseId(expenseId);
        syncBudgetSpentAmount(existing.getUserId(), existing.getCategoryId());
    }

    // SUM(amount) for all expenses of a user.
    @Override
    public double getTotalByUser(int userId) {
        Double result = expenseRepository.sumAmountByUserId(userId);
        return result == null ? 0.0 : result;
    }

    // Used by budget-service to check how much has been spent in a category.
    @Override
    public double getTotalByCategory(int userId, int categoryId) {
        Double result = expenseRepository.sumAmountByUserIdAndCategoryId(userId, categoryId);
        return result == null ? 0.0 : result;
    }

    // Used by the Monthly Summary Dashboard.
    @Override
    public double getTotalByMonth(int userId, int month, int year) {
        Double result = expenseRepository.sumAmountByUserIdAndMonth(userId, month, year);
        return result == null ? 0.0 : result;
    }

    private void syncBudgetSpentAmount(int userId, int categoryId) {
        try {
            Map<String, Object> budget = budgetClient.getActiveBudgetByCategory(
                    userId,
                    categoryId,
                    buildAuthorizationHeader(userId)
            );
            Object budgetIdValue = budget != null ? budget.get("budgetId") : null;
            if (!(budgetIdValue instanceof Number budgetIdNumber)) {
                return;
            }

            double spentAmount = getTotalByCategory(userId, categoryId);
            budgetClient.updateSpentAmount(
                    budgetIdNumber.intValue(),
                    Map.of("spentAmount", spentAmount),
                    buildAuthorizationHeader(userId)
            );
        } catch (Exception e) {
            System.err.println("Budget sync skipped for userId=" + userId
                    + ", categoryId=" + categoryId + ": " + e.getMessage());
        }
    }

    private String buildAuthorizationHeader(int userId) {
        return "Bearer " + generateServiceToken(userId);
    }

    private String generateServiceToken(int userId) {
        return Jwts.builder()
                .subject("expense-service")
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
