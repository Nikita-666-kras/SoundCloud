package com.example.soundcloud.controller;

import com.example.soundcloud.config.SecurityUtils;
import com.example.soundcloud.dto.NonStopDtos;
import com.example.soundcloud.model.User;
import com.example.soundcloud.service.PromotionService;
import com.example.soundcloud.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;
    private final UserService userService;

    public PromotionController(PromotionService promotionService, UserService userService) {
        this.promotionService = promotionService;
        this.userService = userService;
    }

    @PostMapping
    public NonStopDtos.PromotionMineItem create(@RequestBody NonStopDtos.CreatePromotionRequest req) {
        User user = SecurityUtils.getCurrentUser(userService)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (req.trackId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "trackId обязателен");
        }
        int max = req.maxImpressions() != null ? req.maxImpressions() : 100;
        return promotionService.createCampaign(user, req.trackId(), max);
    }

    @GetMapping("/mine")
    public List<NonStopDtos.PromotionMineItem> mine() {
        User user = SecurityUtils.getCurrentUser(userService)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return promotionService.listMine(user);
    }
}
