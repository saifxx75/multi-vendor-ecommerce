package com.marketplace.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Builds a safe Pageable from query parameters: size is capped and sorting is limited to a whitelist,
 * so a client cannot trigger errors (or expensive sorts) by sorting on arbitrary fields.
 */
final class PageUtil {

    private static final int MAX_PAGE_SIZE = 100;

    private PageUtil() {
    }

    static Pageable of(int page, int size, String sortBy, String direction,
                       Set<String> allowedSortFields, String defaultSortField) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String field = (sortBy != null && allowedSortFields.contains(sortBy)) ? sortBy : defaultSortField;
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(dir, field));
    }
}
