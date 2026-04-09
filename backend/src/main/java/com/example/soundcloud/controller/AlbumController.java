package com.example.soundcloud.controller;

import com.example.soundcloud.config.SecurityUtils;
import com.example.soundcloud.model.User;
import com.example.soundcloud.service.AlbumLikeService;
import com.example.soundcloud.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

    private final AlbumLikeService albumLikeService;
    private final UserService userService;

    public AlbumController(AlbumLikeService albumLikeService, UserService userService) {
        this.albumLikeService = albumLikeService;
        this.userService = userService;
    }

    @GetMapping("/liked")
    public ResponseEntity<List<AlbumLikeService.AlbumSummary>> getLikedAlbums() {
        User user = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        return ResponseEntity.ok(albumLikeService.getLikedAlbums(user));
    }

    @PostMapping("/like")
    public ResponseEntity<?> like(@RequestParam("ownerId") UUID ownerId,
                                  @RequestParam("albumName") String albumName) {
        User user = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        albumLikeService.like(user, ownerId, albumName != null ? albumName.trim() : "");
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/like")
    public ResponseEntity<?> unlike(@RequestParam("ownerId") UUID ownerId,
                                    @RequestParam("albumName") String albumName) {
        User user = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        albumLikeService.unlike(user, ownerId, albumName != null ? albumName : "");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/liked/check")
    public ResponseEntity<Boolean> checkLiked(@RequestParam("ownerId") UUID ownerId,
                                             @RequestParam("albumName") String albumName) {
        User user = SecurityUtils.getCurrentUser(userService).orElse(null);
        return ResponseEntity.ok(albumLikeService.isLiked(user, ownerId, albumName != null ? albumName : ""));
    }
}
