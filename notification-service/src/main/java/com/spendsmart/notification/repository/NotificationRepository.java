package com.spendsmart.notification.repository;

import com.spendsmart.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // SELECT * FROM notifications WHERE notification_id = ?
    Optional<Notification> findByNotificationId(int notificationId);

    // SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC
    // Gets all notifications for a user (newest first)
    List<Notification> findByUserIdOrderByCreatedAtDesc(int userId);

    // SELECT * FROM notifications WHERE user_id = ? AND is_read = ?
    // Used to get unread notifications only
    List<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(int userId, boolean isRead);

    // SELECT * FROM notifications WHERE user_id = ? AND type = ?
    // Filter by type: BUDGET_WARNING, BUDGET_EXCEEDED, RECURRING_REMINDER, etc.
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(int userId, String type);

    // SELECT * FROM notifications WHERE user_id = ? AND priority = ?
    // Filter by priority: LOW, MEDIUM, HIGH, CRITICAL
    List<Notification> findByUserIdAndPriorityOrderByCreatedAtDesc(int userId, String priority);

    // COUNT(*) FROM notifications WHERE user_id = ? AND is_read = false
    // Used for the badge count on the notification bell icon
    int countByUserIdAndIsRead(int userId, boolean isRead);

    // DELETE FROM notifications WHERE notification_id = ?
    void deleteByNotificationId(int notificationId);

    // Check if a notification already exists for a specific reference
    // Prevents duplicate budget alerts for the same budget in the same check cycle
    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.userId = :userId " +
           "AND n.referenceId = :refId AND n.referenceType = :refType " +
           "AND n.type = :type AND n.isRead = false")
    boolean existsActiveNotification(@Param("userId") int userId,
                                     @Param("refId") int refId,
                                     @Param("refType") String refType,
                                     @Param("type") String type);
}