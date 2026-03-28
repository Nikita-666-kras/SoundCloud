package com.example.soundcloud.service;

import com.example.soundcloud.model.FriendRequest;
import com.example.soundcloud.model.User;
import com.example.soundcloud.repository.FriendRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final NotificationService notificationService;

    public FriendService(FriendRequestRepository friendRequestRepository,
                         NotificationService notificationService) {
        this.friendRequestRepository = friendRequestRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public FriendRequest sendRequest(User from, User to) {
        if (from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("Cannot friend yourself");
        }
        Optional<FriendRequest> existing = friendRequestRepository.findByRequesterAndReceiver(from, to);
        if (existing.isPresent()) {
            if (existing.get().getStatus() == FriendRequest.Status.PENDING) {
                throw new IllegalStateException("Friend request already sent");
            }
            return existing.get();
        }
        FriendRequest r = new FriendRequest();
        r.setRequester(from);
        r.setReceiver(to);
        r.setStatus(FriendRequest.Status.PENDING);
        FriendRequest saved = friendRequestRepository.save(r);
        notificationService.createFriendRequestNotification(to, from);
        return saved;
    }

    public List<FriendRequest> incomingPending(User user) {
        return friendRequestRepository.findByReceiverAndStatus(user, FriendRequest.Status.PENDING);
    }

    public List<FriendRequest> outgoingPending(User user) {
        return friendRequestRepository.findByRequesterAndStatus(user, FriendRequest.Status.PENDING);
    }

    public List<FriendRequest> acceptedFor(User user) {
        List<FriendRequest> asRequester = friendRequestRepository.findByRequesterAndStatus(user, FriendRequest.Status.ACCEPTED);
        List<FriendRequest> asReceiver = friendRequestRepository.findByReceiverAndStatus(user, FriendRequest.Status.ACCEPTED);
        asRequester.addAll(asReceiver);
        return asRequester;
    }

    @Transactional
    public FriendRequest respond(UUID requestId, User receiver, boolean accept) {
        FriendRequest request = friendRequestRepository.findById(requestId).orElseThrow();
        if (!request.getReceiver().getId().equals(receiver.getId())) {
            throw new IllegalStateException("Not receiver");
        }
        request.setStatus(accept ? FriendRequest.Status.ACCEPTED : FriendRequest.Status.REJECTED);
        return friendRequestRepository.save(request);
    }

    @Transactional
    public void removeFriend(User me, UUID friendId) {
        if (me.getId().equals(friendId)) {
            throw new IllegalArgumentException("Cannot remove yourself");
        }
        List<FriendRequest> accepted = acceptedFor(me);
        FriendRequest toRemove = accepted.stream()
                .filter(fr -> {
                    UUID otherId = fr.getRequester().getId().equals(me.getId())
                            ? fr.getReceiver().getId()
                            : fr.getRequester().getId();
                    return otherId.equals(friendId);
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Not friends"));
        friendRequestRepository.delete(toRemove);
    }
}

