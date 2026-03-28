package com.example.soundcloud.repository;

import com.example.soundcloud.model.SupportMessage;
import com.example.soundcloud.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, UUID> {

    List<SupportMessage> findByUserOrderByCreatedAtAsc(User user);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT m.user.id FROM SupportMessage m")
    List<UUID> findDistinctUserIds();

    Optional<SupportMessage> findFirstByUser_IdOrderByCreatedAtDesc(UUID userId);
}
