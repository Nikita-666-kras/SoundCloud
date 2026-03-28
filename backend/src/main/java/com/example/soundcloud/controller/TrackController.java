package com.example.soundcloud.controller;

import com.example.soundcloud.dto.TrackDtos;
import com.example.soundcloud.model.Track;
import com.example.soundcloud.model.User;
import com.example.soundcloud.model.TrackReport;
import com.example.soundcloud.service.TrackReportService;
import com.example.soundcloud.service.NotificationService;
import com.example.soundcloud.repository.TrackRepository;
import com.example.soundcloud.service.FileStorageService;
import com.example.soundcloud.service.TrackCommentService;
import com.example.soundcloud.service.TrackService;
import com.example.soundcloud.service.UserService;
import com.example.soundcloud.service.PlaylistService;
import com.example.soundcloud.service.FileValidationService;
import com.example.soundcloud.config.SecurityUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/tracks")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class TrackController {

    private final TrackService trackService;
    private final UserService userService;
    private final TrackRepository trackRepository;
    private final FileStorageService fileStorageService;
    private final TrackCommentService commentService;
    private final TrackReportService reportService;
    private final NotificationService notificationService;
    private final PlaylistService playlistService;
    private final FileValidationService fileValidationService;

    private static final Logger log = LoggerFactory.getLogger(TrackController.class);

    public TrackController(TrackService trackService,
                           UserService userService,
                           TrackRepository trackRepository,
                           FileStorageService fileStorageService,
                           TrackCommentService commentService,
                           TrackReportService reportService,
                           NotificationService notificationService,
                           PlaylistService playlistService,
                           FileValidationService fileValidationService) {
        this.trackService = trackService;
        this.userService = userService;
        this.trackRepository = trackRepository;
        this.fileStorageService = fileStorageService;
        this.commentService = commentService;
        this.reportService = reportService;
        this.notificationService = notificationService;
        this.playlistService = playlistService;
        this.fileValidationService = fileValidationService;
    }

    @GetMapping
    public List<TrackDtos.TrackListItem> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "popular") String sort
    ) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 20;
        }
        List<Track> pageContent = "recent".equalsIgnoreCase(sort)
                ? trackService.listPageRecent(page, size)
                : trackService.listPage(page, size);
        return pageContent.stream()
                .map(t -> trackService.toListItem(t, trackService.countLikes(t)))
                .toList();
    }

    @GetMapping("/search")
    public ResponseEntity<List<TrackDtos.TrackListItem>> search(
            @RequestParam("q") String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        List<Track> tracks = trackService.searchTracks(q, page, size);
        List<TrackDtos.TrackListItem> list = tracks.stream()
                .map(t -> trackService.toListItem(t, trackService.countLikes(t)))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/search/albums")
    public ResponseEntity<List<TrackService.AlbumSearchResult>> searchAlbums(@RequestParam("q") String q) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(trackService.searchAlbums(q));
    }

    @GetMapping("/albums")
    public ResponseEntity<List<TrackService.AlbumSearchResult>> listAlbums(
            @RequestParam(name = "sort", defaultValue = "popular") String sort,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return ResponseEntity.ok(trackService.listAlbums(sort, safeLimit));
    }

    @GetMapping("/from-following")
    public ResponseEntity<List<TrackDtos.TrackListItem>> listFromFollowing(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        User user = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
        List<Track> pageContent = trackService.listPageFromFollowing(user, page, size);
        List<TrackDtos.TrackListItem> list = pageContent.stream()
                .map(t -> trackService.toListItem(t, trackService.countLikes(t)))
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("title") String title,
                                    @RequestParam(value = "description", required = false) String description,
                                    @RequestParam(value = "genre", required = false) String genre,
                                    @RequestParam(value = "album", required = false) String album,
                                    @RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "cover", required = false) MultipartFile cover) throws Exception {
        User owner = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        try {
            fileValidationService.validateAudio(file);
            if (cover != null && !cover.isEmpty()) {
                fileValidationService.validateImage(cover);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        Track track = trackService.uploadTrack(owner, title, description, genre, album, file, cover);
        long likes = trackService.countLikes(track);
        return ResponseEntity.ok(trackService.toResponse(track, likes));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> like(@PathVariable UUID id) {
        User user = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        Optional<Track> trackOpt = trackRepository.findById(id);
        if (trackOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Track not found");
        }
        Track track = trackOpt.get();
        trackService.likeTrack(user, track);
        if (!track.getOwner().getId().equals(user.getId())) {
            notificationService.createLikeNotification(track.getOwner(), user, track);
        }
        long likes = trackService.countLikes(track);
        return ResponseEntity.ok(likes);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<TrackDtos.CommentResponse>> comments(@PathVariable UUID id) {
        Optional<Track> trackOpt = trackRepository.findById(id);
        if (trackOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<TrackDtos.CommentResponse> list = commentService.getComments(trackOpt.get())
                .stream()
                .map(commentService::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable UUID id, @RequestParam("text") String text) {
        User user = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        Optional<Track> trackOpt = trackRepository.findById(id);
        if (trackOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Track not found");
        }
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body("Empty text");
        }
        Track track = trackOpt.get();
        var comment = commentService.addComment(track, user, text.trim());
        if (!track.getOwner().getId().equals(user.getId())) {
            notificationService.createCommentNotification(track.getOwner(), user, track);
        }
        return ResponseEntity.ok(commentService.toResponse(comment));
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> stream(@PathVariable UUID id, @RequestHeader HttpHeaders headers) throws Exception {
        Optional<Track> trackOpt = trackRepository.findById(id);
        if (trackOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Track track = trackOpt.get();
        track.setPlayCount(track.getPlayCount() + 1);
        User owner = track.getOwner();
        owner.setTotalPlayCount(owner.getTotalPlayCount() + 1);
        trackRepository.save(track);
        userService.save(owner);

        String objectName = track.getAudioPath();
        long fileSize = fileStorageService.getAudioSize(objectName);

        String rangeHeader = headers.getFirst(HttpHeaders.RANGE);
        if (rangeHeader == null) {
            InputStreamResource resource = fileStorageService.getAudio(objectName);
            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentLength(fileSize)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }

        // Parse Range header, e.g. "bytes=0-1023"
        HttpRange httpRange = HttpRange.parseRanges(rangeHeader).get(0);
        long start = httpRange.getRangeStart(fileSize);
        long end = httpRange.getRangeEnd(fileSize);
        long rangeLength = end - start + 1;

        InputStreamResource resource = fileStorageService.getAudioRange(objectName, start, rangeLength);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                .contentLength(rangeLength)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<Resource> cover(@PathVariable UUID id) throws Exception {
        Optional<Track> trackOpt = trackRepository.findById(id);
        if (trackOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Track track = trackOpt.get();
        if (track.getCoverUrl() == null) {
            return ResponseEntity.notFound().build();
        }
        InputStreamResource resource = fileStorageService.getImage(track.getCoverUrl());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        User currentUser = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        Track track = trackRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!track.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        trackService.deleteTrack(track, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrackDtos.TrackListItem> update(@PathVariable UUID id,
                                                          @RequestBody TrackDtos.TrackUpdateRequest request) {
        User currentUser = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        Optional<Track> trackOpt = trackRepository.findById(id);
        if (trackOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Track track = trackOpt.get();
        if (!track.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Track updated = trackService.updateTrack(track, request);
        long likes = trackService.countLikes(updated);
        TrackDtos.TrackListItem dto = trackService.toListItem(updated, likes);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/cover")
    public ResponseEntity<?> updateCover(@PathVariable UUID id, @RequestParam("file") MultipartFile coverFile) throws Exception {
        User currentUser = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        Optional<Track> trackOpt = trackRepository.findById(id);
        if (trackOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Track not found");
        }
        Track track = trackOpt.get();
        if (!track.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not owner of track");
        }
        try {
            fileValidationService.validateImage(coverFile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        String coverObject = fileStorageService.storeImage(coverFile);
        track.setCoverUrl(coverObject);
        trackRepository.save(track);
        return ResponseEntity.ok().build();
    }

    public record TrackReportRequest(String reason, String details) {
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<?> report(@PathVariable UUID id, @RequestBody TrackReportRequest request) {
        User reporter = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        Optional<Track> trackOpt = trackRepository.findById(id);
        if (trackOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Track not found");
        }
        String reason = request.reason() != null ? request.reason() : "Другое";
        String details = request.details();
        TrackReport r = reportService.createReport(trackOpt.get(), reporter, reason, details);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/repost")
    public ResponseEntity<?> repost(@PathVariable UUID id, @RequestParam("toUserId") UUID toUserId) {
        User from = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        Optional<User> toOpt = userService.findById(toUserId);
        Optional<Track> trackOpt = trackRepository.findById(id);
        if (toOpt.isEmpty() || trackOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User or track not found");
        }
        User to = toOpt.get();
        Track track = trackOpt.get();
        if (from.getId().equals(to.getId())) {
            return ResponseEntity.badRequest().body("Cannot repost to yourself");
        }
        try {
            playlistService.repostTrack(from, to, track.getId());
        } catch (Exception e) {
            log.error("Repost failed: trackId={}, toUserId={}", id, toUserId, e);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body("Repost failed");
        }
        try {
            notificationService.createRepostNotification(to, from, track);
        } catch (Exception e) {
            log.warn("Repost notification failed: trackId={}, toUserId={}", id, toUserId, e);
        }
        return ResponseEntity.ok().build();
    }
}

