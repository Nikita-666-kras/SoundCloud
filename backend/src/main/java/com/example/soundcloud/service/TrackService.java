package com.example.soundcloud.service;

import com.example.soundcloud.dto.TrackDtos;
import com.example.soundcloud.model.Track;
import com.example.soundcloud.model.TrackLike;
import com.example.soundcloud.model.User;
import com.example.soundcloud.model.UserFollow;
import com.example.soundcloud.repository.TrackCommentRepository;
import com.example.soundcloud.repository.TrackLikeRepository;
import com.example.soundcloud.repository.TrackReportRepository;
import com.example.soundcloud.repository.TrackRepository;
import com.example.soundcloud.repository.AlbumLikeRepository;
import com.example.soundcloud.repository.PlaylistTrackRepository;
import com.example.soundcloud.repository.PromotionCampaignRepository;
import com.example.soundcloud.repository.UserFollowRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TrackService {

    private final TrackRepository trackRepository;
    private final TrackLikeRepository likeRepository;
    private final UserFollowRepository userFollowRepository;
    private final FileStorageService fileStorageService;
    private final TrackCommentRepository trackCommentRepository;
    private final TrackReportRepository trackReportRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final AlbumLikeRepository albumLikeRepository;
    private final PromotionCampaignRepository promotionCampaignRepository;

    public TrackService(TrackRepository trackRepository,
                        TrackLikeRepository likeRepository,
                        UserFollowRepository userFollowRepository,
                        FileStorageService fileStorageService,
                        TrackCommentRepository trackCommentRepository,
                        TrackReportRepository trackReportRepository,
                        PlaylistTrackRepository playlistTrackRepository,
                        AlbumLikeRepository albumLikeRepository,
                        PromotionCampaignRepository promotionCampaignRepository) {
        this.trackRepository = trackRepository;
        this.likeRepository = likeRepository;
        this.userFollowRepository = userFollowRepository;
        this.fileStorageService = fileStorageService;
        this.trackCommentRepository = trackCommentRepository;
        this.trackReportRepository = trackReportRepository;
        this.playlistTrackRepository = playlistTrackRepository;
        this.albumLikeRepository = albumLikeRepository;
        this.promotionCampaignRepository = promotionCampaignRepository;
    }

    @Transactional
    public Track uploadTrack(User owner,
                             String title,
                             String description,
                             String genre,
                             String album,
                             MultipartFile audioFile,
                             MultipartFile coverFile) throws Exception {
        String audioPath = fileStorageService.storeAudio(audioFile);
        Track track = new Track();
        track.setOwner(owner);
        track.setTitle(title);
        track.setDescription(description);
        track.setGenre(genre);
        track.setAlbum(album);
        track.setAudioPath(audioPath);
        if (coverFile != null && !coverFile.isEmpty()) {
            String coverObject = fileStorageService.storeImage(coverFile);
            track.setCoverUrl(coverObject);
        }
        return trackRepository.save(track);
    }

    public List<Track> listAll() {
        return trackRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Track> listPage(int page, int size) {
        // Популярные за последний месяц: считаем лайки за 30 дней и сортируем по ним
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);

        List<Track> all = new ArrayList<>(
                trackRepository.findAll().stream()
                        .filter(t -> t.getOwner() == null || !t.getOwner().isPrivateAccount())
                        .toList()
        );

        all.sort(Comparator
                .comparingLong((Track t) -> likeRepository.countByTrackAndCreatedAtAfter(t, monthAgo))
                .reversed()
                .thenComparing(Track::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int fromIndex = Math.max(page * size, 0);
        if (fromIndex >= all.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + size, all.size());
        return all.subList(fromIndex, toIndex);
    }

    public List<Track> listPageRecent(int page, int size) {
        return trackRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).getContent();
    }

    public List<Track> listPageFromFollowing(User user, int page, int size) {
        List<User> followed = userFollowRepository.findByFollower(user).stream()
                .map(UserFollow::getTarget)
                .collect(Collectors.toList());
        if (followed.isEmpty()) {
            return List.of();
        }
        return trackRepository.findByOwnerInOrderByCreatedAtDesc(followed, PageRequest.of(page, size)).getContent();
    }

    public List<Track> listByOwner(User owner) {
        return trackRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    public List<Track> listByOwnerAndAlbum(User owner, String album) {
        return trackRepository.findByOwnerAndAlbumOrderByCreatedAtDesc(owner, album);
    }

    public List<Track> searchTracks(String q, int page, int size) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return trackRepository.search(q.trim(), PageRequest.of(page, Math.min(size, 50))).getContent();
    }

    public record AlbumSearchResult(java.util.UUID ownerId, String ownerUsername, String album, String coverUrl, int trackCount, long playCount) {}

    public List<AlbumSearchResult> searchAlbums(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        List<Track> tracks = trackRepository.findByAlbumContainingIgnoreCaseAndOwner_PrivateAccountFalse(q.trim());
        return buildAlbumsFromTracks(tracks);
    }

    public List<AlbumSearchResult> listAlbums(String sort, int limit) {
        int loadSize = Math.min(limit * 15, 500);
        List<Track> tracks;
        if ("recent".equalsIgnoreCase(sort)) {
            tracks = trackRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, loadSize)).getContent().stream()
                    .filter(t -> t.getOwner() != null && !t.getOwner().isPrivateAccount())
                    .filter(t -> t.getAlbum() != null && !t.getAlbum().isBlank())
                    .toList();
        } else {
            tracks = listPage(0, loadSize).stream()
                    .filter(t -> t.getAlbum() != null && !t.getAlbum().isBlank())
                    .toList();
        }
        List<AlbumSearchResult> albums = buildAlbumsFromTracks(tracks);
        return albums.size() <= limit ? albums : albums.subList(0, limit);
    }

    private List<AlbumSearchResult> buildAlbumsFromTracks(List<Track> tracks) {
        Map<String, AlbumSearchResult> byKey = new LinkedHashMap<>();
        for (Track t : tracks) {
            if (t.getAlbum() == null || t.getAlbum().isBlank()) continue;
            String key = t.getOwner().getId() + "|" + t.getAlbum();
            long playCount = t.getPlayCount();
            byKey.compute(key, (k, existing) -> {
                if (existing == null) {
                    return new AlbumSearchResult(
                            t.getOwner().getId(),
                            t.getOwner().getUsername(),
                            t.getAlbum(),
                            t.getCoverUrl() != null ? "/api/tracks/" + t.getId() + "/cover" : null,
                            1,
                            playCount
                    );
                }
                return new AlbumSearchResult(
                        existing.ownerId(),
                        existing.ownerUsername(),
                        existing.album(),
                        existing.coverUrl() != null ? existing.coverUrl() : (t.getCoverUrl() != null ? "/api/tracks/" + t.getId() + "/cover" : null),
                        existing.trackCount() + 1,
                        existing.playCount() + playCount
                );
            });
        }
        return List.copyOf(byKey.values());
    }

    public long getTotalPlayCountForOwner(User user) {
        return trackRepository.findByOwnerOrderByCreatedAtDesc(user).stream()
                .mapToLong(Track::getPlayCount)
                .sum();
    }

    @Transactional
    public void deleteTrack(Track track, User currentUser) {
        if (!track.getOwner().getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Not track owner");
        }
        likeRepository.deleteByTrack(track);
        trackCommentRepository.deleteByTrack(track);
        trackReportRepository.findByTrack(track).forEach(trackReportRepository::delete);
        playlistTrackRepository.deleteByTrack(track);
        if (track.getAudioPath() != null) {
            fileStorageService.deleteAudio(track.getAudioPath());
        }
        if (track.getCoverUrl() != null) {
            fileStorageService.deleteImage(track.getCoverUrl());
        }
        promotionCampaignRepository.deleteByTrack(track);
        trackRepository.delete(track);
    }

    @Transactional
    public void deleteAlbum(User owner, String albumName, User currentUser) {
        if (!owner.getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Not album owner");
        }
        List<Track> tracks = trackRepository.findByOwnerAndAlbumOrderByCreatedAtDesc(owner, albumName);
        for (Track t : tracks) {
            deleteTrack(t, currentUser);
        }
        albumLikeRepository.deleteByOwnerIdAndAlbumName(owner.getId(), albumName);
    }

    public TrackDtos.TrackUploadResponse toResponse(Track track, long likesCount) {
        return new TrackDtos.TrackUploadResponse(
                track.getId(),
                track.getTitle(),
                track.getDescription(),
                track.getGenre(),
                track.getAlbum(),
                track.getCoverUrl() != null ? "/api/tracks/" + track.getId() + "/cover" : null,
                "/api/tracks/" + track.getId() + "/stream",
                track.getOwner().getId(),
                track.getOwner().getUsername(),
                track.getCreatedAt(),
                track.getPlayCount(),
                likesCount
        );
    }

    /**
     * Cover URL for feed: track's own cover, or album cover if track is in an album but has no cover.
     */
    public String getCoverUrlForFeed(Track track) {
        if (track.getCoverUrl() != null) {
            return "/api/tracks/" + track.getId() + "/cover";
        }
        if (track.getAlbum() != null && !track.getAlbum().isBlank() && track.getOwner() != null) {
            return trackRepository
                    .findFirstByOwnerAndAlbumAndCoverUrlIsNotNullOrderByCreatedAtAsc(track.getOwner(), track.getAlbum())
                    .map(t -> "/api/tracks/" + t.getId() + "/cover")
                    .orElse(null);
        }
        return null;
    }

    public boolean userHasLiked(User user, Track track) {
        if (user == null || track == null) {
            return false;
        }
        return likeRepository.findByUserAndTrack(user, track).isPresent();
    }

    public TrackDtos.TrackListItem toListItem(Track track, long likesCount, User viewerOrNull) {
        boolean likedByMe = userHasLiked(viewerOrNull, track);
        return new TrackDtos.TrackListItem(
                track.getId(),
                track.getTitle(),
                track.getGenre(),
                track.getAlbum(),
                getCoverUrlForFeed(track),
                track.getOwner().getId(),
                track.getOwner().getUsername(),
                track.getPlayCount(),
                likesCount,
                likedByMe,
                track.getCreatedAt()
        );
    }

    @Transactional
    public void likeTrack(User user, Track track) {
        likeRepository.findByUserAndTrack(user, track)
                .ifPresentOrElse(
                        likeRepository::delete,
                        () -> {
                            TrackLike like = new TrackLike();
                            like.setUser(user);
                            like.setTrack(track);
                            likeRepository.save(like);
                        }
                );
    }

    public long countLikes(Track track) {
        return likeRepository.countByTrack(track);
    }

    @Transactional
    public Track updateTrack(Track track, TrackDtos.TrackUpdateRequest request) {
        if (request.title() != null && !request.title().isBlank()) {
            track.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            track.setDescription(request.description());
        }
        if (request.genre() != null) {
            track.setGenre(request.genre());
        }
        if (request.album() != null) {
            track.setAlbum(request.album());
        }
        return trackRepository.save(track);
    }
}

