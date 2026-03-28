package com.example.soundcloud.service;

import com.example.soundcloud.dto.SupportDtos;
import com.example.soundcloud.model.SupportMessage;
import com.example.soundcloud.model.User;
import com.example.soundcloud.repository.SupportMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SupportService {

    private static final String WELCOME =
            "Здравствуйте! Это чат поддержки slapshous. Напишите ваш вопрос — ответ появится здесь.";

    private final SupportMessageRepository supportMessageRepository;
    private final UserService userService;

    public SupportService(SupportMessageRepository supportMessageRepository, UserService userService) {
        this.supportMessageRepository = supportMessageRepository;
        this.userService = userService;
    }

    @Transactional
    public List<SupportDtos.SupportMessageResponse> getThread(User user) {
        List<SupportMessage> list = supportMessageRepository.findByUserOrderByCreatedAtAsc(user);
        if (list.isEmpty()) {
            SupportMessage welcome = new SupportMessage();
            welcome.setUser(user);
            welcome.setSender(SupportMessage.Sender.STAFF);
            welcome.setBody(WELCOME);
            supportMessageRepository.save(welcome);
            list = supportMessageRepository.findByUserOrderByCreatedAtAsc(user);
        }
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SupportDtos.SupportMessageResponse addUserMessage(User user, String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Пустое сообщение");
        }
        if (trimmed.length() > 4000) {
            throw new IllegalArgumentException("Сообщение слишком длинное");
        }
        SupportMessage m = new SupportMessage();
        m.setUser(user);
        m.setSender(SupportMessage.Sender.USER);
        m.setBody(trimmed);
        supportMessageRepository.save(m);
        return toResponse(m);
    }

    @Transactional
    public SupportDtos.SupportMessageResponse addStaffMessage(User targetUser, String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Пустое сообщение");
        }
        if (trimmed.length() > 4000) {
            throw new IllegalArgumentException("Сообщение слишком длинное");
        }
        SupportMessage m = new SupportMessage();
        m.setUser(targetUser);
        m.setSender(SupportMessage.Sender.STAFF);
        m.setBody(trimmed);
        supportMessageRepository.save(m);
        return toResponse(m);
    }

    @Transactional(readOnly = true)
    public List<SupportDtos.SupportMessageResponse> getThreadReadOnly(User targetUser) {
        return supportMessageRepository.findByUserOrderByCreatedAtAsc(targetUser).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupportDtos.AdminSupportThreadSummary> listThreadsForAdmin() {
        List<UUID> ids = supportMessageRepository.findDistinctUserIds();
        List<SupportDtos.AdminSupportThreadSummary> out = new ArrayList<>();
        for (UUID id : ids) {
            Optional<User> userOpt = userService.findById(id);
            if (userOpt.isEmpty()) {
                continue;
            }
            User u = userOpt.get();
            Optional<SupportMessage> lastOpt = supportMessageRepository.findFirstByUser_IdOrderByCreatedAtDesc(id);
            if (lastOpt.isEmpty()) {
                continue;
            }
            SupportMessage last = lastOpt.get();
            String preview = last.getBody();
            if (preview.length() > 140) {
                preview = preview.substring(0, 137) + "…";
            }
            out.add(new SupportDtos.AdminSupportThreadSummary(
                    u.getId(), u.getUsername(), preview, last.getCreatedAt().toString()));
        }
        out.sort(Comparator.comparing(SupportDtos.AdminSupportThreadSummary::lastMessageAt).reversed());
        return out;
    }

    private SupportDtos.SupportMessageResponse toResponse(SupportMessage m) {
        return new SupportDtos.SupportMessageResponse(
                m.getId(),
                m.getSender().name(),
                m.getBody(),
                m.getCreatedAt().toString()
        );
    }
}
