package com.example.soundcloud.repository;

import com.example.soundcloud.model.Notification;
import com.example.soundcloud.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    long countByUserAndReadIsFalse(User user);

    void deleteByUser(User user);
}

