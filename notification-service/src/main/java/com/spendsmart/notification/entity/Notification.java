package com.spendsmart.notification.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Schema(description = "User notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private int notificationId;

    @Column(nullable = false)
    @Schema(example = "1")
    private int userId;

    @Column(nullable = false)
    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 200, message = "Title must be 2-200 characters")
    @Schema(example = "Budget Alert: Food")
    private String title;

    @Column(nullable = false, length = 1000)
    @NotBlank(message = "Message is required")
    @Schema(example = "You have used 80% of your Food budget.")
    private String message;

    @Column(nullable = false)
    @NotBlank(message = "Notification type is required")
    @Schema(example = "BUDGET_WARNING", allowableValues = {"BUDGET_WARNING", "BUDGET_EXCEEDED", "RECURRING_REMINDER", "MONTHLY_REPORT", "SYSTEM"})
    private String type;

    @Schema(example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
    private String priority = "MEDIUM";

    @Column(name = "is_read")
    @Schema(example = "false")
    private boolean isRead = false;

    @Schema(example = "3")
    private Integer referenceId;

    @Schema(example = "BUDGET", allowableValues = {"BUDGET", "RECURRING", "EXPENSE", "INCOME"})
    private String referenceType;

    @Schema(example = "2026-05-05T11:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Schema(example = "2026-05-05T12:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime readAt;


    public Notification() {}

    public Notification(int notificationId, int userId, String title, String message,
                        String type, String priority, boolean isRead,
                        Integer referenceId, String referenceType,
                        LocalDateTime createdAt, LocalDateTime readAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.priority = priority;
        this.isRead = isRead;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }


    public int getNotificationId() { return notificationId; }
    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public String getPriority() { return priority; }
    public boolean isRead() { return isRead; }
    public Integer getReferenceId() { return referenceId; }
    public String getReferenceType() { return referenceType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }


    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setType(String type) { this.type = type; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setRead(boolean read) { this.isRead = read; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    @Override
    public String toString() {
        return "Notification{id=" + notificationId + ", userId=" + userId +
               ", title='" + title + "', type='" + type +
               "', isRead=" + isRead + "}";
    }
}
