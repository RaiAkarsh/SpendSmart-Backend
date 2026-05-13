package com.spendsmart.expense;

import com.spendsmart.expense.client.BudgetClient;
import com.spendsmart.expense.entity.Expense;
import com.spendsmart.expense.repository.ExpenseRepository;
import com.spendsmart.expense.serviceimpl.ExpenseServiceImpl;
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
@DisplayName("ExpenseServiceImpl Unit Tests")
class ExpenseServiceImplTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private BudgetClient budgetClient;

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    private Expense testExpense;

    @BeforeEach
    void setUp() {
        // Inject @Value fields that Spring normally reads from application.properties
        ReflectionTestUtils.setField(expenseService, "budgetServiceUrl", "http://localhost:8085");
        ReflectionTestUtils.setField(expenseService, "secret", "SpendSmartSecretKey2026_Standard32Bytes!");

        testExpense = new Expense();
        testExpense.setExpenseId(1);
        testExpense.setUserId(1);
        testExpense.setCategoryId(1);
        testExpense.setTitle("Lunch at office");
        testExpense.setAmount(250.0);
        testExpense.setCurrency("INR");
        testExpense.setPaymentMethod("UPI");
        testExpense.setDate(LocalDate.of(2026, 4, 20));
    }


    @Test
    @DisplayName("addExpense: should save expense and return with id")
    void addExpense_shouldSaveAndReturn() {
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

        Expense result = expenseService.addExpense(testExpense);

        assertNotNull(result);
        assertEquals("Lunch at office", result.getTitle());
        assertEquals(250.0, result.getAmount());
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }


    @Test
    @DisplayName("getExpenseById: should return expense when found")
    void getExpenseById_shouldReturn_whenFound() {
        when(expenseRepository.findByExpenseId(1)).thenReturn(Optional.of(testExpense));
        Expense result = expenseService.getExpenseById(1);
        assertEquals(1, result.getExpenseId());
    }

    @Test
    @DisplayName("getExpenseById: should throw when not found")
    void getExpenseById_shouldThrow_whenNotFound() {
        when(expenseRepository.findByExpenseId(999)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> expenseService.getExpenseById(999));
    }


    @Test
    @DisplayName("getExpensesByUser: should return all expenses ordered by date desc")
    void getExpensesByUser_shouldReturnList() {
        when(expenseRepository.findByUserIdOrderByDateDesc(1))
                .thenReturn(Arrays.asList(testExpense));
        List<Expense> result = expenseService.getExpensesByUser(1);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getExpensesByCategory: should return category expenses")
    void getExpensesByCategory_shouldReturnMatches() {
        when(expenseRepository.findByUserIdAndCategoryId(1, 1))
                .thenReturn(Arrays.asList(testExpense));

        List<Expense> result = expenseService.getExpensesByCategory(1, 1);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getExpensesByMonth: should return month expenses")
    void getExpensesByMonth_shouldReturnMatches() {
        when(expenseRepository.findByUserIdAndMonth(1, 4, 2026))
                .thenReturn(Arrays.asList(testExpense));

        List<Expense> result = expenseService.getExpensesByMonth(1, 4, 2026);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getExpensesByPaymentMethod: should normalize payment method")
    void getExpensesByPaymentMethod_shouldUppercaseMethod() {
        when(expenseRepository.findByUserIdAndPaymentMethod(1, "UPI"))
                .thenReturn(Arrays.asList(testExpense));

        List<Expense> result = expenseService.getExpensesByPaymentMethod(1, "upi");

        assertEquals(1, result.size());
        verify(expenseRepository).findByUserIdAndPaymentMethod(1, "UPI");
    }


    @Test
    @DisplayName("getExpensesByDateRange: should throw when startDate is after endDate")
    void getExpensesByDateRange_shouldThrow_whenStartAfterEnd() {
        LocalDate start = LocalDate.of(2026, 4, 30);
        LocalDate end   = LocalDate.of(2026, 4, 1);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> expenseService.getExpensesByDateRange(1, start, end));
        assertTrue(ex.getMessage().contains("startDate cannot be after endDate"));
    }

    @Test
    @DisplayName("getExpensesByDateRange: should return expenses for valid range")
    void getExpensesByDateRange_shouldReturn_whenValidRange() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end   = LocalDate.of(2026, 4, 30);
        when(expenseRepository.findByUserIdAndDateBetween(1, start, end))
                .thenReturn(Arrays.asList(testExpense));

        List<Expense> result = expenseService.getExpensesByDateRange(1, start, end);
        assertEquals(1, result.size());
    }


    @Test
    @DisplayName("getTotalByUser: should return 0.0 when user has no expenses (null SUM)")
    void getTotalByUser_shouldReturnZero_whenNoExpenses() {
        when(expenseRepository.sumAmountByUserId(1)).thenReturn(null);
        double total = expenseService.getTotalByUser(1);
        assertEquals(0.0, total);
    }

    @Test
    @DisplayName("getTotalByUser: should return correct sum")
    void getTotalByUser_shouldReturnSum() {
        when(expenseRepository.sumAmountByUserId(1)).thenReturn(1710.0);
        double total = expenseService.getTotalByUser(1);
        assertEquals(1710.0, total);
    }

    @Test
    @DisplayName("getTotalByMonth: should return 0.0 when null from repo")
    void getTotalByMonth_shouldReturnZero_whenNull() {
        when(expenseRepository.sumAmountByUserIdAndMonth(1, 4, 2026)).thenReturn(null);
        assertEquals(0.0, expenseService.getTotalByMonth(1, 4, 2026));
    }

    @Test
    @DisplayName("getTotalByCategory: should return 0 when null")
    void getTotalByCategory_shouldReturnZero_whenNull() {
        when(expenseRepository.sumAmountByUserIdAndCategoryId(1, 1)).thenReturn(null);
        assertEquals(0.0, expenseService.getTotalByCategory(1, 1));
    }

    @Test
    @DisplayName("getTotalByCategory: should return category total")
    void getTotalByCategory_shouldReturnTotal() {
        when(expenseRepository.sumAmountByUserIdAndCategoryId(1, 1)).thenReturn(250.0);
        assertEquals(250.0, expenseService.getTotalByCategory(1, 1));
    }

    @Test
    @DisplayName("getRecurringExpenses: should return recurring expenses")
    void getRecurringExpenses_shouldReturnMatches() {
        testExpense.setRecurring(true);
        when(expenseRepository.findByUserIdAndIsRecurring(1, true))
                .thenReturn(Arrays.asList(testExpense));

        List<Expense> result = expenseService.getRecurringExpenses(1);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isRecurring());
    }


    @Test
    @DisplayName("updateExpense: should update fields and refresh updatedAt")
    void updateExpense_shouldUpdateAllFields() {
        when(expenseRepository.findByExpenseId(1)).thenReturn(Optional.of(testExpense));
        when(expenseRepository.save(any())).thenReturn(testExpense);
        when(budgetClient.getActiveBudgetByCategory(anyInt(), anyInt(), anyString()))
                .thenReturn(Map.of("budgetId", 10));
        when(expenseRepository.sumAmountByUserIdAndCategoryId(anyInt(), anyInt())).thenReturn(300.0);

        Expense updates = new Expense();
        updates.setCategoryId(2);
        updates.setTitle("Updated lunch");
        updates.setAmount(300.0);
        updates.setCurrency("INR");
        updates.setDate(LocalDate.of(2026, 4, 20));
        updates.setPaymentMethod("CARD");

        Expense result = expenseService.updateExpense(1, updates);

        verify(expenseRepository, times(1)).save(argThat(e ->
                e.getTitle().equals("Updated lunch") && e.getAmount() == 300.0));
    }

    @Test
    @DisplayName("deleteExpense: should delete non-default expense")
    void deleteExpense_shouldDelete_whenNonDefault() {
        when(expenseRepository.findByExpenseId(1)).thenReturn(Optional.of(testExpense));

        expenseService.deleteExpense(1);

        verify(expenseRepository).deleteByExpenseId(1);
    }

    @Test
    @DisplayName("deleteExpense: should throw for default expense")
    void deleteExpense_shouldThrow_whenDefaultExpense() {
        testExpense.setDefault(true);
        when(expenseRepository.findByExpenseId(1)).thenReturn(Optional.of(testExpense));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> expenseService.deleteExpense(1));

        assertTrue(ex.getMessage().contains("Default expenses cannot be deleted"));
        verify(expenseRepository, never()).deleteByExpenseId(anyInt());
    }


    @Test
    @DisplayName("searchExpenses: should return all when keyword is empty")
    void searchExpenses_shouldReturnAll_whenKeywordIsEmpty() {
        when(expenseRepository.findByUserIdOrderByDateDesc(1))
                .thenReturn(Arrays.asList(testExpense));

        List<Expense> result = expenseService.searchExpenses(1, "");
        assertEquals(1, result.size());
        verify(expenseRepository, never()).searchByKeyword(anyInt(), anyString());
    }

    @Test
    @DisplayName("searchExpenses: should search by keyword when non-empty")
    void searchExpenses_shouldSearch_whenKeywordProvided() {
        when(expenseRepository.searchByKeyword(1, "lunch"))
                .thenReturn(Arrays.asList(testExpense));

        List<Expense> result = expenseService.searchExpenses(1, "lunch");
        assertEquals(1, result.size());
    }
}
