package com.eema.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;

/** Reject extra auth properties without changing the application's general JSON contract. */
public interface StrictAuthRequest {
    @JsonAnySetter
    default void rejectUnsupportedField(String name, JsonNode value) {
        // Neither the property name nor its potentially sensitive value belongs in the error.
        throw new IllegalArgumentException("Request contains unsupported fields.");
    }
}
