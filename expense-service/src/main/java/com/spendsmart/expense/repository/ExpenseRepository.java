package com.spendsmart.expense.repository;

import com.spendsmart.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer> {

    // SELECT * FROM expenses WHERE user_id = ?
    List<Expense> findByUserIdOrderByDateDesc(int userId);

    // SELECT * FROM expenses WHERE user_id = ? AND type = ?
    // Filters by type: EXPENSE or SPLIT
    List<Expense> findByUserIdAndType(int userId, String type);

    // SELECT * FROM expenses WHERE category_id = ?
    List<Expense> findByCategoryId(int categoryId);

    // SELECT * FROM expenses WHERE user_id = ? AND category_id = ?
    List<Expense> findByUserIdAndCategoryId(int userId, int categoryId);

    // SELECT * FROM expenses WHERE expense_id = ?
    Optional<Expense> findByExpenseId(int expenseId);

    // SELECT * FROM expenses WHERE user_id = ? AND date BETWEEN ? AND ?
    // Used for date-range filtering (e.g. last 30 days, custom range)
    List<Expense> findByUserIdAndDateBetween(int userId, LocalDate startDate, LocalDate endDate);

    // SELECT * FROM expenses WHERE user_id = ? AND MONTH(date)=? AND YEAR(date)=?
    // Used for monthly expense view (April 2026)
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
           "AND MONTH(e.date) = :month AND YEAR(e.date) = :year " +
           "ORDER BY e.date DESC")
    List<Expense> findByUserIdAndMonth(
            @Param("userId") int userId,
            @Param("month") int month,
            @Param("year") int year);

    // SELECT * FROM expenses WHERE user_id = ? AND is_recurring = true
    List<Expense> findByUserIdAndIsRecurring(int userId, boolean isRecurring);

    // SELECT * FROM expenses WHERE user_id = ?
    // AND (title LIKE %keyword% OR notes LIKE %keyword%)
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
           "AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(e.notes) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY e.date DESC")
    List<Expense> searchByKeyword(@Param("userId") int userId, @Param("keyword") String keyword);

    // SELECT * FROM expenses WHERE user_id = ? AND payment_method = ?
    List<Expense> findByUserIdAndPaymentMethod(int userId, String paymentMethod);

    // SELECT SUM(amount) FROM expenses WHERE user_id = ?
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId")
    Double sumAmountByUserId(@Param("userId") int userId);

    // SELECT SUM(amount) FROM expenses WHERE user_id = ? AND category_id = ?
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId AND e.categoryId = :categoryId")
    Double sumAmountByUserIdAndCategoryId(@Param("userId") int userId, @Param("categoryId") int categoryId);

    // SELECT SUM(amount) WHERE user_id=? AND MONTH(date)=? AND YEAR(date)=?
    // Used to calculate total monthly spending
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId " +
           "AND MONTH(e.date) = :month AND YEAR(e.date) = :year")
    Double sumAmountByUserIdAndMonth(
            @Param("userId") int userId,
            @Param("month") int month,
            @Param("year") int year);

    // DELETE FROM expenses WHERE expense_id = ?
    void deleteByExpenseId(int expenseId);
}