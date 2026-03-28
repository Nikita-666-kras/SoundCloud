package com.example.soundcloud.repository;

import com.example.soundcloud.model.Playlist;
import com.example.soundcloud.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

    List<Playlist> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<Playlist> findByOwnerAndName(User owner, String name);
}

