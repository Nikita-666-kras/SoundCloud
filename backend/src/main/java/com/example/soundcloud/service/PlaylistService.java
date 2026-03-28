package com.example.soundcloud.service;

import com.example.soundcloud.dto.PlaylistDtos;
import com.example.soundcloud.dto.TrackDtos;
import com.example.soundcloud.model.Playlist;
import com.example.soundcloud.model.PlaylistTrack;
import com.example.soundcloud.model.Track;
import com.example.soundcloud.model.User;
import com.example.soundcloud.repository.PlaylistRepository;
import com.example.soundcloud.repository.PlaylistTrackRepository;
import com.example.soundcloud.repository.TrackRepository;
import com.example.soundcloud.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;
    private final TrackService trackService;

    public PlaylistService(PlaylistRepository playlistRepository,
                           PlaylistTrackRepository playlistTrackRepository,
                           TrackRepository trackRepository,
                           UserRepository userRepository,
                           TrackService trackService) {
        this.playlistRepository = playlistRepository;
        this.playlistTrackRepository = playlistTrackRepository;
        this.trackRepository = trackRepository;
        this.userRepository = userRepository;
        this.trackService = trackService;
    }

    public List<Playlist> listByOwner(User owner) {
        return playlistRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    public int getTrackCount(Playlist playlist) {
        return playlistTrackRepository.findByPlaylistOrderByPositionAsc(playlist).size();
    }

    public Playlist create(User owner, String name, String description) {
        Playlist playlist = new Playlist();
        playlist.setOwner(owner);
        playlist.setName(name);
        playlist.setDescription(description);
        return playlistRepository.save(playlist);
    }

    public PlaylistDtos.PlaylistSummary toSummary(Playlist playlist, int trackCount) {
        return new PlaylistDtos.PlaylistSummary(
                playlist.getId(),
                playlist.getName(),
                playlist.getDescription(),
                playlist.getCreatedAt(),
                trackCount
        );
    }

    public PlaylistDtos.PlaylistDetails toDetails(Playlist playlist, User viewer) {
        List<PlaylistTrack> pts = playlistTrackRepository.findByPlaylistOrderByPositionAsc(playlist);
        List<TrackDtos.TrackListItem> items = pts.stream()
                .map(PlaylistTrack::getTrack)
                .map(t -> trackService.toListItem(t, trackService.countLikes(t), viewer))
                .toList();
        return new PlaylistDtos.PlaylistDetails(
                playlist.getId(),
                playlist.getName(),
                playlist.getDescription(),
                playlist.getCreatedAt(),
                items
        );
    }

    @Transactional
    public void addTrack(Playlist playlist, UUID trackId) {
        Track track = trackRepository.findById(trackId).orElseThrow();
        if (playlistTrackRepository.findByPlaylistAndTrack(playlist, track).isPresent()) {
            return;
        }
        int nextPos = playlistTrackRepository.findByPlaylistOrderByPositionAsc(playlist).size() + 1;
        PlaylistTrack pt = new PlaylistTrack();
        pt.setPlaylist(playlist);
        pt.setTrack(track);
        pt.setPosition(nextPos);
        try {
            playlistTrackRepository.save(pt);
        } catch (DataIntegrityViolationException e) {
            // Трек уже в плейлисте (гонка или повторный запрос) — считаем успехом
        }
    }

    @Transactional
    public Playlist getOrCreateRepostPlaylist(User owner, User fromUser) {
        String name = "Послушать от " + fromUser.getUsername();
        return playlistRepository.findByOwnerAndName(owner, name)
                .orElseGet(() -> {
                    Playlist p = new Playlist();
                    p.setOwner(owner);
                    p.setName(name);
                    p.setDescription("Треки, которые прислал " + fromUser.getUsername());
                    return playlistRepository.save(p);
                });
    }

    @Transactional
    public void repostTrack(User from, User to, UUID trackId) {
        User fromManaged = userRepository.findById(from.getId()).orElseThrow();
        User toManaged = userRepository.findById(to.getId()).orElseThrow();
        Playlist playlist = getOrCreateRepostPlaylist(toManaged, fromManaged);
        addTrack(playlist, trackId);
    }

    @Transactional
    public void deletePlaylist(Playlist playlist, User currentUser) {
        if (!playlist.getOwner().getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Not playlist owner");
        }
        playlistTrackRepository.deleteByPlaylist(playlist);
        playlistRepository.delete(playlist);
    }
}

