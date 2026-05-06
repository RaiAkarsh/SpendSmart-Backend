package com.spendsmart.income.serviceimpl;

import com.spendsmart.income.entity.Income;
import com.spendsmart.income.repository.IncomeRepository;
import com.spendsmart.income.service.IncomeService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncomeServiceImpl implements IncomeService {

    @Autowired
    private IncomeRepository incomeRepository;

    // Saves a new income entry.
    // source is normalized to UPPERCASE before saving.
    // recurrencePeriod is also normalized if provided.
    @Override
    public Income addIncome(Income income) {
        // Normalize source to uppercase for consistent storage
        if (income.getSource() != null) {
            income.setSource(income.getSource().toUpperCase());
        }
        // Normalize recurrencePeriod if provided
        if (income.getRecurrencePeriod() != null) {
            income.setRecurrencePeriod(income.getRecurrencePeriod().toUpperCase());
        }
        income.setCreatedAt(LocalDateTime.now());
        return incomeRepository.save(income);
    }

    @Override
    public Income getIncomeById(int incomeId) {
        return incomeRepository.findByIncomeId(incomeId)
                .orElseThrow(() -> new RuntimeException("Income not found with id: " + incomeId));
    }

    // Returns all income entries for a user, newest first.
    @Override
    public List<Income> getIncomesByUser(int userId) {
        return incomeRepository.findByUserIdOrderByDateDesc(userId);
    }

    // Filters by source type.
    // Powers the "Income Breakdown by Source" chart in analytics-service.
    @Override
    public List<Income> getIncomesBySource(int userId, String source) {
        return incomeRepository.findByUserIdAndSource(userId, source.toUpperCase());
    }

    @Override
    public List<Income> getIncomesByCategory(int userId, int categoryId) {
        return incomeRepository.findByUserIdAndCategoryId(userId, categoryId);
    }

    @Override
    public List<Income> getIncomesByDateRange(int userId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("startDate cannot be after endDate");
        }
        return incomeRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
    }

    // Used by Monthly Summary Dashboard to compute net savings:
    //   netSavings = totalIncome(month) - totalExpenses(month)
    @Override
    public List<Income> getIncomesByMonth(int userId, int month, int year) {
        return incomeRepository.findByUserIdAndMonth(userId, month, year);
    }

    @Override
    public List<Income> searchIncomes(int userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getIncomesByUser(userId);
        }
        return incomeRepository.searchByKeyword(userId, keyword.trim());
    }

    // Returns only recurring income entries.
    // recurring-service uses this list to auto-generate future income entries
    // on their due dates (salary on 1st of month, etc.)
    @Override
    public List<Income> getRecurringIncomes(int userId) {
        return incomeRepository.findByUserIdAndIsRecurring(userId, true);
    }

    // All fields can be updated including source and recurrencePeriod.
    @Override
    public Income updateIncome(int incomeId, Income updatedIncome) {
        Income existing = getIncomeById(incomeId);

        existing.setCategoryId(updatedIncome.getCategoryId());
        existing.setTitle(updatedIncome.getTitle());
        existing.setAmount(updatedIncome.getAmount());
        existing.setCurrency(updatedIncome.getCurrency());

        // Normalize source to uppercase
        if (updatedIncome.getSource() != null) {
            existing.setSource(updatedIncome.getSource().toUpperCase());
        }

        existing.setDate(updatedIncome.getDate());
        existing.setNotes(updatedIncome.getNotes());
        existing.setRecurring(updatedIncome.isRecurring());

        // Normalize recurrencePeriod if provided
        if (updatedIncome.getRecurrencePeriod() != null) {
            existing.setRecurrencePeriod(updatedIncome.getRecurrencePeriod().toUpperCase());
        } else {
            existing.setRecurrencePeriod(null);
        }

        return incomeRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteIncome(int incomeId) {
        getIncomeById(incomeId); // verify exists first
        incomeRepository.deleteByIncomeId(incomeId);
    }

    // SUM of all income for a user across all time.
    // Used by analytics-service for lifetime financial overview.
    @Override
    public double getTotalByUser(int userId) {
        Double result = incomeRepository.sumAmountByUserId(userId);
        return result == null ? 0.0 : result;
    }

    // Used in Monthly Summary Dashboard:
    //   totalIncome(April 2026) - totalExpenses(April 2026) = netSavings
    @Override
    public double getTotalByMonth(int userId, int month, int year) {
        Double result = incomeRepository.sumAmountByUserIdAndMonth(userId, month, year);
        return result == null ? 0.0 : result;
    }

    // Total received from a specific source.
    // Powers the "Income by Source" breakdown chart.
    @Override
    public double getTotalBySource(int userId, String source) {
        Double result = incomeRepository.sumAmountByUserIdAndSource(userId, source.toUpperCase());
        return result == null ? 0.0 : result;
    }
}