package media.social.modults.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.notification.service.NotificationService;
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
    public ResponseEntity<?> countUnread() {

        return ResponseData.success(
                notificationService.countUnread(),
                "Count unread notifications successfully",
                HttpStatus.OK
        );
    }

}