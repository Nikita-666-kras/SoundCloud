package com.example.soundcloud.repository;

import com.example.soundcloud.model.Track;
import com.example.soundcloud.model.TrackLike;
import com.example.soundcloud.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackLikeRepository extends JpaRepository<TrackLike, UUID> {

    long countByTrack(Track track);

    long countByTrackAndCreatedAtAfter(Track track, LocalDateTime createdAfter);

    Optional<TrackLike> findByUserAndTrack(User user, Track track);

    List<TrackLike> findByUser(User user);

    List<TrackLike> findByUserOrderByCreatedAtDesc(User user);

    void deleteByTrack(Track track);
}

