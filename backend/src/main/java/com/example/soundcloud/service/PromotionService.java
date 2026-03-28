package com.example.soundcloud.service;

import com.example.soundcloud.dto.NonStopDtos;
import com.example.soundcloud.model.PromotionCampaign;
import com.example.soundcloud.model.Track;
import com.example.soundcloud.model.User;
import com.example.soundcloud.repository.PromotionCampaignRepository;
import com.example.soundcloud.repository.TrackRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class PromotionService {

    private final PromotionCampaignRepository promotionCampaignRepository;
    private final TrackRepository trackRepository;

    public PromotionService(PromotionCampaignRepository promotionCampaignRepository, TrackRepository trackRepository) {
        this.promotionCampaignRepository = promotionCampaignRepository;
        this.trackRepository = trackRepository;
    }

    @Transactional
    public NonStopDtos.PromotionMineItem createCampaign(User owner, UUID trackId, int maxImpressions) {
        if (maxImpressions < 1 || maxImpressions > 50_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxImpressions 1..50000");
        }
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Трек не найден"));
        if (!track.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Не ваш трек");
        }
        if (promotionCampaignRepository.existsByTrack_IdAndActiveTrue(trackId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "У этого трека уже есть активная кампания");
        }
        PromotionCampaign c = new PromotionCampaign();
        c.setTrack(track);
        c.setMaxImpressions(maxImpressions);
        promotionCampaignRepository.save(c);
        return toMineItem(c);
    }

    @Transactional(readOnly = true)
    public List<NonStopDtos.PromotionMineItem> listMine(User owner) {
        return promotionCampaignRepository.findByTrack_Owner_IdOrderByCreatedAtDesc(owner.getId()).stream()
                .map(this::toMineItem)
                .toList();
    }

    private NonStopDtos.PromotionMineItem toMineItem(PromotionCampaign c) {
        return new NonStopDtos.PromotionMineItem(
                c.getId(),
                c.getTrack().getId(),
                c.getTrack().getTitle(),
                c.getMaxImpressions(),
                c.getServedCount(),
                c.getSkipCount(),
                c.getLikeCount(),
                c.getFullListenCount(),
                c.isActive()
        );
    }

    /**
     * Резервирует один показ промо: увеличивает servedCount, при достижении лимита деактивирует кампанию.
     */
    @Transactional
    public Optional<NonStopDtos.NonStopTrackSlot> tryReservePromotedSlot(Set<UUID> excludeTrackIds, TrackService trackService, User viewerOrNull) {
        List<PromotionCampaign> eligible = promotionCampaignRepository.findActiveWithRemainingSlots().stream()
                .filter(c -> !excludeTrackIds.contains(c.getTrack().getId()))
                .filter(c -> c.getTrack().getOwner() != null && !c.getTrack().getOwner().isPrivateAccount())
                .toList();
        if (eligible.isEmpty()) {
            return Optional.empty();
        }
        Collections.shuffle(eligible);
        PromotionCampaign c = eligible.get(0);
        c.setServedCount(c.getServedCount() + 1);
        if (c.getServedCount() >= c.getMaxImpressions()) {
            c.setActive(false);
        }
        promotionCampaignRepository.save(c);
        Track t = c.getTrack();
        return Optional.of(new NonStopDtos.NonStopTrackSlot(
                trackService.toListItem(t, trackService.countLikes(t), viewerOrNull),
                c.getId()
        ));
    }

    @Transactional
    public void recordFeedback(UUID campaignId, UUID trackId, String eventRaw) {
        if (campaignId == null) {
            return;
        }
        String event = eventRaw == null ? "" : eventRaw.trim().toUpperCase();
        PromotionCampaign c = promotionCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Кампания не найдена"));
        if (!c.getTrack().getId().equals(trackId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Трек не совпадает с кампанией");
        }
        switch (event) {
            case "SKIP" -> c.setSkipCount(c.getSkipCount() + 1);
            case "LIKE" -> c.setLikeCount(c.getLikeCount() + 1);
            case "FULL_LISTEN" -> c.setFullListenCount(c.getFullListenCount() + 1);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "event: SKIP, LIKE или FULL_LISTEN");
        }
        promotionCampaignRepository.save(c);
    }
}
