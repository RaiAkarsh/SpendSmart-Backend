package com.spendsmart.recurring;

import com.spendsmart.recurring.client.ExpenseClient;
import com.spendsmart.recurring.client.IncomeClient;
import com.spendsmart.recurring.entity.RecurringTransaction;
import com.spendsmart.recurring.repository.RecurringRepository;
import com.spendsmart.recurring.serviceimpl.RecurringServiceImpl;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecurringServiceImpl Unit Tests")
class RecurringServiceImplTest {

    @Mock
    private RecurringRepository recurringRepository;

    @Mock
    private ExpenseClient expenseClient;

    @Mock
    private IncomeClient incomeClient;

    @InjectMocks
    private RecurringServiceImpl recurringService;

    private RecurringTransaction netflixRule;
    private RecurringTransaction salaryRule;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recurringService, "expenseServiceUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(recurringService, "incomeServiceUrl",  "http://localhost:8083");
        ReflectionTestUtils.setField(recurringService, "secret", "SpendSmartSecretKey2026_Standard32Bytes!");

        netflixRule = new RecurringTransaction();
        netflixRule.setRecurringId(1);
        netflixRule.setUserId(1);
        netflixRule.setCategoryId(6);
        netflixRule.setTitle("Netflix Subscription");
        netflixRule.setAmount(649.0);
        netflixRule.setType("EXPENSE");
        netflixRule.setFrequency("MONTHLY");
        netflixRule.setStartDate(LocalDate.of(2026, 5, 1));
        netflixRule.setNextDueDate(LocalDate.of(2026, 5, 1));
        netflixRule.setActive(true);

        salaryRule = new RecurringTransaction();
        salaryRule.setRecurringId(2);
        salaryRule.setUserId(1);
        salaryRule.setCategoryId(9);
        salaryRule.setTitle("Monthly Salary");
        salaryRule.setAmount(75000.0);
        salaryRule.setType("INCOME");
        salaryRule.setFrequency("MONTHLY");
        salaryRule.setSource("SALARY");
        salaryRule.setStartDate(LocalDate.of(2026, 5, 1));
        salaryRule.setNextDueDate(LocalDate.of(2026, 5, 1));
        salaryRule.setActive(true);
    }


    @Test
    @DisplayName("addRecurring: should set nextDueDate = startDate on creation")
    void addRecurring_shouldSetNextDueDateToStartDate() {
        when(recurringRepository.save(any())).thenReturn(netflixRule);

        RecurringTransaction input = new RecurringTransaction();
        input.setUserId(1);
        input.setType("expense");        // lowercase
        input.setFrequency("monthly");   // lowercase
        input.setStartDate(LocalDate.of(2026, 5, 1));

        recurringService.addRecurring(input);

        verify(recurringRepository).save(argThat(r ->
                r.getNextDueDate().equals(LocalDate.of(2026, 5, 1)) &&
                "EXPENSE".equals(r.getType()) &&
                "MONTHLY".equals(r.getFrequency()) &&
                r.isActive()));
    }

    @Test
    @DisplayName("addRecurring: should normalize source to UPPERCASE for INCOME type")
    void addRecurring_shouldNormalizeSource_forIncomeType() {
        when(recurringRepository.save(any())).thenReturn(salaryRule);
        when(incomeClient.createIncome(anyMap(), anyString())).thenReturn(Map.of("incomeId", 77));

        RecurringTransaction input = new RecurringTransaction();
        input.setType("INCOME");
        input.setFrequency("MONTHLY");
        input.setSource("salary");    // lowercase
        input.setStartDate(LocalDate.now());

        recurringService.addRecurring(input);

        verify(recurringRepository, atLeastOnce()).save(argThat(r -> "SALARY".equals(r.getSource())));
    }


    @Test
    @DisplayName("nextDueDate: DAILY frequency should advance by 1 day")
    void nextDueDate_shouldAdvanceByOneDay_forDailyFrequency() {
        LocalDate today = LocalDate.now();
        RecurringTransaction dailyRule = new RecurringTransaction();
        dailyRule.setRecurringId(3);
        dailyRule.setUserId(1);
        dailyRule.setCategoryId(1);
        dailyRule.setTitle("Daily Coffee");
        dailyRule.setAmount(80.0);
        dailyRule.setType("EXPENSE");
        dailyRule.setFrequency("DAILY");
        dailyRule.setNextDueDate(today);
        dailyRule.setActive(true);
        dailyRule.setPaymentMethod("CASH");

        when(recurringRepository.findByIsActiveAndNextDueDateLessThanEqual(true, today))
                .thenReturn(Arrays.asList(dailyRule));
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doReturn(Map.of("expenseId", 31)).when(expenseClient).createExpense(anyMap(), anyString());

        recurringService.processDueTransactions();

        // nextDueDate should advance by 1 day
        verify(recurringRepository).save(argThat(r ->
                r.getNextDueDate().equals(today.plusDays(1))));
    }

    @Test
    @DisplayName("nextDueDate: MONTHLY frequency should advance by 1 month")
    void nextDueDate_shouldAdvanceByOneMonth_forMonthlyFrequency() {
        LocalDate today = LocalDate.now();
        netflixRule.setNextDueDate(today);

        when(recurringRepository.findByIsActiveAndNextDueDateLessThanEqual(true, today))
                .thenReturn(Arrays.asList(netflixRule));
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doReturn(Map.of("expenseId", 41)).when(expenseClient).createExpense(anyMap(), anyString());

        recurringService.processDueTransactions();

        verify(recurringRepository).save(argThat(r ->
                r.getNextDueDate().equals(today.plusMonths(1))));
    }

    @Test
    @DisplayName("processDueTransactions: should deactivate rule when past endDate")
    void processDueTransactions_shouldDeactivate_whenPastEndDate() {
        LocalDate today = LocalDate.now();
        netflixRule.setNextDueDate(today);
        netflixRule.setEndDate(today);  // endDate = today, next would be past it

        when(recurringRepository.findByIsActiveAndNextDueDateLessThanEqual(true, today))
                .thenReturn(Arrays.asList(netflixRule));
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doReturn(Map.of("expenseId", 51)).when(expenseClient).createExpense(anyMap(), anyString());

        recurringService.processDueTransactions();

        verify(recurringRepository).save(argThat(r -> !r.isActive()));
    }


    @Test
    @DisplayName("deactivateRecurring: should set isActive to false")
    void deactivateRecurring_shouldSetInactive() {
        when(recurringRepository.findByRecurringId(1)).thenReturn(Optional.of(netflixRule));
        when(recurringRepository.save(any())).thenReturn(netflixRule);

        recurringService.deactivateRecurring(1);

        verify(recurringRepository).save(argThat(r -> !r.isActive()));
    }


    @Test
    @DisplayName("activateRecurring: should set isActive to true")
    void activateRecurring_shouldSetActive() {
        netflixRule.setActive(false);
        when(recurringRepository.findByRecurringId(1)).thenReturn(Optional.of(netflixRule));
        when(recurringRepository.save(any())).thenReturn(netflixRule);

        recurringService.activateRecurring(1);

        verify(recurringRepository).save(argThat(RecurringTransaction::isActive));
    }


    @Test
    @DisplayName("deleteRecurring: should delete when found")
    void deleteRecurring_shouldDelete_whenFound() {
        when(recurringRepository.findByRecurringId(1)).thenReturn(Optional.of(netflixRule));
        assertDoesNotThrow(() -> recurringService.deleteRecurring(1));
        verify(recurringRepository).deleteByRecurringId(1);
    }

    @Test
    @DisplayName("deleteRecurring: should throw when not found")
    void deleteRecurring_shouldThrow_whenNotFound() {
        when(recurringRepository.findByRecurringId(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> recurringService.deleteRecurring(99));
        verify(recurringRepository, never()).deleteByRecurringId(anyInt());
    }


    @Test
    @DisplayName("countActiveRules: should return correct count")
    void countActiveRules_shouldReturnCount() {
        when(recurringRepository.countByUserIdAndIsActive(1, true)).thenReturn(3);
        assertEquals(3, recurringService.countActiveRules(1));
    }

    @Test
    @DisplayName("updateRecurring: should move generated transaction from expense to income when type changes")
    void updateRecurring_shouldMoveGeneratedTransaction_whenTypeChanges() {
        netflixRule.setLastGeneratedExpenseId(88);
        netflixRule.setLastGeneratedDate(LocalDate.of(2026, 5, 1));

        RecurringTransaction updated = new RecurringTransaction();
        updated.setUserId(1);
        updated.setCategoryId(9);
        updated.setTitle("Monthly Salary");
        updated.setAmount(75000.0);
        updated.setCurrency("INR");
        updated.setType("INCOME");
        updated.setFrequency("MONTHLY");
        updated.setStartDate(LocalDate.of(2026, 5, 1));
        updated.setNextDueDate(LocalDate.of(2026, 6, 1));
        updated.setSource("SALARY");
        updated.setActive(true);

        when(recurringRepository.findByRecurringId(1)).thenReturn(Optional.of(netflixRule));
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(incomeClient.createIncome(anyMap(), anyString())).thenReturn(Map.of("incomeId", 91));

        recurringService.updateRecurring(1, updated);

        verify(expenseClient).deleteExpense(eq(88), anyString());
        verify(incomeClient).createIncome(anyMap(), anyString());
        verify(recurringRepository, atLeastOnce()).save(argThat(rule ->
                "INCOME".equals(rule.getType())
                        && Integer.valueOf(91).equals(rule.getLastGeneratedIncomeId())
                        && rule.getLastGeneratedExpenseId() == null));
    }
}
