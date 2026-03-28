package com.example.soundcloud.repository;

import com.example.soundcloud.model.Playlist;
import com.example.soundcloud.model.PlaylistTrack;
import com.example.soundcloud.model.Track;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, UUID> {

    List<PlaylistTrack> findByPlaylistOrderByPositionAsc(Playlist playlist);

    Optional<PlaylistTrack> findByPlaylistAndTrack(Playlist playlist, Track track);

    void deleteByTrack(Track track);

    void deleteByPlaylist(Playlist playlist);
}

