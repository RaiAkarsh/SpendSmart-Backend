package com.spendsmart.income;

import com.spendsmart.income.entity.Income;
import com.spendsmart.income.repository.IncomeRepository;
import com.spendsmart.income.serviceimpl.IncomeServiceImpl;
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
@DisplayName("IncomeServiceImpl Unit Tests")
class IncomeServiceImplTest {

    @Mock
    private IncomeRepository incomeRepository;

    @InjectMocks
    private IncomeServiceImpl incomeService;

    private Income salaryIncome;
    private Income freelanceIncome;

    @BeforeEach
    void setUp() {
        salaryIncome = new Income();
        salaryIncome.setIncomeId(1);
        salaryIncome.setUserId(1);
        salaryIncome.setCategoryId(9);
        salaryIncome.setTitle("April Salary");
        salaryIncome.setAmount(75000.0);
        salaryIncome.setCurrency("INR");
        salaryIncome.setSource("SALARY");
        salaryIncome.setDate(LocalDate.of(2026, 4, 1));
        salaryIncome.setRecurring(true);
        salaryIncome.setRecurrencePeriod("MONTHLY");

        freelanceIncome = new Income();
        freelanceIncome.setIncomeId(2);
        freelanceIncome.setUserId(1);
        freelanceIncome.setCategoryId(10);
        freelanceIncome.setTitle("Website project");
        freelanceIncome.setAmount(25000.0);
        freelanceIncome.setSource("FREELANCE");
        freelanceIncome.setDate(LocalDate.of(2026, 4, 15));
        freelanceIncome.setRecurring(false);
    }


    @Test
    @DisplayName("addIncome: should normalize source to UPPERCASE before saving")
    void addIncome_shouldNormalizeSourceToUppercase() {
        Income input = new Income();
        input.setUserId(1);
        input.setTitle("Salary");
        input.setAmount(75000.0);
        input.setSource("salary");       // lowercase  -  should be normalized
        input.setDate(LocalDate.now());
        when(incomeRepository.save(any(Income.class))).thenReturn(salaryIncome);

        incomeService.addIncome(input);

        // Verify source was uppercased before save
        verify(incomeRepository).save(argThat(i -> "SALARY".equals(i.getSource())));
    }

    @Test
    @DisplayName("addIncome: should normalize recurrencePeriod to UPPERCASE")
    void addIncome_shouldNormalizeRecurrencePeriod() {
        Income input = new Income();
        input.setUserId(1);
        input.setTitle("Salary");
        input.setAmount(75000.0);
        input.setSource("SALARY");
        input.setDate(LocalDate.now());
        input.setRecurring(true);
        input.setRecurrencePeriod("monthly");   // lowercase
        when(incomeRepository.save(any())).thenReturn(salaryIncome);

        incomeService.addIncome(input);

        verify(incomeRepository).save(argThat(i -> "MONTHLY".equals(i.getRecurrencePeriod())));
    }


    @Test
    @DisplayName("getIncomeById: should return income when found")
    void getIncomeById_shouldReturn_whenFound() {
        when(incomeRepository.findByIncomeId(1)).thenReturn(Optional.of(salaryIncome));
        Income result = incomeService.getIncomeById(1);
        assertEquals("April Salary", result.getTitle());
    }

    @Test
    @DisplayName("getIncomeById: should throw when not found")
    void getIncomeById_shouldThrow_whenNotFound() {
        when(incomeRepository.findByIncomeId(999)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> incomeService.getIncomeById(999));
        assertTrue(ex.getMessage().contains("not found"));
    }


    @Test
    @DisplayName("getIncomesBySource: should uppercase source before querying")
    void getIncomesBySource_shouldUppercaseSource() {
        when(incomeRepository.findByUserIdAndSource(1, "FREELANCE"))
                .thenReturn(Arrays.asList(freelanceIncome));

        List<Income> result = incomeService.getIncomesBySource(1, "freelance");
        assertEquals(1, result.size());
        verify(incomeRepository).findByUserIdAndSource(1, "FREELANCE");
    }


    @Test
    @DisplayName("getIncomesByDateRange: should throw when startDate after endDate")
    void getIncomesByDateRange_shouldThrow_whenStartAfterEnd() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> incomeService.getIncomesByDateRange(1,
                        LocalDate.of(2026, 4, 30),
                        LocalDate.of(2026, 4, 1)));
        assertTrue(ex.getMessage().contains("startDate cannot be after endDate"));
    }


    @Test
    @DisplayName("getTotalByUser: should return 0.0 when no income (null from SUM)")
    void getTotalByUser_shouldReturnZero_whenNull() {
        when(incomeRepository.sumAmountByUserId(1)).thenReturn(null);
        assertEquals(0.0, incomeService.getTotalByUser(1));
    }

    @Test
    @DisplayName("getTotalByUser: should return correct total")
    void getTotalByUser_shouldReturnTotal() {
        when(incomeRepository.sumAmountByUserId(1)).thenReturn(105000.0);
        assertEquals(105000.0, incomeService.getTotalByUser(1));
    }


    @Test
    @DisplayName("getTotalByMonth: should return 0.0 when null")
    void getTotalByMonth_shouldReturnZero_whenNull() {
        when(incomeRepository.sumAmountByUserIdAndMonth(1, 4, 2026)).thenReturn(null);
        assertEquals(0.0, incomeService.getTotalByMonth(1, 4, 2026));
    }


    @Test
    @DisplayName("getTotalBySource: should uppercase source and return correct sum")
    void getTotalBySource_shouldUppercaseAndReturn() {
        when(incomeRepository.sumAmountByUserIdAndSource(1, "SALARY"))
                .thenReturn(150000.0);
        double total = incomeService.getTotalBySource(1, "salary");
        assertEquals(150000.0, total);
        verify(incomeRepository).sumAmountByUserIdAndSource(1, "SALARY");
    }


    @Test
    @DisplayName("updateIncome: should update all fields and normalize source")
    void updateIncome_shouldUpdateAndNormalizeSource() {
        when(incomeRepository.findByIncomeId(2)).thenReturn(Optional.of(freelanceIncome));
        when(incomeRepository.save(any())).thenReturn(freelanceIncome);

        Income updates = new Income();
        updates.setCategoryId(10);
        updates.setTitle("Final payment");
        updates.setAmount(30000.0);
        updates.setCurrency("INR");
        updates.setSource("freelance");   // lowercase  -  should be normalized
        updates.setDate(LocalDate.of(2026, 4, 15));

        incomeService.updateIncome(2, updates);

        verify(incomeRepository).save(argThat(i ->
                "FREELANCE".equals(i.getSource()) && i.getAmount() == 30000.0));
    }


    @Test
    @DisplayName("getRecurringIncomes: should return only isRecurring=true entries")
    void getRecurringIncomes_shouldReturnOnlyRecurring() {
        when(incomeRepository.findByUserIdAndIsRecurring(1, true))
                .thenReturn(Arrays.asList(salaryIncome));

        List<Income> result = incomeService.getRecurringIncomes(1);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isRecurring());
    }


    @Test
    @DisplayName("deleteIncome: should delete when income exists")
    void deleteIncome_shouldDelete_whenExists() {
        when(incomeRepository.findByIncomeId(1)).thenReturn(Optional.of(salaryIncome));
        assertDoesNotThrow(() -> incomeService.deleteIncome(1));
        verify(incomeRepository, times(1)).deleteByIncomeId(1);
    }

    @Test
    @DisplayName("deleteIncome: should throw when income not found")
    void deleteIncome_shouldThrow_whenNotFound() {
        when(incomeRepository.findByIncomeId(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> incomeService.deleteIncome(99));
        verify(incomeRepository, never()).deleteByIncomeId(anyInt());
    }
}