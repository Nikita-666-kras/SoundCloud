package com.example.soundcloud.controller;

import com.example.soundcloud.dto.UserDtos;
import com.example.soundcloud.model.FriendRequest;
import com.example.soundcloud.model.User;
import com.example.soundcloud.service.FriendService;
import com.example.soundcloud.service.UserService;
import com.example.soundcloud.config.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/friends")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class FriendController {

    private final FriendService friendService;
    private final UserService userService;

    public FriendController(FriendService friendService, UserService userService) {
        this.friendService = friendService;
        this.userService = userService;
    }

    private Optional<User> findUser(UUID id) {
        return userService.findById(id);
    }

    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(@RequestParam("toId") UUID toId) {
        User from = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        Optional<User> toOpt = findUser(toId);
        if (toOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }
        try {
            friendService.sendRequest(from, toOpt.get());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(CONFLICT).body("Friend request already sent");
        }
    }

    public record FriendSummary(UUID id, String username, String bio, String avatarUrl) {
    }

    public record FriendRequestSummary(UUID id, FriendSummary user, java.time.LocalDateTime createdAt) {
    }

    @GetMapping("/me")
    public ResponseEntity<List<FriendSummary>> myFriends() {
        User me = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        return ResponseEntity.ok(friendService.acceptedFor(me).stream()
                .map(fr -> {
                    User other = fr.getRequester().getId().equals(me.getId())
                            ? fr.getReceiver()
                            : fr.getRequester();
                    return new FriendSummary(
                            other.getId(),
                            other.getUsername(),
                            other.getBio(),
                            other.getAvatarUrl()
                    );
                })
                .toList());
    }

    @GetMapping("/me/incoming")
    public ResponseEntity<List<FriendRequestSummary>> myIncoming() {
        User me = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        List<FriendRequestSummary> list = friendService.incomingPending(me).stream()
                .map(fr -> new FriendRequestSummary(
                        fr.getId(),
                        new FriendSummary(
                                fr.getRequester().getId(),
                                fr.getRequester().getUsername(),
                                fr.getRequester().getBio(),
                                fr.getRequester().getAvatarUrl()
                        ),
                        fr.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/me/outgoing")
    public ResponseEntity<List<FriendRequestSummary>> myOutgoing() {
        User me = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        List<FriendRequestSummary> list = friendService.outgoingPending(me).stream()
                .map(fr -> new FriendRequestSummary(
                        fr.getId(),
                        new FriendSummary(
                                fr.getReceiver().getId(),
                                fr.getReceiver().getUsername(),
                                fr.getReceiver().getBio(),
                                fr.getReceiver().getAvatarUrl()
                        ),
                        fr.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<FriendSummary>> friends(@PathVariable UUID userId) {
        User me = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        if (!me.getId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN);
        }
        List<FriendSummary> list = friendService.acceptedFor(me).stream()
                .map(fr -> {
                    User other = fr.getRequester().getId().equals(me.getId())
                            ? fr.getReceiver()
                            : fr.getRequester();
                    return new FriendSummary(
                            other.getId(),
                            other.getUsername(),
                            other.getBio(),
                            other.getAvatarUrl()
                    );
                })
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/search")
    public ResponseEntity<List<FriendSummary>> searchByUsername(@RequestParam("q") String query) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        List<User> users = userService.findByUsernamePart(query.trim());
        List<FriendSummary> result = users.stream()
                .map(u -> new FriendSummary(
                        u.getId(),
                        u.getUsername(),
                        u.getBio(),
                        u.getAvatarUrl()
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{userId}/incoming")
    public ResponseEntity<List<FriendRequestSummary>> incoming(@PathVariable UUID userId) {
        User me = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        if (!me.getId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN);
        }
        List<FriendRequestSummary> list = friendService.incomingPending(me).stream()
                .map(fr -> new FriendRequestSummary(
                        fr.getId(),
                        new FriendSummary(
                                fr.getRequester().getId(),
                                fr.getRequester().getUsername(),
                                fr.getRequester().getBio(),
                                fr.getRequester().getAvatarUrl()
                        ),
                        fr.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{userId}/outgoing")
    public ResponseEntity<List<FriendRequestSummary>> outgoing(@PathVariable UUID userId) {
        User me = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        if (!me.getId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN);
        }
        List<FriendRequestSummary> list = friendService.outgoingPending(me).stream()
                .map(fr -> new FriendRequestSummary(
                        fr.getId(),
                        new FriendSummary(
                                fr.getReceiver().getId(),
                                fr.getReceiver().getUsername(),
                                fr.getReceiver().getBio(),
                                fr.getReceiver().getAvatarUrl()
                        ),
                        fr.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/me/{friendId}")
    public ResponseEntity<?> removeFriend(@PathVariable UUID friendId) {
        User me = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        try {
            friendService.removeFriend(me, friendId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Not friends");
        }
    }

    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable UUID id) {
        User me = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        friendService.respond(id, me, true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable UUID id) {
        User me = SecurityUtils.getCurrentUser(userService).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        friendService.respond(id, me, false);
        return ResponseEntity.ok().build();
    }
}

