package com.example.soundcloud.controller;

import com.example.soundcloud.dto.SupportDtos;
import com.example.soundcloud.model.User;
import com.example.soundcloud.service.SupportService;
import com.example.soundcloud.service.UserService;
import com.example.soundcloud.config.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportService supportService;
    private final UserService userService;

    public SupportController(SupportService supportService, UserService userService) {
        this.supportService = supportService;
        this.userService = userService;
    }

    @GetMapping("/messages")
    public ResponseEntity<List<SupportDtos.SupportMessageResponse>> list() {
        User user = SecurityUtils.getCurrentUser(userService)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        return ResponseEntity.ok(supportService.getThread(user));
    }

    @PostMapping("/messages")
    public ResponseEntity<SupportDtos.SupportMessageResponse> send(@RequestBody SupportDtos.SendSupportMessageRequest request) {
        User user = SecurityUtils.getCurrentUser(userService)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        try {
            return ResponseEntity.ok(supportService.addUserMessage(user, request.text()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
