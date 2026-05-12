package com.spendsmart.notification.controller;

import com.spendsmart.notification.entity.Notification;
import com.spendsmart.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "User notifications, unread state, and budget alert checks")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Operation(summary = "Create a notification manually")
    @PostMapping
    public ResponseEntity<?> createNotification(@Valid @RequestBody Notification notification) {
        try {
            Notification saved = notificationService.createNotification(notification);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get notifications by user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUser(@PathVariable int userId) {
        List<Notification> notifications = notificationService.getByUser(userId);
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Get notification by id")
    @GetMapping("/{notificationId}")
    public ResponseEntity<?> getById(@PathVariable int notificationId) {
        try {
            return ResponseEntity.ok(notificationService.getById(notificationId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Get unread notifications by user")
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<?> getUnread(@PathVariable int userId) {
        List<Notification> notifications = notificationService.getUnreadByUser(userId);
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Get unread notification count")
    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<?> getUnreadCount(@PathVariable int userId) {
        int count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "unreadCount", count));
    }

    @Operation(summary = "Get notifications by type")
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<?> getByType(@PathVariable int userId,
                                        @PathVariable String type) {
        List<Notification> notifications =
            notificationService.getByUserAndType(userId, type);
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Get notifications by priority")
    @GetMapping("/user/{userId}/priority/{priority}")
    public ResponseEntity<?> getByPriority(@PathVariable int userId,
                                            @PathVariable String priority) {
        List<Notification> notifications =
            notificationService.getByUserAndPriority(userId, priority);
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Mark notification as read")
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable int notificationId) {
        try {
            Notification updated = notificationService.markAsRead(notificationId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Mark all notifications as read")
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<?> markAllAsRead(@PathVariable int userId) {
        try {
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(success("All notifications marked as read"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Delete a notification")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(@PathVariable int notificationId) {
        try {
            notificationService.deleteNotification(notificationId);
            return ResponseEntity.ok(success("Notification deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Delete read notifications")
    @DeleteMapping("/user/{userId}/read")
    public ResponseEntity<?> deleteReadNotifications(@PathVariable int userId) {
        try {
            notificationService.deleteReadNotifications(userId);
            return ResponseEntity.ok(success("Read notifications deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @Operation(summary = "Manually check budget alerts")
    @PostMapping("/check-budgets")
    public ResponseEntity<?> triggerBudgetCheck() {
        try {
            notificationService.checkBudgetAlerts();
            return ResponseEntity.ok(success("Budget alerts checked successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("error", message);
        return map;
    }

    private Map<String, String> success(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        return map;
    }
}
