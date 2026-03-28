package com.example.soundcloud.service;

import com.example.soundcloud.dto.UserDtos;
import com.example.soundcloud.model.Playlist;
import com.example.soundcloud.model.Track;
import com.example.soundcloud.model.User;
import com.example.soundcloud.repository.AlbumLikeRepository;
import com.example.soundcloud.repository.FriendRequestRepository;
import com.example.soundcloud.repository.NotificationRepository;
import com.example.soundcloud.repository.PlaylistRepository;
import com.example.soundcloud.repository.PlaylistTrackRepository;
import com.example.soundcloud.repository.RefreshTokenRepository;
import com.example.soundcloud.repository.TrackLikeRepository;
import com.example.soundcloud.repository.UserFollowRepository;
import com.example.soundcloud.repository.UserRepository;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NotificationRepository notificationRepository;
    private final TrackLikeRepository trackLikeRepository;
    private final UserFollowRepository userFollowRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final AlbumLikeRepository albumLikeRepository;
    private final TrackService trackService;
    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final PasswordEncoder passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public UserService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       NotificationRepository notificationRepository,
                       TrackLikeRepository trackLikeRepository,
                       UserFollowRepository userFollowRepository,
                       FriendRequestRepository friendRequestRepository,
                       AlbumLikeRepository albumLikeRepository,
                       TrackService trackService,
                       PlaylistRepository playlistRepository,
                       PlaylistTrackRepository playlistTrackRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.notificationRepository = notificationRepository;
        this.trackLikeRepository = trackLikeRepository;
        this.userFollowRepository = userFollowRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.albumLikeRepository = albumLikeRepository;
        this.trackService = trackService;
        this.playlistRepository = playlistRepository;
        this.playlistTrackRepository = playlistTrackRepository;
    }

    @Transactional
    public User register(UserDtos.RegisterRequest request) {
        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return userRepository.save(user);
    }

    public Optional<User> authenticate(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()));
    }

    public UserDtos.UserResponse toResponse(User user) {
        return toResponse(user, 0L);
    }

    public UserDtos.UserResponse toResponse(User user, long playCount) {
        return new UserDtos.UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getAvatarUrl(),
                user.isPrivateAccount(),
                user.isAdmin(),
                playCount
        );
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public List<User> findByUsernamePart(String part) {
        return userRepository.findByUsernameContainingIgnoreCase(part);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(UUID id, UserDtos.UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow();
        if (request.username() != null && !request.username().isBlank()) {
            user.setUsername(request.username());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.privateAccount() != null) {
            user.setPrivateAccount(request.privateAccount());
        }
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(User user) {
        refreshTokenRepository.deleteByUser(user);
        notificationRepository.deleteByUser(user);
        trackLikeRepository.findByUser(user).forEach(trackLikeRepository::delete);
        userFollowRepository.findByFollower(user).forEach(userFollowRepository::delete);
        userFollowRepository.findByTarget(user).forEach(userFollowRepository::delete);
        friendRequestRepository.deleteByRequester(user);
        friendRequestRepository.deleteByReceiver(user);
        albumLikeRepository.deleteByUser(user);
        List<Track> tracks = trackService.listByOwner(user);
        for (Track t : tracks) {
            trackService.deleteTrack(t, user);
        }
        List<Playlist> playlists = playlistRepository.findByOwnerOrderByCreatedAtDesc(user);
        for (Playlist p : playlists) {
            playlistTrackRepository.deleteByPlaylist(p);
            playlistRepository.delete(p);
        }
        userRepository.delete(user);
    }
}

