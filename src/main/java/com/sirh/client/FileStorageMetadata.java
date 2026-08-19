package com.sirh.client;

import java.time.Instant;

public record FileStorageMetadata(
    String id,
    String bucket,
    String contentType,
    long sizeBytes,
    Instant lastModified,
    String status) {}
