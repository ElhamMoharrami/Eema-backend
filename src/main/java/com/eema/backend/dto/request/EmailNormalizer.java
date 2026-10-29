package com.eema.backend.dto.request;

import java.util.Locale;

/** Canonical human-entered email only. Never use for passwords or opaque tokens. */
public final class EmailNormalizer {
    private EmailNormalizer() {}

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
