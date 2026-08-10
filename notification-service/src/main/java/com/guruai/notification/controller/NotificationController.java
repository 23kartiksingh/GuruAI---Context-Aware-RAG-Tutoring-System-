package com.guruai.notification.controller;

import com.guruai.common.dto.ApiResponse;
import com.guruai.notification.dto.response.NotificationResponse;
import com.guruai.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** All notifications for a user (newest first). */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAll(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getNotifications(userId)));
    }

    /** Unread notifications only. */
    @GetMapping("/{userId}/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnread(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getUnread(userId)));
    }

    /** Mark all notifications as read. */
    @PostMapping("/{userId}/mark-read")
    public ResponseEntity<ApiResponse<String>> markAllRead(@PathVariable UUID userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read"));
    }
}
