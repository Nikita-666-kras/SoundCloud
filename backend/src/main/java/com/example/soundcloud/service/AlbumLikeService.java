package com.example.soundcloud.service;

import com.example.soundcloud.model.AlbumLike;
import com.example.soundcloud.model.Track;
import com.example.soundcloud.model.User;
import com.example.soundcloud.repository.AlbumLikeRepository;
import com.example.soundcloud.repository.TrackRepository;
import com.example.soundcloud.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlbumLikeService {

    private final AlbumLikeRepository albumLikeRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;

    public AlbumLikeService(AlbumLikeRepository albumLikeRepository,
                            UserRepository userRepository,
                            TrackRepository trackRepository) {
        this.albumLikeRepository = albumLikeRepository;
        this.userRepository = userRepository;
        this.trackRepository = trackRepository;
    }

    @Transactional
    public void like(User user, UUID ownerId, String albumName) {
        if (albumName == null || albumName.isBlank()) return;
        if (albumLikeRepository.existsByUserAndOwnerIdAndAlbumName(user, ownerId, albumName)) return;
        AlbumLike like = new AlbumLike();
        like.setUser(user);
        like.setOwnerId(ownerId);
        like.setAlbumName(albumName.trim());
        albumLikeRepository.save(like);
    }

    @Transactional
    public void unlike(User user, UUID ownerId, String albumName) {
        albumLikeRepository.findByUserAndOwnerIdAndAlbumName(user, ownerId, albumName)
                .ifPresent(albumLikeRepository::delete);
    }

    public boolean isLiked(User user, UUID ownerId, String albumName) {
        if (user == null || ownerId == null || albumName == null || albumName.isBlank()) return false;
        return albumLikeRepository.existsByUserAndOwnerIdAndAlbumName(user, ownerId, albumName);
    }

    public record AlbumSummary(UUID ownerId, String ownerUsername, String album, String coverUrl, int trackCount, long playCount) {}

    public List<AlbumSummary> getLikedAlbums(User user) {
        if (user == null) return List.of();
        List<AlbumLike> likes = albumLikeRepository.findByUserOrderByCreatedAtDesc(user);
        List<AlbumSummary> result = new ArrayList<>();
        for (AlbumLike like : likes) {
            Optional<User> ownerOpt = userRepository.findById(like.getOwnerId());
            if (ownerOpt.isEmpty()) continue;
            User owner = ownerOpt.get();
            List<Track> tracks = trackRepository.findByOwnerAndAlbumOrderByCreatedAtDesc(owner, like.getAlbumName());
            String coverUrl = null;
            long playCount = 0;
            for (Track t : tracks) {
                if (t.getCoverUrl() != null && coverUrl == null) {
                    coverUrl = "/api/tracks/" + t.getId() + "/cover";
                }
                playCount += t.getPlayCount();
            }
            result.add(new AlbumSummary(
                    like.getOwnerId(),
                    owner.getUsername(),
                    like.getAlbumName(),
                    coverUrl,
                    tracks.size(),
                    playCount
            ));
        }
        return result;
    }
}
