package com.spendsmart.recurring.serviceimpl;

import com.spendsmart.recurring.client.ExpenseClient;
import com.spendsmart.recurring.client.IncomeClient;
import com.spendsmart.recurring.entity.RecurringTransaction;
import com.spendsmart.recurring.repository.RecurringRepository;
import com.spendsmart.recurring.service.RecurringService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecurringServiceImpl implements RecurringService {

    @Autowired
    private RecurringRepository recurringRepository;

    @Autowired
    private ExpenseClient expenseClient;

    @Autowired
    private IncomeClient incomeClient;

    @Value("${expense.service.url}")
    private String expenseServiceUrl;

    @Value("${income.service.url}")
    private String incomeServiceUrl;

    @Value("${jwt.secret}")
    private String secret;

    // Creates a new recurring rule.
    // type and frequency are normalized to UPPERCASE.
    @Override
    public RecurringTransaction addRecurring(RecurringTransaction recurring) {
        if (recurring.getType() != null)
            recurring.setType(recurring.getType().toUpperCase());
        if (recurring.getFrequency() != null)
            recurring.setFrequency(recurring.getFrequency().toUpperCase());
        if (recurring.getSource() != null)
            recurring.setSource(recurring.getSource().toUpperCase());

        recurring.setNextDueDate(recurring.getStartDate());
        recurring.setActive(true);
        recurring.setCreatedAt(LocalDateTime.now());
        RecurringTransaction saved = recurringRepository.save(recurring);

        // UX fix: if rule starts today/past, generate first transaction immediately
        // instead of waiting for midnight scheduler.
        if (saved.isActive()
                && saved.getNextDueDate() != null
                && !saved.getNextDueDate().isAfter(LocalDate.now())) {
            try {
                saved = generateImmediateOccurrence(saved);
            } catch (Exception e) {
                System.err.println("Immediate recurring generation failed for id="
                        + saved.getRecurringId() + ": " + e.getMessage());
            }
        }
        return saved;
    }

    @Override
    public RecurringTransaction getById(int recurringId) {
        return recurringRepository.findByRecurringId(recurringId)
                .orElseThrow(() -> new RuntimeException(
                    "Recurring rule not found with id: " + recurringId));
    }

    @Override
    public List<RecurringTransaction> getByUser(int userId) {
        return recurringRepository.findByUserId(userId);
    }

    @Override
    public List<RecurringTransaction> getActiveByUser(int userId) {
        return recurringRepository.findByUserIdAndIsActive(userId, true);
    }

    @Override
    public List<RecurringTransaction> getByUserAndType(int userId, String type) {
        return recurringRepository.findByUserIdAndType(userId, type.toUpperCase());
    }

    // Returns active rules whose nextDueDate falls within the current month.
    // Used in the dashboard "Upcoming Transactions" reminder widget.
    @Override
    public List<RecurringTransaction> getUpcomingThisMonth(int userId) {
        LocalDate today = LocalDate.now();
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        return recurringRepository.findByUserIdAndIsActiveAndNextDueDateBetween(
                userId, true, today, endOfMonth);
    }

    // Updates a recurring rule.
    // IMPORTANT: changes only affect FUTURE generations.
    // Past auto-generated transactions already in expense/income-service
    @Override
    public RecurringTransaction updateRecurring(int recurringId, RecurringTransaction updated) {
        RecurringTransaction existing = getById(recurringId);
        String previousType = existing.getType();
        existing.setCategoryId(updated.getCategoryId());
        existing.setTitle(updated.getTitle());
        existing.setAmount(updated.getAmount());
        existing.setCurrency(updated.getCurrency());
        existing.setStartDate(updated.getStartDate());
        existing.setNextDueDate(updated.getNextDueDate());
        existing.setEndDate(updated.getEndDate());
        existing.setDescription(updated.getDescription());
        existing.setActive(updated.isActive());

        if (updated.getType() != null) {
            existing.setType(updated.getType().toUpperCase());
        }
        if (updated.getFrequency() != null)
            existing.setFrequency(updated.getFrequency().toUpperCase());
        if (updated.getSource() != null) {
            existing.setSource(updated.getSource().toUpperCase());
        } else {
            existing.setSource(null);
        }
        existing.setPaymentMethod(updated.getPaymentMethod());
        RecurringTransaction saved = recurringRepository.save(existing);

        reconcileGeneratedOccurrence(saved, previousType);
        saved = recurringRepository.save(saved);

        // Keep behavior consistent with create:
        // if rule is active and due now/past, generate immediately.
        if (saved.isActive()
                && saved.getNextDueDate() != null
                && !saved.getNextDueDate().isAfter(LocalDate.now())) {
            try {
                if (saved.getLastGeneratedExpenseId() == null && saved.getLastGeneratedIncomeId() == null) {
                    saved = generateImmediateOccurrence(saved);
                }
            } catch (Exception e) {
                System.err.println("Immediate recurring generation after update failed for id="
                        + saved.getRecurringId() + ": " + e.getMessage());
            }
        }
        return saved;
    }

    // Historical records are preserved.
    @Override
    public void deactivateRecurring(int recurringId) {
        RecurringTransaction rule = getById(recurringId);
        rule.setActive(false);
        recurringRepository.save(rule);
    }

    @Override
    public void activateRecurring(int recurringId) {
        RecurringTransaction rule = getById(recurringId);
        rule.setActive(true);
        recurringRepository.save(rule);
    }

    @Override
    @Transactional
    public void deleteRecurring(int recurringId) {
        getById(recurringId); // verify exists
        recurringRepository.deleteByRecurringId(recurringId);
    }

    // @Scheduled(cron = "0 0 0 * * *") means:
    //   second=0, minute=0, hour=0, day=any, month=any, weekday=any
    //   = run at 00:00:00 every day
    //   1. Find all ACTIVE rules where nextDueDate <= today
    //   2. For each rule: call expense-service or income-service via REST
    //   3. Advance nextDueDate by one frequency period
    //   POST /recurring/process
    @Override
    @Scheduled(cron = "0 0 0 * * *")
    public void processDueTransactions() {
        LocalDate today = LocalDate.now();

        // Find all active rules due today or overdue
        List<RecurringTransaction> dueRules =
            recurringRepository.findByIsActiveAndNextDueDateLessThanEqual(true, today);

        for (RecurringTransaction rule : dueRules) {
            try {
                // Step 1: Generate the actual transaction
                if ("EXPENSE".equals(rule.getType())) {
                    Map<String, Object> response = createExpenseOccurrence(rule, rule.getNextDueDate());
                    rule.setLastGeneratedExpenseId(extractId(response, "expenseId"));
                    rule.setLastGeneratedIncomeId(null);
                } else if ("INCOME".equals(rule.getType())) {
                    Map<String, Object> response = createIncomeOccurrence(rule, rule.getNextDueDate());
                    rule.setLastGeneratedIncomeId(extractId(response, "incomeId"));
                    rule.setLastGeneratedExpenseId(null);
                }
                rule.setLastGeneratedDate(rule.getNextDueDate());

                // Step 2: Advance nextDueDate
                LocalDate newDueDate = calculateNextDueDate(rule.getNextDueDate(), rule.getFrequency());
                rule.setNextDueDate(newDueDate);

                // Step 3: Deactivate if past endDate
                if (rule.getEndDate() != null && newDueDate.isAfter(rule.getEndDate())) {
                    rule.setActive(false);
                }

                recurringRepository.save(rule);

            } catch (Exception e) {
                // Log the error but continue processing other rules
                System.err.println("Failed to process recurring rule id="
                    + rule.getRecurringId() + ": " + e.getMessage());
            }
        }
    }

    private RecurringTransaction generateImmediateOccurrence(RecurringTransaction rule) {
        LocalDate occurrenceDate = rule.getNextDueDate();

        if ("EXPENSE".equals(rule.getType())) {
            Map<String, Object> response = createExpenseOccurrence(rule, occurrenceDate);
            rule.setLastGeneratedExpenseId(extractId(response, "expenseId"));
            rule.setLastGeneratedIncomeId(null);
        } else if ("INCOME".equals(rule.getType())) {
            Map<String, Object> response = createIncomeOccurrence(rule, occurrenceDate);
            rule.setLastGeneratedIncomeId(extractId(response, "incomeId"));
            rule.setLastGeneratedExpenseId(null);
        }

        rule.setLastGeneratedDate(occurrenceDate);
        rule.setNextDueDate(calculateNextDueDate(rule.getNextDueDate(), rule.getFrequency()));
        if (rule.getEndDate() != null && rule.getNextDueDate().isAfter(rule.getEndDate())) {
            rule.setActive(false);
        }
        return recurringRepository.save(rule);
    }

    private void reconcileGeneratedOccurrence(RecurringTransaction rule, String previousType) {
        LocalDate generatedDate = rule.getLastGeneratedDate();
        if (generatedDate == null) {
            return;
        }

        String authorization = buildAuthorizationHeader(rule.getUserId());
        boolean typeChanged = previousType != null && !previousType.equalsIgnoreCase(rule.getType());

        if (typeChanged) {
            deleteLinkedExpense(rule, authorization);
            deleteLinkedIncome(rule, authorization);

            if ("EXPENSE".equals(rule.getType())) {
                Map<String, Object> response = createExpenseOccurrence(rule, generatedDate);
                rule.setLastGeneratedExpenseId(extractId(response, "expenseId"));
                rule.setLastGeneratedIncomeId(null);
            } else {
                Map<String, Object> response = createIncomeOccurrence(rule, generatedDate);
                rule.setLastGeneratedIncomeId(extractId(response, "incomeId"));
                rule.setLastGeneratedExpenseId(null);
            }
            return;
        }

        if ("EXPENSE".equals(rule.getType())) {
            if (rule.getLastGeneratedExpenseId() != null) {
                expenseClient.updateExpense(
                        rule.getLastGeneratedExpenseId(),
                        buildExpensePayload(rule, generatedDate),
                        authorization
                );
            } else {
                Map<String, Object> response = createExpenseOccurrence(rule, generatedDate);
                rule.setLastGeneratedExpenseId(extractId(response, "expenseId"));
            }
            rule.setLastGeneratedIncomeId(null);
        } else if ("INCOME".equals(rule.getType())) {
            if (rule.getLastGeneratedIncomeId() != null) {
                incomeClient.updateIncome(
                        rule.getLastGeneratedIncomeId(),
                        buildIncomePayload(rule, generatedDate),
                        authorization
                );
            } else {
                Map<String, Object> response = createIncomeOccurrence(rule, generatedDate);
                rule.setLastGeneratedIncomeId(extractId(response, "incomeId"));
            }
            rule.setLastGeneratedExpenseId(null);
        }
    }

    private Map<String, Object> createExpenseOccurrence(RecurringTransaction rule, LocalDate date) {
        return expenseClient.createExpense(
                buildExpensePayload(rule, date),
                buildAuthorizationHeader(rule.getUserId())
        );
    }

    private Map<String, Object> createIncomeOccurrence(RecurringTransaction rule, LocalDate date) {
        return incomeClient.createIncome(
                buildIncomePayload(rule, date),
                buildAuthorizationHeader(rule.getUserId())
        );
    }

    private Map<String, Object> buildExpensePayload(RecurringTransaction rule, LocalDate date) {
        Map<String, Object> expenseBody = new HashMap<>();
        expenseBody.put("userId", rule.getUserId());
        expenseBody.put("categoryId", rule.getCategoryId());
        expenseBody.put("title", rule.getTitle());
        expenseBody.put("amount", rule.getAmount());
        expenseBody.put("currency", rule.getCurrency());
        expenseBody.put("paymentMethod", rule.getPaymentMethod() != null ? rule.getPaymentMethod() : "CASH");
        expenseBody.put("date", date.toString());
        expenseBody.put("notes", "Auto-generated by recurring rule #" + rule.getRecurringId());
        expenseBody.put("isRecurring", false);
        return expenseBody;
    }

    private Map<String, Object> buildIncomePayload(RecurringTransaction rule, LocalDate date) {
        Map<String, Object> incomeBody = new HashMap<>();
        incomeBody.put("userId", rule.getUserId());
        incomeBody.put("categoryId", rule.getCategoryId());
        incomeBody.put("title", rule.getTitle());
        incomeBody.put("amount", rule.getAmount());
        incomeBody.put("currency", rule.getCurrency());
        incomeBody.put("source", rule.getSource() != null ? rule.getSource() : "OTHER");
        incomeBody.put("date", date.toString());
        incomeBody.put("notes", "Auto-generated by recurring rule #" + rule.getRecurringId());
        incomeBody.put("isRecurring", false);
        return incomeBody;
    }

    private void deleteLinkedExpense(RecurringTransaction rule, String authorization) {
        if (rule.getLastGeneratedExpenseId() != null) {
            expenseClient.deleteExpense(rule.getLastGeneratedExpenseId(), authorization);
            rule.setLastGeneratedExpenseId(null);
        }
    }

    private void deleteLinkedIncome(RecurringTransaction rule, String authorization) {
        if (rule.getLastGeneratedIncomeId() != null) {
            incomeClient.deleteIncome(rule.getLastGeneratedIncomeId(), authorization);
            rule.setLastGeneratedIncomeId(null);
        }
    }

    private Integer extractId(Map<String, Object> response, String key) {
        if (response == null) {
            return null;
        }
        Object value = response.get(key);
        return value instanceof Number number ? number.intValue() : null;
    }

    // Advances the date by one frequency unit.
    private LocalDate calculateNextDueDate(LocalDate current, String frequency) {
        return switch (frequency.toUpperCase()) {
            case "DAILY"     -> current.plusDays(1);
            case "WEEKLY"    -> current.plusWeeks(1);
            case "MONTHLY"   -> current.plusMonths(1);
            case "QUARTERLY" -> current.plusMonths(3);
            case "YEARLY"    -> current.plusYears(1);
            default -> current.plusMonths(1); // fallback to monthly
        };
    }

    @Override
    public int countActiveRules(int userId) {
        return recurringRepository.countByUserIdAndIsActive(userId, true);
    }

    private String buildAuthorizationHeader(int userId) {
        return "Bearer " + generateServiceToken(userId);
    }

    private String generateServiceToken(int userId) {
        return Jwts.builder()
                .subject("recurring-service")
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
