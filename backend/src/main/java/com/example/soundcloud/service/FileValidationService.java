package com.example.soundcloud.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
public class FileValidationService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/pjpeg", "image/png", "image/webp"
    );
    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/wave",
            "audio/ogg", "audio/flac", "audio/x-flac"
    );

    private static final int MAX_IMAGE_SIZE_MB = 10;
    private static final int MAX_AUDIO_SIZE_MB = 100;

    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не выбран");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Допустимые форматы изображения: JPEG, PNG, WebP");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_MB * 1024L * 1024L) {
            throw new IllegalArgumentException("Максимальный размер изображения: " + MAX_IMAGE_SIZE_MB + " МБ");
        }
    }

    public void validateAudio(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Аудиофайл не выбран");
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            contentType = contentType.toLowerCase().split(";")[0].trim();
        }
        if (contentType == null || !ALLOWED_AUDIO_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Допустимые форматы аудио: MP3, WAV, OGG, FLAC");
        }
        if (file.getSize() > MAX_AUDIO_SIZE_MB * 1024L * 1024L) {
            throw new IllegalArgumentException("Максимальный размер аудио: " + MAX_AUDIO_SIZE_MB + " МБ");
        }
    }
}
