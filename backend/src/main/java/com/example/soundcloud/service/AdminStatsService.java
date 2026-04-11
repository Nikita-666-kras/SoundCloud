package com.example.soundcloud.service;

import com.example.soundcloud.dto.AdminDtos;
import com.example.soundcloud.repository.PlaylistRepository;
import com.example.soundcloud.repository.SupportMessageRepository;
import com.example.soundcloud.repository.TrackCommentRepository;
import com.example.soundcloud.repository.TrackLikeRepository;
import com.example.soundcloud.repository.TrackReportRepository;
import com.example.soundcloud.repository.TrackRepository;
import com.example.soundcloud.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class AdminStatsService {

    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final TrackReportRepository trackReportRepository;
    private final TrackLikeRepository trackLikeRepository;
    private final TrackCommentRepository trackCommentRepository;
    private final PlaylistRepository playlistRepository;
    private final SupportMessageRepository supportMessageRepository;
    private final OnlinePresenceService onlinePresenceService;

    public AdminStatsService(
            UserRepository userRepository,
            TrackRepository trackRepository,
            TrackReportRepository trackReportRepository,
            TrackLikeRepository trackLikeRepository,
            TrackCommentRepository trackCommentRepository,
            PlaylistRepository playlistRepository,
            SupportMessageRepository supportMessageRepository,
            OnlinePresenceService onlinePresenceService) {
        this.userRepository = userRepository;
        this.trackRepository = trackRepository;
        this.trackReportRepository = trackReportRepository;
        this.trackLikeRepository = trackLikeRepository;
        this.trackCommentRepository = trackCommentRepository;
        this.playlistRepository = playlistRepository;
        this.supportMessageRepository = supportMessageRepository;
        this.onlinePresenceService = onlinePresenceService;
    }

    @Transactional(readOnly = true)
    public AdminDtos.StatsResponse getStats() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        LocalDateTime dayStart = todayUtc.atStartOfDay();
        LocalDateTime monthStart = todayUtc.withDayOfMonth(1).atStartOfDay();
        LocalDateTime yearStart = todayUtc.withMonth(1).withDayOfMonth(1).atStartOfDay();

        long tracksToday = trackRepository.countByCreatedAtGreaterThanEqual(dayStart);
        long tracksMonth = trackRepository.countByCreatedAtGreaterThanEqual(monthStart);
        long tracksYear = trackRepository.countByCreatedAtGreaterThanEqual(yearStart);

        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long used = total - free;
        int heapPct = max > 0 ? (int) Math.min(100, (used * 100) / max) : 0;
        long heapUsedMb = used / (1024 * 1024);
        long heapMaxMb = max / (1024 * 1024);

        double loadAvg = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        Double load1m = loadAvg >= 0 ? loadAvg : null;
        int procs = rt.availableProcessors();

        return new AdminDtos.StatsResponse(
                userRepository.count(),
                trackRepository.count(),
                trackRepository.sumPlayCount(),
                trackReportRepository.countByResolved(false),
                trackReportRepository.count(),
                trackLikeRepository.count(),
                trackCommentRepository.count(),
                playlistRepository.count(),
                supportMessageRepository.count(),
                onlinePresenceService.countOnline(),
                load1m,
                procs,
                heapPct,
                heapUsedMb,
                heapMaxMb,
                tracksToday,
                tracksMonth,
                tracksYear
        );
    }
}
