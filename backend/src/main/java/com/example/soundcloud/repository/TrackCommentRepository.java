package com.example.soundcloud.repository;

import com.example.soundcloud.model.Track;
import com.example.soundcloud.model.TrackComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrackCommentRepository extends JpaRepository<TrackComment, UUID> {

    List<TrackComment> findByTrackOrderByCreatedAtAsc(Track track);

    void deleteByTrack(Track track);
}

