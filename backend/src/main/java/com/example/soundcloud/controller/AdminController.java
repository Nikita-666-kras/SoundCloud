package com.example.soundcloud.controller;

import com.example.soundcloud.model.TrackReport;
import com.example.soundcloud.model.User;
import com.example.soundcloud.dto.SupportDtos;
import com.example.soundcloud.service.SupportService;
import com.example.soundcloud.service.TrackReportService;
import com.example.soundcloud.service.UserService;
import com.example.soundcloud.service.TrackService;
import com.example.soundcloud.repository.TrackRepository;
import com.example.soundcloud.config.SecurityUtils;
import com.example.soundcloud.dto.TrackDtos;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final TrackReportService reportService;
    private final TrackRepository trackRepository;
    private final TrackService trackService;
    private final SupportService supportService;

    public AdminController(UserService userService,
                           TrackReportService reportService,
                           TrackRepository trackRepository,
                           TrackService trackService,
                           SupportService supportService) {
        this.userService = userService;
        this.reportService = reportService;
        this.trackRepository = trackRepository;
        this.trackService = trackService;
        this.supportService = supportService;
    }

    private Optional<User> requireAdmin() {
        return SecurityUtils.getCurrentUser(userService)
                .filter(User::isAdmin);
    }

    public record TrackReportResponse(
            UUID id,
            UUID trackId,
            String trackTitle,
            UUID reporterId,
            String reporterUsername,
            String reason,
            String details,
            java.time.LocalDateTime createdAt,
            boolean resolved
    ) {
    }

    @GetMapping("/reports")
    public ResponseEntity<List<TrackReportResponse>> listReports() {
        Optional<User> adminOpt = requireAdmin();
        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }
        List<TrackReportResponse> list = reportService.listOpen().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    private TrackReportResponse toResponse(TrackReport r) {
        return new TrackReportResponse(
                r.getId(),
                r.getTrack().getId(),
                r.getTrack().getTitle(),
                r.getReporter().getId(),
                r.getReporter().getUsername(),
                r.getReason(),
                r.getDetails(),
                r.getCreatedAt(),
                r.isResolved()
        );
    }

    @PostMapping("/reports/{id}/resolve")
    public ResponseEntity<?> resolveReport(@PathVariable UUID id) {
        Optional<User> adminOpt = requireAdmin();
        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }
        reportService.resolve(id, adminOpt.get());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/support/threads")
    public ResponseEntity<List<SupportDtos.AdminSupportThreadSummary>> supportThreads() {
        if (requireAdmin().isEmpty()) {
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }
        return ResponseEntity.ok(supportService.listThreadsForAdmin());
    }

    @GetMapping("/support/messages")
    public ResponseEntity<List<SupportDtos.SupportMessageResponse>> supportThreadMessages(@RequestParam("userId") UUID userId) {
        if (requireAdmin().isEmpty()) {
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }
        User target = userService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Пользователь не найден"));
        return ResponseEntity.ok(supportService.getThreadReadOnly(target));
    }

    @PostMapping("/support/reply")
    public ResponseEntity<SupportDtos.SupportMessageResponse> supportReply(@RequestBody SupportDtos.AdminSupportReplyRequest request) {
        Optional<User> adminOpt = requireAdmin();
        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }
        if (request.userId() == null || request.text() == null || request.text().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId и text обязательны");
        }
        User target = userService.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        try {
            return ResponseEntity.ok(supportService.addStaffMessage(target, request.text()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}

