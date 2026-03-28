package com.example.soundcloud.repository;

import com.example.soundcloud.model.FriendRequest;
import com.example.soundcloud.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    List<FriendRequest> findByRequesterAndStatus(User requester, FriendRequest.Status status);

    List<FriendRequest> findByReceiverAndStatus(User receiver, FriendRequest.Status status);

    Optional<FriendRequest> findByRequesterAndReceiver(User requester, User receiver);

    void deleteByRequester(User requester);

    void deleteByReceiver(User receiver);
}

