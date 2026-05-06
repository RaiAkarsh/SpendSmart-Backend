package com.spendsmart.budget;

import com.spendsmart.budget.entity.Budget;
import com.spendsmart.budget.entity.BudgetProgress;
import com.spendsmart.budget.repository.BudgetRepository;
import com.spendsmart.budget.serviceimpl.BudgetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetServiceImpl Unit Tests")
class BudgetServiceImplTest {

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private Budget foodBudget;

    @BeforeEach
    void setUp() {
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
}