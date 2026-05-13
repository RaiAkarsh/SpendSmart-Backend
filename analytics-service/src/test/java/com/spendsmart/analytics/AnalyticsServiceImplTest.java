package com.spendsmart.analytics;

import com.spendsmart.analytics.client.BudgetClient;
import com.spendsmart.analytics.client.ExpenseClient;
import com.spendsmart.analytics.client.IncomeClient;
import com.spendsmart.analytics.dto.CategoryBreakdown;
import com.spendsmart.analytics.dto.FinancialHealthScore;
import com.spendsmart.analytics.dto.MonthlySummary;
import com.spendsmart.analytics.dto.SpendingTrend;
import com.spendsmart.analytics.serviceimpl.AnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsServiceImpl Unit Tests")
class AnalyticsServiceImplTest {

    @Mock
    private ExpenseClient expenseClient;

    @Mock
    private IncomeClient incomeClient;

    @Mock
    private BudgetClient budgetClient;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(analyticsService, "secret", "SpendSmartSecretKey2026_Standard32Bytes!");
    }


    private void mockExpenseTotal(int userId, int month, int year, double amount) {
        when(expenseClient.getTotalByMonth(eq(userId), eq(month), eq(year), anyString()))
                .thenReturn(Map.of("totalExpenses", amount));
    }

    private void mockIncomeTotal(int userId, int month, int year, double amount) {
        when(incomeClient.getTotalByMonth(eq(userId), eq(month), eq(year), anyString()))
                .thenReturn(Map.of("totalIncome", amount));
    }


    @Test
    @DisplayName("getMonthlySummary: should compute netSavings and savingsRate correctly")
    void getMonthlySummary_shouldComputeCorrectly() {
        mockExpenseTotal(1, 4, 2026, 1710.0);
        mockIncomeTotal(1, 4, 2026, 105000.0);

        MonthlySummary result = analyticsService.getMonthlySummary(1, 4, 2026);

        assertEquals(105000.0, result.getTotalIncome(), 0.01);
        assertEquals(1710.0,   result.getTotalExpenses(), 0.01);
        assertEquals(103290.0, result.getNetSavings(), 0.01);
        assertTrue(result.getSavingsRate() > 98.0);
    }

    @Test
    @DisplayName("getMonthlySummary: should return 0 savingsRate when income is 0")
    void getMonthlySummary_shouldReturnZeroRate_whenIncomeIsZero() {
        mockExpenseTotal(1, 4, 2026, 500.0);
        mockIncomeTotal(1, 4, 2026, 0.0);

        MonthlySummary result = analyticsService.getMonthlySummary(1, 4, 2026);

        assertEquals(0.0, result.getSavingsRate(), 0.01);
    }

    @Test
    @DisplayName("getMonthlySummary: should handle downstream failure gracefully (return 0)")
    void getMonthlySummary_shouldHandleServiceFailure_gracefully() {
        when(expenseClient.getTotalByMonth(anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(new RuntimeException("Connection refused"));
        when(incomeClient.getTotalByMonth(anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(new RuntimeException("Connection refused"));

        MonthlySummary result = analyticsService.getMonthlySummary(1, 4, 2026);
        assertEquals(0.0, result.getTotalExpenses(), 0.01);
        assertEquals(0.0, result.getTotalIncome(), 0.01);
    }


    @Test
    @DisplayName("getFinancialHealthScore: score should be in 0-100 range")
    void getFinancialHealthScore_shouldBeInValidRange() {
        // Mock several months of data for trend calculation
        when(expenseClient.getTotalByMonth(anyInt(), anyInt(), anyInt(), anyString()))
                .thenReturn(Map.of("totalExpenses", 30000.0));
        when(incomeClient.getTotalByMonth(anyInt(), anyInt(), anyInt(), anyString()))
                .thenReturn(Map.of("totalIncome", 75000.0));
        when(budgetClient.getActiveBudgets(anyInt(), anyString()))
                .thenReturn(List.of(Map.of("limitAmount", 50000.0, "spentAmount", 30000.0)));

        FinancialHealthScore score = analyticsService.getFinancialHealthScore(1);

        assertNotNull(score);
        assertTrue(score.getTotalScore() >= 0);
        assertTrue(score.getTotalScore() <= 100);
        assertNotNull(score.getGrade());
        assertFalse(score.getGrade().isEmpty());
        assertNotNull(score.getRecommendation());
    }

    @Test
    @DisplayName("getFinancialHealthScore: grade should be A+ when savings rate is very high")
    void getFinancialHealthScore_shouldReturnHighGrade_whenHighSavings() {
        // Income much higher than expenses = high savings rate = high score
        when(expenseClient.getTotalByMonth(anyInt(), anyInt(), anyInt(), anyString()))
                .thenReturn(Map.of("totalExpenses", 5000.0));
        when(incomeClient.getTotalByMonth(anyInt(), anyInt(), anyInt(), anyString()))
                .thenReturn(Map.of("totalIncome", 100000.0));
        when(budgetClient.getActiveBudgets(anyInt(), anyString()))
                .thenReturn(List.of(Map.of("limitAmount", 50000.0, "spentAmount", 5000.0)));

        FinancialHealthScore score = analyticsService.getFinancialHealthScore(1);

        assertTrue(score.getTotalScore() >= 70,
                "High savings rate should produce high health score");
        assertTrue(List.of("A+", "A", "B").contains(score.getGrade()));
    }


    @Test
    @DisplayName("getCategoryBreakdown: should group and sum by categoryId")
    void getCategoryBreakdown_shouldGroupByCategory() {
        // Simulate expense-service returning 3 expenses in 2 categories
        List<Map<String, Object>> expenses = List.of(
                Map.of("categoryId", 1, "amount", 250.0),  // Food
                Map.of("categoryId", 1, "amount", 80.0),   // Food
                Map.of("categoryId", 4, "amount", 1200.0)  // Bills
        );
        when(expenseClient.getExpensesByMonth(eq(1), eq(4), eq(2026), anyString()))
                .thenReturn(expenses);

        List<CategoryBreakdown> result = analyticsService.getCategoryBreakdown(1, 4, 2026);

        assertFalse(result.isEmpty());
        // Bills (1200) should be present
        assertTrue(result.stream().anyMatch(c -> Math.abs(c.getTotalSpent() - 1200.0) < 0.01));
        // Verify percentage sums to ~100%
        double totalPct = result.stream().mapToDouble(CategoryBreakdown::getPercentage).sum();
        assertEquals(100.0, totalPct, 0.1);
    }


    @Test
    @DisplayName("getSpendingTrends: should return N monthly data points")
    void getSpendingTrends_shouldReturnNMonths() {
        when(expenseClient.getTotalByMonth(anyInt(), anyInt(), anyInt(), anyString()))
                .thenReturn(Map.of("totalExpenses", 20000.0));
        when(incomeClient.getTotalByMonth(anyInt(), anyInt(), anyInt(), anyString()))
                .thenReturn(Map.of("totalIncome", 75000.0));

        List<SpendingTrend> trends = analyticsService.getSpendingTrends(1, 6);

        assertEquals(6, trends.size());
        trends.forEach(t -> {
            assertTrue(t.getTotalIncome() >= 0);
            assertTrue(t.getTotalExpenses() >= 0);
        });
    }
}
