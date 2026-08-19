package com.sirh.client;

import java.time.Instant;

public record FileStorageDownloadUrl(String id, String downloadUrl, Instant expiresAt) {}
