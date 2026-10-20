package com.snoozeshare.domain.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.snoozeshare.domain.enums.AmenityType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Converts a {@code Set<AmenityType>} to a comma-separated string and back. */
@Converter
public class AmenitySetConverter
        implements AttributeConverter<Set<AmenityType>, String> {

    @Override
    public String convertToDatabaseColumn(Set<AmenityType> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<AmenityType> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(AmenityType::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }
}
