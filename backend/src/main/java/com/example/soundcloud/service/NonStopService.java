package com.example.soundcloud.service;

import com.example.soundcloud.dto.NonStopDtos;
import com.example.soundcloud.model.Track;
import com.example.soundcloud.model.User;
import com.example.soundcloud.model.UserFollow;
import com.example.soundcloud.repository.TrackRepository;
import com.example.soundcloud.repository.UserFollowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class NonStopService {

    private static final double PROMO_PROBABILITY = 0.28;

    private final TrackRepository trackRepository;
    private final TrackService trackService;
    private final PromotionService promotionService;
    private final UserFollowRepository userFollowRepository;

    public NonStopService(TrackRepository trackRepository,
                          TrackService trackService,
                          PromotionService promotionService,
                          UserFollowRepository userFollowRepository) {
        this.trackRepository = trackRepository;
        this.trackService = trackService;
        this.promotionService = promotionService;
        this.userFollowRepository = userFollowRepository;
    }

    @Transactional(readOnly = true)
    public List<NonStopDtos.NonStopTrackSlot> buildPlaylist(int size, List<UUID> excludeIds, User userOrNull) {
        int n = Math.min(Math.max(size, 1), 40);
        Set<UUID> used = new HashSet<>(excludeIds);
        List<NonStopDtos.NonStopTrackSlot> out = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            if (ThreadLocalRandom.current().nextDouble() < PROMO_PROBABILITY) {
                Optional<NonStopDtos.NonStopTrackSlot> promo = promotionService.tryReservePromotedSlot(used, trackService, userOrNull);
                if (promo.isPresent()) {
                    out.add(promo.get());
                    used.add(promo.get().track().id());
                    continue;
                }
            }
            Optional<Track> organic = pickOrganicTrack(userOrNull, used);
            if (organic.isEmpty()) {
                break;
            }
            Track t = organic.get();
            out.add(new NonStopDtos.NonStopTrackSlot(
                    trackService.toListItem(t, trackService.countLikes(t), userOrNull),
                    null
            ));
            used.add(t.getId());
        }
        return out;
    }

    private Optional<Track> pickOrganicTrack(User userOrNull, Set<UUID> used) {
        LinkedHashSet<Track> pool = new LinkedHashSet<>();
        trackService.listPage(0, 60).stream()
                .filter(t -> t.getOwner() != null && !t.getOwner().isPrivateAccount())
                .filter(t -> !used.contains(t.getId()))
                .forEach(pool::add);
        trackService.listPageRecent(0, 40).stream()
                .filter(t -> t.getOwner() != null && !t.getOwner().isPrivateAccount())
                .filter(t -> !used.contains(t.getId()))
                .forEach(pool::add);

        if (userOrNull != null) {
            List<User> followed = userFollowRepository.findByFollower(userOrNull).stream()
                    .map(UserFollow::getTarget)
                    .collect(Collectors.toList());
            if (!followed.isEmpty()) {
                trackRepository.findByOwnerInOrderByCreatedAtDesc(followed,
                                org.springframework.data.domain.PageRequest.of(0, 40))
                        .getContent().stream()
                        .filter(t -> !used.contains(t.getId()))
                        .forEach(pool::add);
            }
        }

        List<Track> list = new ArrayList<>(pool);
        if (list.isEmpty()) {
            list = trackRepository.findAll().stream()
                    .filter(t -> t.getOwner() != null && !t.getOwner().isPrivateAccount())
                    .filter(t -> !used.contains(t.getId()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        if (list.isEmpty()) {
            return Optional.empty();
        }
        Collections.shuffle(list, ThreadLocalRandom.current());
        return Optional.of(list.get(0));
    }
}
