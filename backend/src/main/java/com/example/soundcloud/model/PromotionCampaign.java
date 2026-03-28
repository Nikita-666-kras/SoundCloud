package com.example.soundcloud.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "promotion_campaigns")
public class PromotionCampaign {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Track track;

    @Column(nullable = false)
    private int maxImpressions = 100;

    @Column(nullable = false)
    private int servedCount = 0;

    @Column(nullable = false)
    private int skipCount = 0;

    @Column(nullable = false)
    private int likeCount = 0;

    @Column(nullable = false)
    private int fullListenCount = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public int getMaxImpressions() {
        return maxImpressions;
    }

    public void setMaxImpressions(int maxImpressions) {
        this.maxImpressions = maxImpressions;
    }

    public int getServedCount() {
        return servedCount;
    }

    public void setServedCount(int servedCount) {
        this.servedCount = servedCount;
    }

    public int getSkipCount() {
        return skipCount;
    }

    public void setSkipCount(int skipCount) {
        this.skipCount = skipCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getFullListenCount() {
        return fullListenCount;
    }

    public void setFullListenCount(int fullListenCount) {
        this.fullListenCount = fullListenCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
