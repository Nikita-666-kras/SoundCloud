package com.example.soundcloud.repository;

import com.example.soundcloud.model.Track;
import com.example.soundcloud.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackRepository extends JpaRepository<Track, UUID> {
    List<Track> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<Track> findFirstByOwnerAndAlbumAndCoverUrlIsNotNullOrderByCreatedAtAsc(User owner, String album);

    List<Track> findAllByOrderByCreatedAtDesc();

    Page<Track> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Track> findByOwnerInOrderByCreatedAtDesc(List<User> owners, Pageable pageable);

    List<Track> findByOwnerAndAlbumOrderByCreatedAtDesc(User owner, String album);

    @Query("SELECT t FROM Track t WHERE (LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(t.owner.username) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(t.album) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "AND (t.owner.privateAccount = false)")
    Page<Track> search(@Param("q") String q, Pageable pageable);

    List<Track> findByAlbumContainingIgnoreCaseAndOwner_PrivateAccountFalse(String albumPart);

    @Query("SELECT COALESCE(SUM(t.playCount), 0) FROM Track t")
    long sumPlayCount();
}

