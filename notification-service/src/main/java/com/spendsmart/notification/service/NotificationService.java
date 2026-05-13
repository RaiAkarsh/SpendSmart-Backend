package com.spendsmart.notification.service;

import com.spendsmart.notification.entity.Notification;

import java.util.List;

public interface NotificationService {

    // Create a new notification for a user
    Notification createNotification(Notification notification);

    // Get a single notification by ID
    Notification getById(int notificationId);

    // Get all notifications for a user (newest first)
    List<Notification> getByUser(int userId);

    // Get only unread notifications
    List<Notification> getUnreadByUser(int userId);

    // Get notifications filtered by type
    List<Notification> getByUserAndType(int userId, String type);

    // Get notifications filtered by priority
    List<Notification> getByUserAndPriority(int userId, String priority);

    // Get count of unread notifications (for badge icon)
    int getUnreadCount(int userId);

    // Mark a single notification as read
    Notification markAsRead(int notificationId);

    // Mark all notifications for a user as read
    void markAllAsRead(int userId);

    // Delete a single notification
    void deleteNotification(int notificationId);

    // Delete all read notifications for a user (cleanup)
    void deleteReadNotifications(int userId);

    // Check budgets and generate alerts (called by scheduler)
    void checkBudgetAlerts();

    // Generate a budget warning notification
    Notification createBudgetWarning(int userId, int budgetId,
                                     String budgetName, double percentageUsed);

    // Generate a budget exceeded notification
    Notification createBudgetExceeded(int userId, int budgetId,
                                      String budgetName, double percentageUsed);
}