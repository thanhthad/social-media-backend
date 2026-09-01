package media.social.modules.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.notification.service.NotificationService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notification", description = "Notification APIs")
public class NotificationController {

    private final NotificationService notificationService;

    // ================= GET MY NOTIFICATIONS =================
    @GetMapping
    @Operation(summary = "Get my notifications")
    @RateLimit(
            name = "NOTIFICATION_GET_LIST",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getMyNotifications(
            Pageable pageable
    ) {

        return ResponseData.success(
                notificationService.getMyNotifications(pageable),
                "Get notifications successfully",
                HttpStatus.OK
        );
    }

    // ================= MARK AS READ =================
    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    @RateLimit(
            name = "NOTIFICATION_MARK_READ",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> markAsRead(
            @PathVariable Long notificationId
    ) {

        notificationService.markAsRead(notificationId);

        return ResponseData.success(
                null,
                "Notification marked as read",
                HttpStatus.OK
        );
    }

    // ================= MARK ALL AS READ =================
    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    @RateLimit(
            name = "NOTIFICATION_MARK_ALL_READ",
            limit = 30,
            windowSeconds = 60
    )
    public ResponseEntity<?> markAllAsRead() {

        notificationService.markAllAsRead();

        return ResponseData.success(
                null,
                "All notifications marked as read",
                HttpStatus.OK
        );
    }

    // ================= COUNT UNREAD =================
    @GetMapping("/unread-count")
    @Operation(summary = "Count unread notifications")
    @RateLimit(
            name = "NOTIFICATION_UNREAD_COUNT",
            limit = 600,
            windowSeconds = 60
    )
    public ResponseEntity<?> countUnread() {

        return ResponseData.success(
                notificationService.countUnread(),
                "Count unread notifications successfully",
                HttpStatus.OK
        );
    }

    // ================= DELETE NOTIFICATION =================
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete / dismiss a notification")
    @RateLimit(
            name = "NOTIFICATION_DELETE",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> deleteNotification(
            @PathVariable Long notificationId
    ) {
        notificationService.deleteById(notificationId);

        return ResponseData.success(
                null,
                "Notification deleted successfully",
                HttpStatus.OK
        );
    }
}