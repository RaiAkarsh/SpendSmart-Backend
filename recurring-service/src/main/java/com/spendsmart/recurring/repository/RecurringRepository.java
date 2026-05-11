package com.spendsmart.recurring.repository;

import com.spendsmart.recurring.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringRepository extends JpaRepository<RecurringTransaction, Integer> {

    // All recurring rules for a user
    List<RecurringTransaction> findByUserId(int userId);

    // Only active rules for a user
    List<RecurringTransaction> findByUserIdAndIsActive(int userId, boolean isActive);

    // Filter by type: EXPENSE or INCOME
    List<RecurringTransaction> findByUserIdAndType(int userId, String type);

    // Find by recurringId
    Optional<RecurringTransaction> findByRecurringId(int recurringId);

    // The @Scheduled job calls this every midnight to find what needs to be generated
    List<RecurringTransaction> findByIsActiveAndNextDueDateLessThanEqual(
            boolean isActive, LocalDate date);

    List<RecurringTransaction> findByUserIdAndIsActiveAndNextDueDateBetween(
            int userId, boolean isActive, LocalDate start, LocalDate end);

    // Count active rules for a user
    int countByUserIdAndIsActive(int userId, boolean isActive);

    // Delete by recurringId (used with @Transactional)
    void deleteByRecurringId(int recurringId);

    // Filter by frequency
    List<RecurringTransaction> findByUserIdAndFrequency(int userId, String frequency);
}