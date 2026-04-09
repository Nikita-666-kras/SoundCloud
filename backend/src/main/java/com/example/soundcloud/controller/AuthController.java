package com.example.soundcloud.controller;

import com.example.soundcloud.dto.UserDtos;
import com.example.soundcloud.model.User;
import com.example.soundcloud.service.AuthTokenService;
import com.example.soundcloud.service.JwtService;
import com.example.soundcloud.service.TrackService;
import com.example.soundcloud.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthTokenService authTokenService;
    private final JwtService jwtService;
    private final TrackService trackService;

    public AuthController(UserService userService, AuthTokenService authTokenService, JwtService jwtService, TrackService trackService) {
        this.userService = userService;
        this.authTokenService = authTokenService;
        this.jwtService = jwtService;
        this.trackService = trackService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDtos.RegisterRequest request) {
        User user = userService.register(request);
        AuthTokenService.TokenPair tokens = authTokenService.issueTokens(user);
        UserDtos.LoginResponse response = new UserDtos.LoginResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.expiresIn(),
                userService.toResponse(user, trackService.getTotalPlayCountForOwner(user))
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDtos.LoginRequest request) {
        return userService.authenticate(request.email(), request.password())
                .map(user -> {
                    AuthTokenService.TokenPair tokens = authTokenService.issueTokens(user);
                    UserDtos.LoginResponse response = new UserDtos.LoginResponse(
                            tokens.accessToken(),
                            tokens.refreshToken(),
                            tokens.expiresIn(),
                            userService.toResponse(user, trackService.getTotalPlayCountForOwner(user))
                    );
                    return ResponseEntity.<UserDtos.LoginResponse>ok(response);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody UserDtos.RefreshRequest request) {
        return authTokenService.refresh(request.refreshToken())
                .map(tokens -> {
                    UUID userId = jwtService.parseUserId(tokens.accessToken());
                    User user = userId != null ? userService.findById(userId).orElse(null) : null;
                    if (user == null) return ResponseEntity.<UserDtos.LoginResponse>status(HttpStatus.UNAUTHORIZED).build();
                    UserDtos.LoginResponse response = new UserDtos.LoginResponse(
                            tokens.accessToken(),
                            tokens.refreshToken(),
                            tokens.expiresIn(),
                            userService.toResponse(user, trackService.getTotalPlayCountForOwner(user))
                    );
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) UserDtos.RefreshRequest request) {
        if (request != null && request.refreshToken() != null) {
            authTokenService.revokeRefreshToken(request.refreshToken());
        }
        return ResponseEntity.ok().build();
    }
}
