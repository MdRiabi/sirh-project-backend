package com.sirh.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// Enveloppe unique pour toute réponse API portant une liste/pagination — même
// forme (`content`/`page`/`size`/`totalElements`/`totalPages`) que la collection
// soit issue d'une Page Spring Data ou d'une simple List sans pagination.
public record ListingPageDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> ListingPageDto<T> of(Page<T> page) {
        return new ListingPageDto<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    public static <T> ListingPageDto<T> of(List<T> content) {
        return new ListingPageDto<>(content, 0, content.size(), content.size(), content.isEmpty() ? 0 : 1);
    }
}
