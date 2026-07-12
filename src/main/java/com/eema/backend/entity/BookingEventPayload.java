package com.eema.backend.entity;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class BookingEventPayload {
    Map<String, Object> data;
}