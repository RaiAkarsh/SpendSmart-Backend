package com.spendsmart.income.repository;

import com.spendsmart.income.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Integer> {

    // SELECT * FROM incomes WHERE user_id = ? ORDER BY date DESC
    List<Income> findByUserIdOrderByDateDesc(int userId);

    // SELECT * FROM incomes WHERE user_id = ? AND source = ?
    // source = SALARY, FREELANCE, BUSINESS, INVESTMENT, GIFT, OTHER
    List<Income> findByUserIdAndSource(int userId, String source);

    // SELECT * FROM incomes WHERE user_id = ? AND category_id = ?
    List<Income> findByUserIdAndCategoryId(int userId, int categoryId);

    // SELECT * FROM incomes WHERE income_id = ?
    Optional<Income> findByIncomeId(int incomeId);

    // SELECT * FROM incomes WHERE user_id = ? AND date BETWEEN ? AND ?
    List<Income> findByUserIdAndDateBetween(int userId, LocalDate startDate, LocalDate endDate);

    // SELECT * FROM incomes WHERE user_id = ? AND MONTH(date)=? AND YEAR(date)=?
    @Query("SELECT i FROM Income i WHERE i.userId = :userId " +
           "AND MONTH(i.date) = :month AND YEAR(i.date) = :year " +
           "ORDER BY i.date DESC")
    List<Income> findByUserIdAndMonth(
            @Param("userId") int userId,
            @Param("month") int month,
            @Param("year") int year);

    // SELECT * FROM incomes WHERE user_id = ? AND is_recurring = true/false
    List<Income> findByUserIdAndIsRecurring(int userId, boolean isRecurring);

    // Keyword search across title and notes
    @Query("SELECT i FROM Income i WHERE i.userId = :userId " +
           "AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(i.notes) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY i.date DESC")
    List<Income> searchByKeyword(@Param("userId") int userId, @Param("keyword") String keyword);

    // SELECT SUM(amount) FROM incomes WHERE user_id = ?
    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.userId = :userId")
    Double sumAmountByUserId(@Param("userId") int userId);

    // SELECT SUM(amount) WHERE user_id = ? AND MONTH(date)=? AND YEAR(date)=?
    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.userId = :userId " +
           "AND MONTH(i.date) = :month AND YEAR(i.date) = :year")
    Double sumAmountByUserIdAndMonth(
            @Param("userId") int userId,
            @Param("month") int month,
            @Param("year") int year);

    // SELECT SUM(amount) WHERE user_id = ? AND source = ?
    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.userId = :userId AND i.source = :source")
    Double sumAmountByUserIdAndSource(@Param("userId") int userId, @Param("source") String source);

    // DELETE FROM incomes WHERE income_id = ?
    void deleteByIncomeId(int incomeId);
}