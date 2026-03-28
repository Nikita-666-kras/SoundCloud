package com.example.soundcloud.service;

import com.example.soundcloud.model.TrackReport;
import com.example.soundcloud.model.Track;
import com.example.soundcloud.model.User;
import com.example.soundcloud.repository.TrackReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TrackReportService {

    private final TrackReportRepository reportRepository;

    public TrackReportService(TrackReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional
    public TrackReport createReport(Track track, User reporter, String reason, String details) {
        TrackReport report = new TrackReport();
        report.setTrack(track);
        report.setReporter(reporter);
        report.setReason(reason);
        report.setDetails(details);
        return reportRepository.save(report);
    }

    public List<TrackReport> listOpen() {
        return reportRepository.findByResolvedFalseOrderByCreatedAtDesc();
    }

    @Transactional
    public TrackReport resolve(UUID id, User admin) {
        TrackReport report = reportRepository.findById(id).orElseThrow();
        report.setResolved(true);
        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }
}

