package com.example.soundcloud.repository;

import com.example.soundcloud.model.Track;
import com.example.soundcloud.model.TrackReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrackReportRepository extends JpaRepository<TrackReport, UUID> {

    long countByResolved(boolean resolved);

    List<TrackReport> findByResolvedFalseOrderByCreatedAtDesc();

    List<TrackReport> findByTrack(Track track);
}

