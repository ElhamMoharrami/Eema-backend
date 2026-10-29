package com.eema.backend.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class AdminPagePolicy {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    private AdminPagePolicy() {}

    public static Pageable bounded(Pageable pageable) {
        int page = Math.max(0, pageable == null ? 0 : pageable.getPageNumber());
        int size = pageable == null ? DEFAULT_SIZE : Math.min(MAX_SIZE, Math.max(1, pageable.getPageSize()));
        return PageRequest.of(page, size);
    }
}
