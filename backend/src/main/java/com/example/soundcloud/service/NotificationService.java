package com.example.soundcloud.service;

import com.example.soundcloud.model.Notification;
import com.example.soundcloud.model.Track;
import com.example.soundcloud.model.User;
import com.example.soundcloud.repository.NotificationRepository;
import com.example.soundcloud.repository.TrackRepository;
import com.example.soundcloud.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               TrackRepository trackRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.trackRepository = trackRepository;
    }

    @Transactional
    public Notification createLikeNotification(User to, User from, Track track) {
        if (to.getId().equals(from.getId())) {
            return null;
        }
        Notification n = new Notification();
        n.setUser(to);
        n.setType(Notification.Type.LIKE);
        n.setMessage(from.getUsername() + " лайкнул твой трек \"" + track.getTitle() + "\"");
        n.setTrackId(track.getId());
        n.setFromUserId(from.getId());
        return notificationRepository.save(n);
    }

    @Transactional
    public Notification createCommentNotification(User to, User from, Track track) {
        if (to.getId().equals(from.getId())) {
            return null;
        }
        Notification n = new Notification();
        n.setUser(to);
        n.setType(Notification.Type.COMMENT);
        n.setMessage(from.getUsername() + " прокомментировал твой трек \"" + track.getTitle() + "\"");
        n.setTrackId(track.getId());
        n.setFromUserId(from.getId());
        return notificationRepository.save(n);
    }

    @Transactional
    public Notification createFriendRequestNotification(User to, User from) {
        if (to.getId().equals(from.getId())) {
            return null;
        }
        Notification n = new Notification();
        n.setUser(to);
        n.setType(Notification.Type.FRIEND_REQUEST);
        n.setMessage(from.getUsername() + " отправил тебе заявку в друзья");
        n.setFromUserId(from.getId());
        return notificationRepository.save(n);
    }

    @Transactional
    public Notification createRepostNotification(User to, User from, Track track) {
        if (to.getId().equals(from.getId())) {
            return null;
        }
        User managedTo = userRepository.findById(to.getId()).orElseThrow();
        User managedFrom = userRepository.findById(from.getId()).orElseThrow();
        Track managedTrack = trackRepository.findById(track.getId()).orElseThrow();
        Notification n = new Notification();
        n.setUser(managedTo);
        n.setType(Notification.Type.REPOST);
        n.setMessage(managedFrom.getUsername() + " прислал тебе трек \"" + managedTrack.getTitle() + "\"");
        n.setTrackId(managedTrack.getId());
        n.setFromUserId(managedFrom.getId());
        return notificationRepository.save(n);
    }

    public List<Notification> listFor(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long unreadCount(User user) {
        return notificationRepository.countByUserAndReadIsFalse(user);
    }

    @Transactional
    public void markRead(User user, UUID id) {
        Notification n = notificationRepository.findById(id).orElseThrow();
        if (!n.getUser().getId().equals(user.getId())) {
            return;
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public int markAllRead(User user) {
        return notificationRepository.markAllReadForUser(user);
    }
}

