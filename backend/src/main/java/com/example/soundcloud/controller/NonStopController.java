package com.example.soundcloud.controller;

import com.example.soundcloud.config.SecurityUtils;
import com.example.soundcloud.dto.NonStopDtos;
import com.example.soundcloud.model.User;
import com.example.soundcloud.service.NonStopService;
import com.example.soundcloud.service.PromotionService;
import com.example.soundcloud.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nonstop")
public class NonStopController {

    private final NonStopService nonStopService;
    private final PromotionService promotionService;
    private final UserService userService;

    public NonStopController(NonStopService nonStopService,
                             PromotionService promotionService,
                             UserService userService) {
        this.nonStopService = nonStopService;
        this.promotionService = promotionService;
        this.userService = userService;
    }

    @GetMapping("/playlist")
    public List<NonStopDtos.NonStopTrackSlot> playlist(
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String exclude) {
        User userOrNull = SecurityUtils.getCurrentUser(userService).orElse(null);
        return nonStopService.buildPlaylist(size, parseExclude(exclude), userOrNull);
    }

    @PostMapping("/feedback")
    public void feedback(@RequestBody NonStopDtos.NonStopFeedbackRequest req) {
        SecurityUtils.getCurrentUser(userService)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (req.trackId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "trackId обязателен");
        }
        promotionService.recordFeedback(req.campaignId(), req.trackId(), req.event());
    }

    private static List<UUID> parseExclude(String exclude) {
        if (exclude == null || exclude.isBlank()) {
            return List.of();
        }
        List<UUID> out = new ArrayList<>();
        for (String part : exclude.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            try {
                out.add(UUID.fromString(p));
            } catch (IllegalArgumentException ignored) {
                // пропускаем битые сегменты
            }
        }
        return out;
    }
}
