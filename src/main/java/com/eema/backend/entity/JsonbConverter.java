package com.eema.backend.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Simple converter for JSONB fields. Just passes through the string.
 * Hibernate will treat the column as jsonb if we set columnDefinition = "jsonb".
 */
@Converter(autoApply = false)
public class JsonbConverter implements AttributeConverter<String, String> {

    public JsonbConverter() {
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData;
    }
}