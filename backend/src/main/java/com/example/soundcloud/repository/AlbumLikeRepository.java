package com.example.soundcloud.repository;

import com.example.soundcloud.model.AlbumLike;
import com.example.soundcloud.model.User;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlbumLikeRepository extends org.springframework.data.jpa.repository.JpaRepository<AlbumLike, UUID> {

    List<AlbumLike> findByUserOrderByCreatedAtDesc(User user);

    Optional<AlbumLike> findByUserAndOwnerIdAndAlbumName(User user, UUID ownerId, String albumName);

    boolean existsByUserAndOwnerIdAndAlbumName(User user, UUID ownerId, String albumName);

    void deleteByOwnerIdAndAlbumName(UUID ownerId, String albumName);

    void deleteByUser(User user);
}
