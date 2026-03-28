package com.example.soundcloud.controller;

import com.example.soundcloud.dto.TrackDtos;
import com.example.soundcloud.dto.UserDtos;
import com.example.soundcloud.model.TrackLike;
import com.example.soundcloud.model.User;
import com.example.soundcloud.model.UserFollow;
import com.example.soundcloud.repository.TrackLikeRepository;
import com.example.soundcloud.repository.UserFollowRepository;
import com.example.soundcloud.service.FileStorageService;
import com.example.soundcloud.service.TrackService;
import com.example.soundcloud.service.UserService;
import com.example.soundcloud.config.SecurityUtils;
import com.example.soundcloud.service.FileValidationService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserController {

    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;
    private final TrackLikeRepository likeRepository;
    private final TrackService trackService;
    private final UserFollowRepository followRepository;

    public UserController(UserService userService,
                          FileStorageService fileStorageService,
                          FileValidationService fileValidationService,
                          TrackLikeRepository likeRepository,
                          TrackService trackService,
                          UserFollowRepository followRepository) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.fileValidationService = fileValidationService;
        this.likeRepository = likeRepository;
        this.trackService = trackService;
        this.followRepository = followRepository;
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDtos.ArtistSummary>> search(@RequestParam("q") String q) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        List<UserDtos.ArtistSummary> list = userService.findByUsernamePart(q.trim()).stream()
                .filter(u -> !u.isPrivateAccount())
                .map(u -> new UserDtos.ArtistSummary(u.getId(), u.getUsername(), u.getBio(), u.getAvatarUrl(), trackService.getTotalPlayCountForOwner(u)))
                .limit(20)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDtos.UserResponse> getUser(@PathVariable UUID id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(userService.toResponse(user, trackService.getTotalPlayCountForOwner(user))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDtos.UserResponse> updateProfile(@PathVariable UUID id,
                                                               @RequestBody UserDtos.UpdateProfileRequest request) {
        User currentUser = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        if (!currentUser.getId().equals(id)) {
            throw new ResponseStatusException(FORBIDDEN);
        }
        User updated = userService.updateProfile(id, request);
        return ResponseEntity.ok(userService.toResponse(updated, trackService.getTotalPlayCountForOwner(updated)));
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<?> uploadAvatar(@PathVariable UUID id, @RequestParam("file") MultipartFile file) throws Exception {
        User currentUser = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        if (!currentUser.getId().equals(id)) {
            throw new ResponseStatusException(FORBIDDEN);
        }
        User user = userService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            fileValidationService.validateImage(file);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        String avatarObject = fileStorageService.storeImage(file);
        user.setAvatarUrl(avatarObject);
        userService.save(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<Resource> getAvatar(@PathVariable UUID id) throws Exception {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        if (user.getAvatarUrl() == null) {
            return ResponseEntity.notFound().build();
        }
        InputStreamResource resource = fileStorageService.getImage(user.getAvatarUrl());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .body(resource);
    }

    @GetMapping("/{id}/favorites")
    public ResponseEntity<java.util.List<TrackDtos.TrackListItem>> favorites(@PathVariable UUID id) {
        return userService.findById(id)
                .map(user -> {
                    java.util.List<TrackLike> likes = likeRepository.findByUserOrderByCreatedAtDesc(user);
                    java.util.List<TrackDtos.TrackListItem> list = likes.stream()
                            .map(TrackLike::getTrack)
                            .map(t -> trackService.toListItem(t, trackService.countLikes(t)))
                            .toList();
                    return ResponseEntity.ok(list);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/tracks")
    public ResponseEntity<java.util.List<TrackDtos.TrackListItem>> userTracks(@PathVariable UUID id) {
        return userService.findById(id)
                .map(user -> {
                    java.util.List<TrackDtos.TrackListItem> list = trackService.listByOwner(user).stream()
                            .map(t -> trackService.toListItem(t, trackService.countLikes(t)))
                            .toList();
                    return ResponseEntity.ok(list);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/albums/{album}")
    public ResponseEntity<java.util.List<TrackDtos.TrackListItem>> userAlbumTracks(@PathVariable UUID id,
                                                                                   @PathVariable String album) {
        return userService.findById(id)
                .map(user -> {
                    java.util.List<TrackDtos.TrackListItem> list = trackService
                            .listByOwnerAndAlbum(user, album)
                            .stream()
                            .map(t -> trackService.toListItem(t, trackService.countLikes(t)))
                            .toList();
                    return ResponseEntity.ok(list);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/albums/{album}")
    public ResponseEntity<?> deleteAlbum(@PathVariable UUID id, @PathVariable String album) {
        User currentUser = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        if (!currentUser.getId().equals(id)) {
            throw new ResponseStatusException(FORBIDDEN);
        }
        User owner = userService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String albumName = album != null ? album.trim() : "";
        if (albumName.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        trackService.deleteAlbum(owner, albumName, currentUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMe() {
        User currentUser = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        userService.deleteUser(currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<?> follow(@PathVariable UUID id) {
        User follower = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        if (id.equals(follower.getId())) {
            return ResponseEntity.badRequest().body("Cannot follow yourself");
        }
        Optional<User> targetOpt = userService.findById(id);
        if (targetOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }
        User target = targetOpt.get();
        followRepository.findByFollowerAndTarget(follower, target)
                .orElseGet(() -> {
                    UserFollow f = new UserFollow();
                    f.setFollower(follower);
                    f.setTarget(target);
                    return followRepository.save(f);
                });
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/follow")
    public ResponseEntity<?> unfollow(@PathVariable UUID id) {
        User follower = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        Optional<User> targetOpt = userService.findById(id);
        if (targetOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }
        followRepository.findByFollowerAndTarget(follower, targetOpt.get())
                .ifPresent(followRepository::delete);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/favorite-artists")
    public ResponseEntity<java.util.List<UserDtos.ArtistSummary>> favoriteArtists(@PathVariable UUID id) {
        return userService.findById(id)
                .map(user -> {
                    java.util.List<UserFollow> follows = followRepository.findByFollower(user);
                    java.util.List<UserDtos.ArtistSummary> list = follows.stream()
                            .map(UserFollow::getTarget)
                            .map(u -> new UserDtos.ArtistSummary(
                                    u.getId(),
                                    u.getUsername(),
                                    u.getBio(),
                                    u.getAvatarUrl(),
                                    trackService.getTotalPlayCountForOwner(u)
                            ))
                            .toList();
                    return ResponseEntity.ok(list);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

