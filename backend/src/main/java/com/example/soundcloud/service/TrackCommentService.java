package com.example.soundcloud.service;

import com.example.soundcloud.dto.TrackDtos;
import com.example.soundcloud.model.Track;
import com.example.soundcloud.model.TrackComment;
import com.example.soundcloud.model.User;
import com.example.soundcloud.repository.TrackCommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrackCommentService {

    private final TrackCommentRepository commentRepository;

    public TrackCommentService(TrackCommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Transactional
    public TrackComment addComment(Track track, User user, String text) {
        TrackComment comment = new TrackComment();
        comment.setTrack(track);
        comment.setUser(user);
        comment.setText(text);
        return commentRepository.save(comment);
    }

    public List<TrackComment> getComments(Track track) {
        return commentRepository.findByTrackOrderByCreatedAtAsc(track);
    }

    public TrackDtos.CommentResponse toResponse(TrackComment comment) {
        return new TrackDtos.CommentResponse(
                comment.getId(),
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getText(),
                comment.getCreatedAt()
        );
    }
}

