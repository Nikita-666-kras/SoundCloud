package com.example.soundcloud.controller;

import com.example.soundcloud.dto.PlaylistDtos;
import com.example.soundcloud.model.Playlist;
import com.example.soundcloud.model.User;
import com.example.soundcloud.repository.PlaylistRepository;
import com.example.soundcloud.service.PlaylistService;
import com.example.soundcloud.service.UserService;
import com.example.soundcloud.config.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final PlaylistRepository playlistRepository;
    private final UserService userService;

    public PlaylistController(PlaylistService playlistService,
                              PlaylistRepository playlistRepository,
                              UserService userService) {
        this.playlistService = playlistService;
        this.playlistRepository = playlistRepository;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<PlaylistDtos.PlaylistSummary>> list(
            @RequestParam(value = "q", required = false) String q) {
        User owner = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        List<PlaylistDtos.PlaylistSummary> list = playlistService.listByOwner(owner).stream()
                .filter(p -> q == null || q.isBlank() ||
                        (p.getName() != null && p.getName().toLowerCase().contains(q.trim().toLowerCase())))
                .map(p -> playlistService.toSummary(p, playlistService.getTrackCount(p)))
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<PlaylistDtos.PlaylistSummary> create(@RequestParam("name") String name,
                                                               @RequestParam(value = "description", required = false) String description) {
        User owner = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Playlist playlist = playlistService.create(owner, name.trim(), description);
        PlaylistDtos.PlaylistSummary summary = playlistService.toSummary(playlist, 0);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaylistDtos.PlaylistDetails> get(@PathVariable UUID id) {
        User currentUser = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        return playlistRepository.findById(id)
                .filter(p -> p.getOwner().getId().equals(currentUser.getId()))
                .map(p -> playlistService.toDetails(p, currentUser))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/tracks")
    public ResponseEntity<?> addTrack(@PathVariable UUID id, @RequestParam("trackId") UUID trackId) {
        User currentUser = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        Optional<Playlist> playlistOpt = playlistRepository.findById(id);
        if (playlistOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!playlistOpt.get().getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(FORBIDDEN);
        }
        playlistService.addTrack(playlistOpt.get(), trackId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        User currentUser = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        Playlist playlist = playlistRepository.findById(id).orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if (!playlist.getOwner().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(FORBIDDEN);
        }
        playlistService.deletePlaylist(playlist, currentUser);
        return ResponseEntity.noContent().build();
    }
}

