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

@Service
public class AdminStatsService {

    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final TrackReportRepository trackReportRepository;
    private final TrackLikeRepository trackLikeRepository;
    private final TrackCommentRepository trackCommentRepository;
    private final PlaylistRepository playlistRepository;
    private final SupportMessageRepository supportMessageRepository;

    public AdminStatsService(
            UserRepository userRepository,
            TrackRepository trackRepository,
            TrackReportRepository trackReportRepository,
            TrackLikeRepository trackLikeRepository,
            TrackCommentRepository trackCommentRepository,
            PlaylistRepository playlistRepository,
            SupportMessageRepository supportMessageRepository) {
        this.userRepository = userRepository;
        this.trackRepository = trackRepository;
        this.trackReportRepository = trackReportRepository;
        this.trackLikeRepository = trackLikeRepository;
        this.trackCommentRepository = trackCommentRepository;
        this.playlistRepository = playlistRepository;
        this.supportMessageRepository = supportMessageRepository;
    }

    @Transactional(readOnly = true)
    public AdminDtos.StatsResponse getStats() {
        return new AdminDtos.StatsResponse(
                userRepository.count(),
                trackRepository.count(),
                trackRepository.sumPlayCount(),
                trackReportRepository.countByResolved(false),
                trackReportRepository.count(),
                trackLikeRepository.count(),
                trackCommentRepository.count(),
                playlistRepository.count(),
                supportMessageRepository.count()
        );
    }
}
