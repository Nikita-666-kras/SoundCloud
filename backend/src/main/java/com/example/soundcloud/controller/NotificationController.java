package com.example.soundcloud.controller;

import com.example.soundcloud.model.Notification;
import com.example.soundcloud.model.User;
import com.example.soundcloud.service.NotificationService;
import com.example.soundcloud.service.UserService;
import com.example.soundcloud.config.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    public record NotificationResponse(
            UUID id,
            String type,
            String message,
            UUID trackId,
            UUID fromUserId,
            java.time.LocalDateTime createdAt,
            boolean read
    ) {
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list() {
        User user = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        List<NotificationResponse> list = notificationService.listFor(user).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> count() {
        User user = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        long count = notificationService.unreadCount(user);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllRead() {
        User user = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        notificationService.markAllRead(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable UUID id) {
        User user = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        notificationService.markRead(user, id);
        return ResponseEntity.ok().build();
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType().name(),
                n.getMessage(),
                n.getTrackId(),
                n.getFromUserId(),
                n.getCreatedAt(),
                n.isRead()
        );
    }
}

