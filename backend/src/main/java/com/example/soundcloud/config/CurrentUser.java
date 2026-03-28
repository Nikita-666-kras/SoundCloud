package com.example.soundcloud.config;

import java.util.UUID;

/**
 * Principal, устанавливаемый JWT-фильтром после успешной проверки токена.
 */
public record CurrentUser(UUID userId) {}
