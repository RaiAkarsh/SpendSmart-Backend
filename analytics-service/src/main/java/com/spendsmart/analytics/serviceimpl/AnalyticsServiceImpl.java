package com.spendsmart.analytics.serviceimpl;

import com.spendsmart.analytics.client.BudgetClient;
import com.spendsmart.analytics.client.ExpenseClient;
import com.spendsmart.analytics.client.IncomeClient;
import com.spendsmart.analytics.dto.CategoryBreakdown;
import com.spendsmart.analytics.dto.FinancialHealthScore;
import com.spendsmart.analytics.dto.MonthlySummary;
import com.spendsmart.analytics.dto.SpendingTrend;
import com.spendsmart.analytics.service.AnalyticsService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    @Autowired
    private ExpenseClient expenseClient;

    @Autowired
    private IncomeClient incomeClient;

    @Autowired
    private BudgetClient budgetClient;

    @Value("${jwt.secret}")
    private String secret;

    // Calls expense-service and income-service for monthly totals,
    // then calculates net savings and savings rate.
    // GET /analytics/user/{userId}/summary/month?month=4&year=2026
    @Override
    @Cacheable(cacheNames = "analytics", key = "'monthly:' + #userId + ':' + #month + ':' + #year")
    public MonthlySummary getMonthlySummary(int userId, int month, int year) {
        double totalExpenses = getMonthlyExpenseTotal(userId, month, year);
        double totalIncome = getMonthlyIncomeTotal(userId, month, year);
        return new MonthlySummary(userId, month, year, totalIncome, totalExpenses);
    }

    // Aggregates all 12 months of a year.
    // Returns: totalIncome, totalExpenses, netSavings, savingsRate,
    //          bestMonth, worstMonth
    @Override
    @Cacheable(cacheNames = "analytics", key = "'yearly:' + #userId + ':' + #year")
    public Map<String, Object> getYearlySummary(int userId, int year) {
        double yearIncome = 0;
        double yearExpenses = 0;
        double bestSavings = Double.MIN_VALUE;
        double worstSavings = Double.MAX_VALUE;
        int bestMonth = 1;
        int worstMonth = 1;

        for (int m = 1; m <= 12; m++) {
            double monthExp = getMonthlyExpenseTotal(userId, m, year);
            double monthInc = getMonthlyIncomeTotal(userId, m, year);
            yearIncome += monthInc;
            yearExpenses += monthExp;

            double monthSavings = monthInc - monthExp;
            if (monthSavings > bestSavings) {
                bestSavings = monthSavings;
                bestMonth = m;
            }
            if (monthSavings < worstSavings) {
                worstSavings = monthSavings;
                worstMonth = m;
            }
        }

        double netSavings = yearIncome - yearExpenses;
        double savingsRate = yearIncome > 0
                ? Math.round(((netSavings / yearIncome) * 100) * 100.0) / 100.0
                : 0.0;

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("year", year);
        result.put("totalIncome", yearIncome);
        result.put("totalExpenses", yearExpenses);
        result.put("netSavings", netSavings);
        result.put("savingsRate", savingsRate);
        result.put("bestMonth", bestMonth);
        result.put("worstMonth", worstMonth);
        return result;
    }

    // Gets all expenses for a user in a month, groups by categoryId,
    // and calculates percentage for each category.
    // Calls: GET /expenses/user/{userId}/month?month=X&year=Y
    // Returns: List of CategoryBreakdown DTOs (pie chart data)
    @Override
    @SuppressWarnings("unchecked")
    @Cacheable(cacheNames = "analytics", key = "'category:' + #userId + ':' + #month + ':' + #year")
    public List<CategoryBreakdown> getCategoryBreakdown(int userId, int month, int year) {
        List<CategoryBreakdown> breakdowns = new ArrayList<>();

        try {
            List<Map<String, Object>> expenses = expenseClient.getExpensesByMonth(
                    userId,
                    month,
                    year,
                    buildAuthorizationHeader(userId)
            );

            if (expenses == null || expenses.isEmpty()) {
                return breakdowns;
            }

            // Group by categoryId and sum amounts
            Map<Integer, Double> categoryTotals = new HashMap<>();
            double grandTotal = 0;

            for (Map<String, Object> expense : expenses) {
                int catId = ((Number) expense.get("categoryId")).intValue();
                double amount = ((Number) expense.get("amount")).doubleValue();
                categoryTotals.merge(catId, amount, Double::sum);
                grandTotal += amount;
            }

            // Convert to CategoryBreakdown DTOs
            for (Map.Entry<Integer, Double> entry : categoryTotals.entrySet()) {
                double percentage = grandTotal > 0
                        ? (entry.getValue() / grandTotal) * 100
                        : 0.0;
                breakdowns.add(new CategoryBreakdown(
                        entry.getKey(),
                        "Category #" + entry.getKey(), // category-service lookup in Phase 5
                        entry.getValue(),
                        percentage
                ));
            }

        } catch (Exception e) {
            System.err.println("Failed to get category breakdown: " + e.getMessage());
        }

        return breakdowns;
    }

    // Returns expense and income totals for the last N months.
    // Used for the line chart on the dashboard.
    @Override
    @Cacheable(cacheNames = "analytics", key = "'trends:' + #userId + ':' + #months")
    public List<SpendingTrend> getSpendingTrends(int userId, int months) {
        List<SpendingTrend> trends = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate target = now.minusMonths(i);
            int month = target.getMonthValue();
            int year = target.getYear();

            double totalExpenses = getMonthlyExpenseTotal(userId, month, year);
            double totalIncome = getMonthlyIncomeTotal(userId, month, year);

            trends.add(new SpendingTrend(month, year, totalExpenses, totalIncome));
        }

        return trends;
    }

    // Simple comparison for a specific month.
    // Returns: totalIncome, totalExpenses, difference, status
    //   status: "SURPLUS" if income > expenses, "DEFICIT" if not
    @Override
    @Cacheable(cacheNames = "analytics", key = "'income-vs-expense:' + #userId + ':' + #month + ':' + #year")
    public Map<String, Object> getIncomeVsExpense(int userId, int month, int year) {
        double totalExpenses = getMonthlyExpenseTotal(userId, month, year);
        double totalIncome = getMonthlyIncomeTotal(userId, month, year);
        double difference = totalIncome - totalExpenses;

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("month", month);
        result.put("year", year);
        result.put("totalIncome", totalIncome);
        result.put("totalExpenses", totalExpenses);
        result.put("difference", difference);
        result.put("status", difference >= 0 ? "SURPLUS" : "DEFICIT");
        return result;
    }

    // Calls the /total endpoints on expense-service and income-service.
    // Returns overall lifetime financial position.
    @Override
    @SuppressWarnings("unchecked")
    @Cacheable(cacheNames = "analytics", key = "'all-time:' + #userId")
    public Map<String, Object> getAllTimeTotals(int userId) {
        double totalExpenses = 0;
        double totalIncome = 0;

        try {
            Map<String, Object> expResponse = expenseClient.getTotalByUser(
                    userId,
                    buildAuthorizationHeader(userId)
            );
            if (expResponse != null && expResponse.get("totalExpenses") != null) {
                totalExpenses = ((Number) expResponse.get("totalExpenses")).doubleValue();
            }
        } catch (Exception e) {
            System.err.println("Failed to get expense total: " + e.getMessage());
        }

        try {
            Map<String, Object> incResponse = incomeClient.getTotalByUser(
                    userId,
                    buildAuthorizationHeader(userId)
            );
            if (incResponse != null && incResponse.get("totalIncome") != null) {
                totalIncome = ((Number) incResponse.get("totalIncome")).doubleValue();
            }
        } catch (Exception e) {
            System.err.println("Failed to get income total: " + e.getMessage());
        }

        double netPosition = totalIncome - totalExpenses;
        double savingsRate = totalIncome > 0
                ? Math.round(((netPosition / totalIncome) * 100) * 100.0) / 100.0
                : 0.0;

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("totalIncome", totalIncome);
        result.put("totalExpenses", totalExpenses);
        result.put("netPosition", netPosition);
        result.put("savingsRate", savingsRate);
        result.put("status", netPosition >= 0 ? "POSITIVE" : "NEGATIVE");
        return result;
    }

    // Calculates a composite financial health score (0-100).
    // Uses savings rate + budget adherence + consistency.
    @Override
    @SuppressWarnings("unchecked")
    @Cacheable(cacheNames = "analytics", key = "'health:' + #userId")
    public FinancialHealthScore getFinancialHealthScore(int userId) {
        // Get current month's savings rate
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        double totalIncome = getMonthlyIncomeTotal(userId, month, year);
        double totalExpenses = getMonthlyExpenseTotal(userId, month, year);
        double savingsRate = totalIncome > 0
                ? ((totalIncome - totalExpenses) / totalIncome) * 100
                : 0.0;

        // Get budget adherence from budget-service
        int onTrackBudgets = 0;
        int totalBudgets = 0;
        try {
            List<Map<String, Object>> budgets = budgetClient.getActiveBudgets(
                    userId,
                    buildAuthorizationHeader(userId)
            );
            if (budgets != null) {
                totalBudgets = budgets.size();
                for (Map<String, Object> budget : budgets) {
                    double limit = ((Number) budget.get("limitAmount")).doubleValue();
                    double spent = ((Number) budget.get("spentAmount")).doubleValue();
                    if (spent <= limit) {
                        onTrackBudgets++;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get budget data: " + e.getMessage());
        }

        // Consistency check: compare this month vs last month
        boolean isConsistent = true;
        try {
            LocalDate lastMonth = now.minusMonths(1);
            double lastMonthExp = getMonthlyExpenseTotal(userId,
                    lastMonth.getMonthValue(), lastMonth.getYear());
            // Spending is "consistent" if it doesn't vary more than 30%
            if (lastMonthExp > 0) {
                double variance = Math.abs(totalExpenses - lastMonthExp) / lastMonthExp;
                isConsistent = variance <= 0.3;
            }
        } catch (Exception e) {
            // Default to consistent if we can't check
        }

        return new FinancialHealthScore(userId, savingsRate,
                onTrackBudgets, totalBudgets, isConsistent);
    }


    // Calls: GET /expenses/user/{userId}/total/month?month=X&year=Y
    // Returns the totalExpenses value, or 0.0 if service is down
    @SuppressWarnings("unchecked")
    private double getMonthlyExpenseTotal(int userId, int month, int year) {
        try {
            Map<String, Object> response = expenseClient.getTotalByMonth(
                    userId,
                    month,
                    year,
                    buildAuthorizationHeader(userId)
            );
            if (response != null && response.get("totalExpenses") != null) {
                return ((Number) response.get("totalExpenses")).doubleValue();
            }
        } catch (Exception e) {
            System.err.println("Failed to get monthly expense total: " + e.getMessage());
        }
        return 0.0;
    }

    // Calls: GET /incomes/user/{userId}/total/month?month=X&year=Y
    // Returns the totalIncome value, or 0.0 if service is down
    @SuppressWarnings("unchecked")
    private double getMonthlyIncomeTotal(int userId, int month, int year) {
        try {
            Map<String, Object> response = incomeClient.getTotalByMonth(
                    userId,
                    month,
                    year,
                    buildAuthorizationHeader(userId)
            );
            if (response != null && response.get("totalIncome") != null) {
                return ((Number) response.get("totalIncome")).doubleValue();
            }
        } catch (Exception e) {
            System.err.println("Failed to get monthly income total: " + e.getMessage());
        }
        return 0.0;
    }

    private String buildAuthorizationHeader(int userId) {
        return "Bearer " + generateServiceToken(userId);
    }

    private String generateServiceToken(int userId) {
        return Jwts.builder()
                .subject("analytics-service")
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
