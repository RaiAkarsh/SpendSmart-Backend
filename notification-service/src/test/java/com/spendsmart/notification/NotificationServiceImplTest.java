package com.spendsmart.notification;

import com.spendsmart.notification.client.BudgetClient;
import com.spendsmart.notification.entity.Notification;
import com.spendsmart.notification.repository.NotificationRepository;
import com.spendsmart.notification.serviceimpl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Unit Tests")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private BudgetClient budgetClient;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification unreadNotification;
    private Notification readNotification;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "budgetServiceUrl", "http://localhost:8085");
        ReflectionTestUtils.setField(notificationService, "secret", "SpendSmartSecretKey2026_Standard32Bytes!");

        unreadNotification = new Notification();
        unreadNotification.setNotificationId(1);
        unreadNotification.setUserId(1);
        unreadNotification.setTitle("Food budget at 82%");
        unreadNotification.setMessage("You have spent Ãƒ ¢Ã¢â‚¬Å¡Ã‚ ¹4100 of your Ãƒ ¢Ã¢â‚¬Å¡Ã‚ ¹5000 Food budget.");
        unreadNotification.setType("BUDGET_ALERT");
        unreadNotification.setPriority("HIGH");
        unreadNotification.setRead(false);
        unreadNotification.setReferenceId(1);
        unreadNotification.setReferenceType("BUDGET");

        readNotification = new Notification();
        readNotification.setNotificationId(2);
        readNotification.setUserId(1);
        readNotification.setTitle("Welcome to SpendSmart!");
        readNotification.setMessage("Your account is ready.");
        readNotification.setType("SYSTEM");
        readNotification.setPriority("LOW");
        readNotification.setRead(true);
    }


    @Test
    @DisplayName("createNotification: should set isRead=false on creation")
    void createNotification_shouldSetIsReadFalse() {
        when(notificationRepository.save(any())).thenReturn(unreadNotification);

        Notification input = new Notification();
        input.setUserId(1);
        input.setTitle("Test alert");
        input.setMessage("Test message");
        input.setType("SYSTEM");
        input.setPriority("MEDIUM");
        // Intentionally set read=true to verify service overrides it
        input.setRead(true);

        notificationService.createNotification(input);

        verify(notificationRepository).save(argThat(n -> !n.isRead()));
    }


    @Test
    @DisplayName("createBudgetWarning: should create HIGH priority BUDGET_WARNING notification")
    void createBudgetWarning_shouldCreateHighPriorityAlert() {
        when(notificationRepository.save(any())).thenReturn(unreadNotification);

        Notification result = notificationService.createBudgetWarning(
                1, 1, "Food Budget", 82.6);

        verify(notificationRepository).save(argThat(n ->
                "BUDGET_WARNING".equals(n.getType()) &&
                "HIGH".equals(n.getPriority()) &&
                !n.isRead() &&
                n.getUserId() == 1));
    }


    @Test
    @DisplayName("createBudgetExceeded: should create BUDGET_EXCEEDED notification")
    void createBudgetExceeded_shouldCreateExceededNotification() {
        when(notificationRepository.save(any())).thenReturn(unreadNotification);

        Notification result = notificationService.createBudgetExceeded(
                1, 1, "Food Budget", 102.6);

        verify(notificationRepository).save(argThat(n ->
                "BUDGET_EXCEEDED".equals(n.getType()) &&
                !n.isRead()));
    }


    @Test
    @DisplayName("getByUser: should return all notifications newest first")
    void getByUser_shouldReturnAllNotifications() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1))
                .thenReturn(Arrays.asList(unreadNotification, readNotification));

        List<Notification> result = notificationService.getByUser(1);

        assertEquals(2, result.size());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(1);
    }


    @Test
    @DisplayName("getUnreadByUser: should return only unread notifications")
    void getUnreadByUser_shouldReturnOnlyUnread() {
        when(notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(1, false))
                .thenReturn(Arrays.asList(unreadNotification));

        List<Notification> result = notificationService.getUnreadByUser(1);

        assertEquals(1, result.size());
        assertFalse(result.get(0).isRead());
    }


    @Test
    @DisplayName("getUnreadCount: should return correct count for bell badge")
    void getUnreadCount_shouldReturnCount() {
        when(notificationRepository.countByUserIdAndIsRead(1, false)).thenReturn(3);
        assertEquals(3, notificationService.getUnreadCount(1));
    }

    @Test
    @DisplayName("getUnreadCount: should return 0 when all read")
    void getUnreadCount_shouldReturnZero_whenAllRead() {
        when(notificationRepository.countByUserIdAndIsRead(1, false)).thenReturn(0);
        assertEquals(0, notificationService.getUnreadCount(1));
    }


    @Test
    @DisplayName("markAsRead: should set isRead=true and set readAt timestamp")
    void markAsRead_shouldSetIsReadTrueAndReadAt() {
        when(notificationRepository.findByNotificationId(1))
                .thenReturn(Optional.of(unreadNotification));
        when(notificationRepository.save(any())).thenReturn(unreadNotification);

        Notification result = notificationService.markAsRead(1);

        verify(notificationRepository).save(argThat(n ->
                n.isRead() && n.getReadAt() != null));
    }

    @Test
    @DisplayName("markAsRead: should throw when notification not found")
    void markAsRead_shouldThrow_whenNotFound() {
        when(notificationRepository.findByNotificationId(999)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> notificationService.markAsRead(999));
    }


    @Test
    @DisplayName("markAllAsRead: should mark all unread notifications as read")
    void markAllAsRead_shouldMarkAllUnread() {
        when(notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(1, false))
                .thenReturn(Arrays.asList(unreadNotification));
        when(notificationRepository.saveAll(any())).thenReturn(Arrays.asList(unreadNotification));

        notificationService.markAllAsRead(1);

        verify(notificationRepository, times(1))
                .saveAll(argThat(list -> {
                    for (Notification n : list) {
                        if (!n.isRead()) return false;
                    }
                    return true;
                }));
    }


    @Test
    @DisplayName("deleteNotification: should delete when found")
    void deleteNotification_shouldDelete_whenFound() {
        when(notificationRepository.findByNotificationId(1))
                .thenReturn(Optional.of(unreadNotification));

        assertDoesNotThrow(() -> notificationService.deleteNotification(1));
        verify(notificationRepository).deleteByNotificationId(1);
    }

    @Test
    @DisplayName("deleteNotification: should throw when not found")
    void deleteNotification_shouldThrow_whenNotFound() {
        when(notificationRepository.findByNotificationId(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> notificationService.deleteNotification(99));
        verify(notificationRepository, never()).deleteByNotificationId(anyInt());
    }


    @Test
    @DisplayName("deleteReadNotifications: should delete all read notifications for user")
    void deleteReadNotifications_shouldDeleteAllRead() {
        notificationService.deleteReadNotifications(1);
        verify(notificationRepository).deleteAll(anyList());
    }
}
