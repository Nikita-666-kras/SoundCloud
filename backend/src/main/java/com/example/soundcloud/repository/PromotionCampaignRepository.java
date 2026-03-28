package com.example.soundcloud.repository;

import com.example.soundcloud.model.PromotionCampaign;
import com.example.soundcloud.model.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionCampaignRepository extends JpaRepository<PromotionCampaign, UUID> {

    boolean existsByTrack_IdAndActiveTrue(UUID trackId);

    void deleteByTrack(Track track);

    List<PromotionCampaign> findByTrack_Owner_IdOrderByCreatedAtDesc(UUID ownerId);

    @Query("SELECT c FROM PromotionCampaign c WHERE c.active = true AND c.servedCount < c.maxImpressions")
    List<PromotionCampaign> findActiveWithRemainingSlots();
}
