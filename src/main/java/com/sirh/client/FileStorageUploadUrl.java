package com.sirh.client;

import java.time.Instant;

public record FileStorageUploadUrl(
    String id, String bucket, String key, String uploadUrl, Instant expiresAt) {}
