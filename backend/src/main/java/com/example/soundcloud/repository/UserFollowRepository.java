package com.example.soundcloud.repository;

import com.example.soundcloud.model.User;
import com.example.soundcloud.model.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserFollowRepository extends JpaRepository<UserFollow, UUID> {

    List<UserFollow> findByFollower(User follower);

    Optional<UserFollow> findByFollowerAndTarget(User follower, User target);

    List<UserFollow> findByTarget(User target);

    void deleteByFollower(User follower);

    void deleteByTarget(User target);
}

