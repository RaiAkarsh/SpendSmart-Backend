package com.spendsmart.notification.serviceimpl;

import com.spendsmart.notification.client.BudgetClient;
import com.spendsmart.notification.entity.Notification;
import com.spendsmart.notification.repository.NotificationRepository;
import com.spendsmart.notification.service.NotificationService;
import jakarta.transaction.Transactional;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private BudgetClient budgetClient;

    @Value("${budget.service.url}")
    private String budgetServiceUrl;

    @Value("${jwt.secret}")
    private String secret;

    // Saves a new notification to the database.
    // type and priority are normalized to UPPERCASE.
    @Override
    public Notification createNotification(Notification notification) {
        if (notification.getType() != null)
            notification.setType(notification.getType().toUpperCase());
        if (notification.getPriority() != null)
            notification.setPriority(notification.getPriority().toUpperCase());
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Override
    public Notification getById(int notificationId) {
        return notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new RuntimeException(
                    "Notification not found with id: " + notificationId));
    }

    // Returns all notifications for a user, newest first.
    // Used in the notifications panel/dropdown.
    @Override
    public List<Notification> getByUser(int userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Notification> getUnreadByUser(int userId) {
        return notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
    }

    @Override
    public List<Notification> getByUserAndType(int userId, String type) {
        return notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                userId, type.toUpperCase());
    }

    @Override
    public List<Notification> getByUserAndPriority(int userId, String priority) {
        return notificationRepository.findByUserIdAndPriorityOrderByCreatedAtDesc(
                userId, priority.toUpperCase());
    }

    // Returns the count of unread notifications.
    @Override
    public int getUnreadCount(int userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    // Marks a single notification as read and records the timestamp.
    @Override
    public Notification markAsRead(int notificationId) {
        Notification notification = getById(notificationId);
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    // Marks ALL unread notifications for a user as read.
    // Triggered by "Mark all as read" button in the UI.
    @Override
    public void markAllAsRead(int userId) {
        List<Notification> unread = getUnreadByUser(userId);
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : unread) {
            n.setRead(true);
            n.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public void deleteNotification(int notificationId) {
        getById(notificationId); // verify exists
        notificationRepository.deleteByNotificationId(notificationId);
    }

    // Cleanup: deletes all read notifications for a user.
    // Called manually or by a scheduled cleanup job.
    @Override
    @Transactional
    public void deleteReadNotifications(int userId) {
        List<Notification> readNotifications =
            notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, true);
        notificationRepository.deleteAll(readNotifications);
    }

    // @Scheduled(cron = "0 0 * * * *") means:
    //   second=0, minute=0, hour=every, day=any, month=any, weekday=any
    //   = run at the top of every hour
    //   1. Call budget-service to get all budgets (GET /budgets)
    //   2. For each budget, check if spentAmount >= alertThreshold% of limit
    //   POST /notifications/check-budgets
    @Override
    @Scheduled(cron = "0 0 * * * *")
    @SuppressWarnings("unchecked")
    public void checkBudgetAlerts() {
        try {
            List<Map<String, Object>> budgets = budgetClient.getAllBudgets(buildAuthorizationHeader());

            if (budgets == null || budgets.isEmpty()) {
                return;
            }

            for (Map<String, Object> budget : budgets) {
                int budgetId = ((Number) budget.get("budgetId")).intValue();
                int userId = ((Number) budget.get("userId")).intValue();
                String name = (String) budget.get("name");
                double limitAmount = ((Number) budget.get("limitAmount")).doubleValue();
                double spentAmount = ((Number) budget.get("spentAmount")).doubleValue();
                int alertThreshold = ((Number) budget.get("alertThreshold")).intValue();
                boolean isActive = (Boolean) budget.get("active");

                if (!isActive || limitAmount <= 0) continue;

                double percentageUsed = (spentAmount / limitAmount) * 100;

                // Check if budget is EXCEEDED (100%+)
                if (spentAmount > limitAmount) {
                    if (!notificationRepository.existsActiveNotification(
                            userId, budgetId, "BUDGET", "BUDGET_EXCEEDED")) {
                        createBudgetExceeded(userId, budgetId, name, percentageUsed);
                    }
                }
                // Check if budget is at WARNING threshold (e.g. 80%)
                else if (percentageUsed >= alertThreshold) {
                    if (!notificationRepository.existsActiveNotification(
                            userId, budgetId, "BUDGET", "BUDGET_WARNING")) {
                        createBudgetWarning(userId, budgetId, name, percentageUsed);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to check budget alerts: " + e.getMessage());
        }
    }

    // Creates a BUDGET_WARNING notification.
    @Override
    public Notification createBudgetWarning(int userId, int budgetId,
                                            String budgetName, double percentageUsed) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle("Budget Alert: " + budgetName);
        notification.setMessage("You've used " +
                Math.round(percentageUsed) + "% of your " + budgetName +
                " budget. Consider reducing spending in this category.");
        notification.setType("BUDGET_WARNING");
        notification.setPriority("HIGH");
        notification.setReferenceId(budgetId);
        notification.setReferenceType("BUDGET");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    // Creates a BUDGET_EXCEEDED notification.
    @Override
    public Notification createBudgetExceeded(int userId, int budgetId,
                                             String budgetName, double percentageUsed) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle("Budget Exceeded: " + budgetName);
        notification.setMessage("You've exceeded your " + budgetName +
                " budget! Currently at " + Math.round(percentageUsed) +
                "%. Review your expenses immediately.");
        notification.setType("BUDGET_EXCEEDED");
        notification.setPriority("CRITICAL");
        notification.setReferenceId(budgetId);
        notification.setReferenceType("BUDGET");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    private String buildAuthorizationHeader() {
        return "Bearer " + generateServiceToken();
    }

    private String generateServiceToken() {
        return Jwts.builder()
                .setSubject("notification-service@internal")
                .claim("userId", 0)
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60 * 60 * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
