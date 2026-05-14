package com.spendsmart.budget;

import com.spendsmart.budget.entity.Budget;
import com.spendsmart.budget.entity.BudgetProgress;
import com.spendsmart.budget.repository.BudgetRepository;
import com.spendsmart.budget.serviceimpl.BudgetAlertPublisher;
import com.spendsmart.budget.serviceimpl.BudgetServiceImpl;
import com.spendsmart.budget.client.ExpenseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetServiceImpl Unit Tests")
class BudgetServiceImplTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetAlertPublisher budgetAlertPublisher;

    @Mock
    private ExpenseClient expenseClient;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private Budget foodBudget;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(budgetService, "secret", "SpendSmartSecretKey2026_Standard32Bytes!");

        foodBudget = new Budget();
        foodBudget.setBudgetId(1);
        foodBudget.setUserId(1);
        foodBudget.setCategoryId(1);
        foodBudget.setName("April Food Budget");
        foodBudget.setLimitAmount(5000.0);
        foodBudget.setSpentAmount(0.0);
        foodBudget.setCurrency("INR");
        foodBudget.setPeriod("MONTHLY");
        foodBudget.setStartDate(LocalDate.of(2026, 4, 1));
        foodBudget.setEndDate(LocalDate.of(2026, 4, 30));
        foodBudget.setAlertThreshold(80);
        foodBudget.setActive(true);

        lenient().when(expenseClient.getTotalByCategory(anyInt(), anyInt(), anyString()))
                .thenReturn(Map.of("totalExpenses", 0.0));
    }


    @Test
    @DisplayName("createBudget: should start with spentAmount=0 and isActive=true")
    void createBudget_shouldStartFresh() {
        when(budgetRepository.save(any(Budget.class))).thenReturn(foodBudget);

        Budget input = new Budget();
        input.setUserId(1);
        input.setName("April Food Budget");
        input.setLimitAmount(5000.0);
        input.setPeriod("monthly");   // lowercase  -  should be normalized

        budgetService.createBudget(input);

        verify(budgetRepository).save(argThat(b ->
                b.getSpentAmount() == 0.0 &&
                b.isActive() &&
                "MONTHLY".equals(b.getPeriod())));
    }


    @Test
    @DisplayName("getBudgetProgress: should return ON_TRACK when below threshold")
    void getBudgetProgress_shouldReturnOnTrack_whenBelowThreshold() {
        foodBudget.setSpentAmount(3000.0);  // 60% of 5000  -  below 80% threshold
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));

        BudgetProgress progress = budgetService.getBudgetProgress(1);

        assertEquals("ON_TRACK", progress.getStatus());
        assertFalse(progress.isAlertTriggered());
        assertFalse(progress.isExceeded());
        assertEquals(2000.0, progress.getRemaining(), 0.01);
    }

    @Test
    @DisplayName("getBudgetProgress: should return WARNING when at/above alert threshold")
    void getBudgetProgress_shouldReturnWarning_whenAtAlertThreshold() {
        foodBudget.setSpentAmount(4130.0);  // 82.6%  -  above 80% threshold
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));

        BudgetProgress progress = budgetService.getBudgetProgress(1);

        assertEquals("WARNING", progress.getStatus());
        assertTrue(progress.isAlertTriggered());
        assertFalse(progress.isExceeded());
    }

    @Test
    @DisplayName("getBudgetProgress: should return EXCEEDED when spent > limit")
    void getBudgetProgress_shouldReturnExceeded_whenOverLimit() {
        foodBudget.setSpentAmount(5130.0);  // 102.6%  -  over 5000 limit
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));

        BudgetProgress progress = budgetService.getBudgetProgress(1);

        assertEquals("EXCEEDED", progress.getStatus());
        assertTrue(progress.isExceeded());
        assertTrue(progress.isAlertTriggered());
        assertTrue(progress.getRemaining() < 0);
    }

    @Test
    @DisplayName("getBudgetProgress: percentageUsed should be 0 when limitAmount is 0")
    void getBudgetProgress_shouldReturnZeroPct_whenLimitIsZero() {
        foodBudget.setLimitAmount(0.0);
        foodBudget.setSpentAmount(0.0);
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));

        BudgetProgress progress = budgetService.getBudgetProgress(1);
        assertEquals(0.0, progress.getPercentageUsed(), 0.01);
    }


    @Test
    @DisplayName("addToSpentAmount: should increment spentAmount correctly")
    void addToSpentAmount_shouldIncrement() {
        foodBudget.setSpentAmount(2000.0);
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));
        when(budgetRepository.save(any())).thenReturn(foodBudget);

        budgetService.addToSpentAmount(1, 500.0);

        verify(budgetRepository).save(argThat(b -> b.getSpentAmount() == 2500.0));
    }


    @Test
    @DisplayName("subtractFromSpentAmount: should decrement spentAmount")
    void subtractFromSpentAmount_shouldDecrement() {
        foodBudget.setSpentAmount(2000.0);
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));
        when(budgetRepository.save(any())).thenReturn(foodBudget);

        budgetService.subtractFromSpentAmount(1, 500.0);

        verify(budgetRepository).save(argThat(b -> b.getSpentAmount() == 1500.0));
    }

    @Test
    @DisplayName("subtractFromSpentAmount: should never go below 0")
    void subtractFromSpentAmount_shouldNeverGoBelowZero() {
        foodBudget.setSpentAmount(100.0);
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));
        when(budgetRepository.save(any())).thenReturn(foodBudget);

        // Subtracting more than current spent
        budgetService.subtractFromSpentAmount(1, 500.0);

        verify(budgetRepository).save(argThat(b -> b.getSpentAmount() == 0.0));
    }


    @Test
    @DisplayName("resetBudgetPeriod: should set spentAmount to 0")
    void resetBudgetPeriod_shouldSetSpentToZero() {
        foodBudget.setSpentAmount(4200.0);
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));
        when(budgetRepository.save(any())).thenReturn(foodBudget);

        budgetService.resetBudgetPeriod(1);

        verify(budgetRepository).save(argThat(b -> b.getSpentAmount() == 0.0));
    }


    @Test
    @DisplayName("deactivateBudget: should set isActive to false")
    void deactivateBudget_shouldSetInactive() {
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));
        when(budgetRepository.save(any())).thenReturn(foodBudget);

        budgetService.deactivateBudget(1);

        verify(budgetRepository).save(argThat(b -> !b.isActive()));
    }


    @Test
    @DisplayName("deleteBudget: should delete when found")
    void deleteBudget_shouldDelete_whenFound() {
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));

        assertDoesNotThrow(() -> budgetService.deleteBudget(1));
        verify(budgetRepository).deleteByBudgetId(1);
    }

    @Test
    @DisplayName("deleteBudget: should throw when not found")
    void deleteBudget_shouldThrow_whenNotFound() {
        when(budgetRepository.findByBudgetId(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> budgetService.deleteBudget(99));
        verify(budgetRepository, never()).deleteByBudgetId(anyInt());
    }


    @Test
    @DisplayName("getAllBudgetProgress: should return BudgetProgress list for all active budgets")
    void getAllBudgetProgress_shouldReturnProgressList() {
        when(budgetRepository.findByUserIdAndIsActive(1, true))
                .thenReturn(Arrays.asList(foodBudget));

        List<BudgetProgress> result = budgetService.getAllBudgetProgress(1);

        assertEquals(1, result.size());
        assertEquals("April Food Budget", result.get(0).getName());
    }

    @Test
    @DisplayName("getAllBudgetProgress: should return empty list when no active budgets")
    void getAllBudgetProgress_shouldReturnEmpty_whenNoActiveBudgets() {
        when(budgetRepository.findByUserIdAndIsActive(1, true))
                .thenReturn(Collections.emptyList());

        List<BudgetProgress> result = budgetService.getAllBudgetProgress(1);
        assertTrue(result.isEmpty());
    }


    @Test
    @DisplayName("getBudgetById: should return budget when found")
    void getBudgetById_shouldReturn_whenFound() {
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));
        Budget b = budgetService.getBudgetById(1);
        assertEquals(1, b.getBudgetId());
    }

    @Test
    @DisplayName("getBudgetById: should throw when not found")
    void getBudgetById_shouldThrow_whenNotFound() {
        when(budgetRepository.findByBudgetId(999)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> budgetService.getBudgetById(999));
    }

    @Test
    @DisplayName("getAllBudgets: should delegate to repository.findAll")
    void getAllBudgets_shouldReturnList() {
        when(budgetRepository.findAll()).thenReturn(Arrays.asList(foodBudget));
        assertEquals(1, budgetService.getAllBudgets().size());
    }

    @Test
    @DisplayName("getBudgetsByUser: should delegate to repository.findByUserId")
    void getBudgetsByUser_shouldReturnList() {
        when(budgetRepository.findByUserId(1)).thenReturn(Arrays.asList(foodBudget));
        assertEquals(1, budgetService.getBudgetsByUser(1).size());
    }

    @Test
    @DisplayName("getActiveBudgets: should delegate to repository.findByUserIdAndIsActive")
    void getActiveBudgets_shouldReturnList() {
        when(budgetRepository.findByUserIdAndIsActive(1, true)).thenReturn(Arrays.asList(foodBudget));
        assertEquals(1, budgetService.getActiveBudgets(1).size());
    }

    @Test
    @DisplayName("getActiveBudgetByCategory: should return when found")
    void getActiveBudgetByCategory_shouldReturn_whenFound() {
        when(budgetRepository.findByUserIdAndCategoryIdAndIsActive(1, 1, true))
                .thenReturn(Optional.of(foodBudget));
        Budget b = budgetService.getActiveBudgetByCategory(1, 1);
        assertEquals(1, b.getCategoryId());
    }

    @Test
    @DisplayName("getActiveBudgetByCategory: should throw when not found")
    void getActiveBudgetByCategory_shouldThrow_whenNotFound() {
        when(budgetRepository.findByUserIdAndCategoryIdAndIsActive(1, 99, true))
                .thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> budgetService.getActiveBudgetByCategory(1, 99));
    }


    @Test
    @DisplayName("updateBudget: should update editable fields and currency")
    void updateBudget_shouldUpdateEditableFieldsAndCurrency() {
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));
        when(budgetRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Budget updates = new Budget();
        updates.setName("Updated Name");
        updates.setLimitAmount(8000.0);
        updates.setAlertThreshold(75);
        updates.setEndDate(LocalDate.of(2026, 5, 31));
        updates.setCurrency("USD");

        Budget saved = budgetService.updateBudget(1, updates);

        assertEquals("Updated Name", saved.getName());
        assertEquals(8000.0, saved.getLimitAmount());
        assertEquals(75, saved.getAlertThreshold());
        assertEquals("USD", saved.getCurrency());
        verify(budgetAlertPublisher).publishIfNeeded(saved);
    }

    @Test
    @DisplayName("updateBudget: should keep existing currency when not provided")
    void updateBudget_shouldKeepCurrency_whenNull() {
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));
        when(budgetRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Budget updates = new Budget();
        updates.setName("Renamed");
        updates.setLimitAmount(5500.0);
        updates.setAlertThreshold(80);
        // currency intentionally null

        Budget saved = budgetService.updateBudget(1, updates);
        assertEquals("INR", saved.getCurrency());
    }


    @Test
    @DisplayName("updateSpentAmount: should clamp negative values to 0")
    void updateSpentAmount_shouldClampNegativeToZero() {
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));
        when(budgetRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        budgetService.updateSpentAmount(1, -50.0);

        verify(budgetRepository).save(argThat(b -> b.getSpentAmount() == 0.0));
    }

    @Test
    @DisplayName("updateSpentAmount: should set exact non-negative value")
    void updateSpentAmount_shouldSetValue() {
        when(budgetRepository.findByBudgetId(1)).thenReturn(Optional.of(foodBudget));
        when(budgetRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        budgetService.updateSpentAmount(1, 1234.5);

        verify(budgetRepository).save(argThat(b -> b.getSpentAmount() == 1234.5));
        verify(budgetAlertPublisher, atLeastOnce()).publishIfNeeded(any());
    }


    @Test
    @DisplayName("createBudget: should prefill spentAmount from expense-service")
    void createBudget_shouldPrefillSpent_fromExpenseService() {
        // Override default 0.0 stub to simulate existing expenses for this category
        when(expenseClient.getTotalByCategory(anyInt(), anyInt(), anyString()))
                .thenReturn(Map.of("totalExpenses", 1200.0));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));

        Budget input = new Budget();
        input.setUserId(1);
        input.setCategoryId(1);
        input.setName("Food");
        input.setLimitAmount(5000.0);
        input.setPeriod("MONTHLY");

        Budget saved = budgetService.createBudget(input);

        assertEquals(1200.0, saved.getSpentAmount());
    }

    @Test
    @DisplayName("createBudget: should treat null expense response as 0 spent")
    void createBudget_shouldHandleNullExpenseResponse() {
        when(expenseClient.getTotalByCategory(anyInt(), anyInt(), anyString())).thenReturn(null);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));

        Budget input = new Budget();
        input.setUserId(1);
        input.setCategoryId(1);
        input.setName("Food");
        input.setLimitAmount(5000.0);
        input.setPeriod("MONTHLY");

        Budget saved = budgetService.createBudget(input);
        assertEquals(0.0, saved.getSpentAmount());
    }

    @Test
    @DisplayName("createBudget: should default to 0 spent when expense client throws")
    void createBudget_shouldDefaultZero_whenExpenseClientFails() {
        when(expenseClient.getTotalByCategory(anyInt(), anyInt(), anyString()))
                .thenThrow(new RuntimeException("expense svc down"));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));

        Budget input = new Budget();
        input.setUserId(1);
        input.setCategoryId(1);
        input.setName("Food");
        input.setLimitAmount(5000.0);
        input.setPeriod("MONTHLY");

        Budget saved = budgetService.createBudget(input);
        assertEquals(0.0, saved.getSpentAmount());
    }

    @Test
    @DisplayName("createBudget: should handle null period without normalization")
    void createBudget_shouldHandleNullPeriod() {
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));

        Budget input = new Budget();
        input.setUserId(1);
        input.setCategoryId(1);
        input.setName("Food");
        input.setLimitAmount(5000.0);
        input.setPeriod(null);

        Budget saved = budgetService.createBudget(input);
        assertNull(saved.getPeriod());
        assertTrue(saved.isActive());
    }
}
