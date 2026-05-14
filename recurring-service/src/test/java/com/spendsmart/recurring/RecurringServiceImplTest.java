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
    @DisplayName("getById: should return rule when found")
    void getById_shouldReturn_whenFound() {
        when(recurringRepository.findByRecurringId(1)).thenReturn(Optional.of(netflixRule));
        assertEquals(1, recurringService.getById(1).getRecurringId());
    }

    @Test
    @DisplayName("getById: should throw when not found")
    void getById_shouldThrow_whenNotFound() {
        when(recurringRepository.findByRecurringId(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> recurringService.getById(99));
    }

    @Test
    @DisplayName("getByUser: should delegate to repository")
    void getByUser_shouldDelegate() {
        when(recurringRepository.findByUserId(1)).thenReturn(Arrays.asList(netflixRule));
        assertEquals(1, recurringService.getByUser(1).size());
    }

    @Test
    @DisplayName("getActiveByUser: should delegate to repository")
    void getActiveByUser_shouldDelegate() {
        when(recurringRepository.findByUserIdAndIsActive(1, true)).thenReturn(Arrays.asList(netflixRule));
        assertEquals(1, recurringService.getActiveByUser(1).size());
    }

    @Test
    @DisplayName("getByUserAndType: should uppercase type and delegate")
    void getByUserAndType_shouldUppercaseAndDelegate() {
        when(recurringRepository.findByUserIdAndType(1, "EXPENSE"))
                .thenReturn(Arrays.asList(netflixRule));
        assertEquals(1, recurringService.getByUserAndType(1, "expense").size());
    }

    @Test
    @DisplayName("getUpcomingThisMonth: should query active rules due in current month")
    void getUpcomingThisMonth_shouldReturnList() {
        when(recurringRepository.findByUserIdAndIsActiveAndNextDueDateBetween(
                eq(1), eq(true), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(netflixRule));
        assertEquals(1, recurringService.getUpcomingThisMonth(1).size());
    }


    @Test
    @DisplayName("addRecurring: should generate immediate occurrence when due today (EXPENSE)")
    void addRecurring_shouldGenerateImmediate_whenDueTodayExpense() {
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(expenseClient.createExpense(anyMap(), anyString())).thenReturn(Map.of("expenseId", 11));

        RecurringTransaction input = new RecurringTransaction();
        input.setUserId(1);
        input.setType("EXPENSE");
        input.setFrequency("MONTHLY");
        input.setStartDate(LocalDate.now());
        input.setPaymentMethod("CASH");

        RecurringTransaction saved = recurringService.addRecurring(input);

        assertEquals(Integer.valueOf(11), saved.getLastGeneratedExpenseId());
        assertEquals(LocalDate.now(), saved.getLastGeneratedDate());
        verify(expenseClient).createExpense(anyMap(), anyString());
    }

    @Test
    @DisplayName("addRecurring: should swallow exception during immediate generation")
    void addRecurring_shouldSwallowException_duringImmediateGeneration() {
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(expenseClient.createExpense(anyMap(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        RecurringTransaction input = new RecurringTransaction();
        input.setUserId(1);
        input.setType("EXPENSE");
        input.setFrequency("MONTHLY");
        input.setStartDate(LocalDate.now());

        assertDoesNotThrow(() -> recurringService.addRecurring(input));
    }

    @Test
    @DisplayName("addRecurring: should NOT generate immediately when startDate is in future")
    void addRecurring_shouldNotGenerateImmediately_whenFutureStart() {
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RecurringTransaction input = new RecurringTransaction();
        input.setUserId(1);
        input.setType("EXPENSE");
        input.setFrequency("MONTHLY");
        input.setStartDate(LocalDate.now().plusMonths(1));

        recurringService.addRecurring(input);

        verify(expenseClient, never()).createExpense(anyMap(), anyString());
    }


    @Test
    @DisplayName("processDueTransactions: should create INCOME occurrence and advance next due date")
    void processDueTransactions_shouldProcessIncome() {
        LocalDate today = LocalDate.now();
        salaryRule.setNextDueDate(today);
        salaryRule.setEndDate(today.plusMonths(6));

        when(recurringRepository.findByIsActiveAndNextDueDateLessThanEqual(true, today))
                .thenReturn(Arrays.asList(salaryRule));
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(incomeClient.createIncome(anyMap(), anyString())).thenReturn(Map.of("incomeId", 33));

        recurringService.processDueTransactions();

        verify(incomeClient).createIncome(anyMap(), anyString());
        verify(recurringRepository).save(argThat(r ->
                Integer.valueOf(33).equals(r.getLastGeneratedIncomeId())
                        && r.getNextDueDate().equals(today.plusMonths(1))));
    }

    @Test
    @DisplayName("processDueTransactions: should swallow exception in single rule processing")
    void processDueTransactions_shouldSwallowException() {
        LocalDate today = LocalDate.now();
        netflixRule.setNextDueDate(today);

        when(recurringRepository.findByIsActiveAndNextDueDateLessThanEqual(true, today))
                .thenReturn(Arrays.asList(netflixRule));
        when(expenseClient.createExpense(anyMap(), anyString()))
                .thenThrow(new RuntimeException("rest failure"));

        assertDoesNotThrow(() -> recurringService.processDueTransactions());
    }

    @Test
    @DisplayName("processDueTransactions: WEEKLY frequency should advance by 1 week")
    void processDueTransactions_weeklyFrequency() {
        LocalDate today = LocalDate.now();
        netflixRule.setFrequency("WEEKLY");
        netflixRule.setNextDueDate(today);

        when(recurringRepository.findByIsActiveAndNextDueDateLessThanEqual(true, today))
                .thenReturn(Arrays.asList(netflixRule));
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doReturn(Map.of("expenseId", 61)).when(expenseClient).createExpense(anyMap(), anyString());

        recurringService.processDueTransactions();

        verify(recurringRepository).save(argThat(r -> r.getNextDueDate().equals(today.plusWeeks(1))));
    }

    @Test
    @DisplayName("processDueTransactions: QUARTERLY frequency should advance by 3 months")
    void processDueTransactions_quarterlyFrequency() {
        LocalDate today = LocalDate.now();
        netflixRule.setFrequency("QUARTERLY");
        netflixRule.setNextDueDate(today);

        when(recurringRepository.findByIsActiveAndNextDueDateLessThanEqual(true, today))
                .thenReturn(Arrays.asList(netflixRule));
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doReturn(Map.of("expenseId", 71)).when(expenseClient).createExpense(anyMap(), anyString());

        recurringService.processDueTransactions();

        verify(recurringRepository).save(argThat(r -> r.getNextDueDate().equals(today.plusMonths(3))));
    }

    @Test
    @DisplayName("processDueTransactions: YEARLY frequency should advance by 1 year")
    void processDueTransactions_yearlyFrequency() {
        LocalDate today = LocalDate.now();
        netflixRule.setFrequency("YEARLY");
        netflixRule.setNextDueDate(today);

        when(recurringRepository.findByIsActiveAndNextDueDateLessThanEqual(true, today))
                .thenReturn(Arrays.asList(netflixRule));
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doReturn(Map.of("expenseId", 81)).when(expenseClient).createExpense(anyMap(), anyString());

        recurringService.processDueTransactions();

        verify(recurringRepository).save(argThat(r -> r.getNextDueDate().equals(today.plusYears(1))));
    }

    @Test
    @DisplayName("processDueTransactions: unknown frequency should fallback to monthly")
    void processDueTransactions_unknownFrequencyFallback() {
        LocalDate today = LocalDate.now();
        netflixRule.setFrequency("WEIRD");
        netflixRule.setNextDueDate(today);

        when(recurringRepository.findByIsActiveAndNextDueDateLessThanEqual(true, today))
                .thenReturn(Arrays.asList(netflixRule));
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doReturn(Map.of("expenseId", 91)).when(expenseClient).createExpense(anyMap(), anyString());

        recurringService.processDueTransactions();

        verify(recurringRepository).save(argThat(r -> r.getNextDueDate().equals(today.plusMonths(1))));
    }

    @Test
    @DisplayName("processDueTransactions: should deactivate before processing when nextDueDate already past endDate")
    void processDueTransactions_shouldDeactivateEarly_whenAlreadyPastEndDate() {
        LocalDate today = LocalDate.now();
        netflixRule.setNextDueDate(today);
        netflixRule.setEndDate(today.minusDays(1));

        when(recurringRepository.findByIsActiveAndNextDueDateLessThanEqual(true, today))
                .thenReturn(Arrays.asList(netflixRule));
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        recurringService.processDueTransactions();

        verify(recurringRepository).save(argThat(r -> !r.isActive()));
        verify(expenseClient, never()).createExpense(anyMap(), anyString());
    }


    @Test
    @DisplayName("updateRecurring: same-type EXPENSE should update linked expense via REST")
    void updateRecurring_sameTypeExpense_shouldUpdateLinkedExpense() {
        netflixRule.setLastGeneratedExpenseId(101);
        netflixRule.setLastGeneratedDate(LocalDate.now().minusDays(1));

        RecurringTransaction updated = new RecurringTransaction();
        updated.setUserId(1);
        updated.setCategoryId(6);
        updated.setTitle("Updated Netflix");
        updated.setAmount(749.0);
        updated.setCurrency("INR");
        updated.setType("EXPENSE");
        updated.setFrequency("MONTHLY");
        updated.setStartDate(LocalDate.now());
        updated.setNextDueDate(LocalDate.now().plusMonths(1));
        updated.setActive(true);
        updated.setPaymentMethod("CARD");

        when(recurringRepository.findByRecurringId(1)).thenReturn(Optional.of(netflixRule));
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        recurringService.updateRecurring(1, updated);

        verify(expenseClient).updateExpense(eq(101), anyMap(), anyString());
        verify(expenseClient, never()).createExpense(anyMap(), anyString());
    }

    @Test
    @DisplayName("updateRecurring: same-type INCOME should update linked income via REST")
    void updateRecurring_sameTypeIncome_shouldUpdateLinkedIncome() {
        salaryRule.setLastGeneratedIncomeId(202);
        salaryRule.setLastGeneratedDate(LocalDate.now().minusDays(2));

        RecurringTransaction updated = new RecurringTransaction();
        updated.setUserId(1);
        updated.setCategoryId(9);
        updated.setTitle("Updated Salary");
        updated.setAmount(80000.0);
        updated.setCurrency("INR");
        updated.setType("INCOME");
        updated.setFrequency("MONTHLY");
        updated.setSource("SALARY");
        updated.setStartDate(LocalDate.now());
        updated.setNextDueDate(LocalDate.now().plusMonths(1));
        updated.setActive(true);

        when(recurringRepository.findByRecurringId(2)).thenReturn(Optional.of(salaryRule));
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        recurringService.updateRecurring(2, updated);

        verify(incomeClient).updateIncome(eq(202), anyMap(), anyString());
        verify(incomeClient, never()).createIncome(anyMap(), anyString());
    }

    @Test
    @DisplayName("updateRecurring: should set source to null when updated source is null")
    void updateRecurring_shouldNullifySource_whenSourceMissing() {
        netflixRule.setSource("PREVIOUS");
        RecurringTransaction updated = new RecurringTransaction();
        updated.setUserId(1);
        updated.setCategoryId(6);
        updated.setTitle("Netflix");
        updated.setAmount(649.0);
        updated.setCurrency("INR");
        updated.setType("EXPENSE");
        updated.setFrequency("MONTHLY");
        updated.setStartDate(LocalDate.now());
        updated.setNextDueDate(LocalDate.now().plusMonths(1));
        updated.setActive(true);
        // source intentionally null

        when(recurringRepository.findByRecurringId(1)).thenReturn(Optional.of(netflixRule));
        when(recurringRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        recurringService.updateRecurring(1, updated);

        verify(recurringRepository, atLeastOnce()).save(argThat(r -> r.getSource() == null));
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
