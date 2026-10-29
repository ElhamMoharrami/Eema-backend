package com.eema.backend.dto.response;
import com.eema.backend.entity.enums.*;
import java.time.OffsetDateTime;
import java.util.UUID;
public record PolicyVersionDto(UUID id,PolicyType policyType,String version,String title,OffsetDateTime effectiveAt,
 OffsetDateTime publishedAt,String content,String contentHash,PolicyStatus status,OffsetDateTime createdAt,boolean accepted){}
