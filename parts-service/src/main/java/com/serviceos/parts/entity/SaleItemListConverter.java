package com.serviceos.parts.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class SaleItemListConverter implements AttributeConverter<List<SaleItem>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<SaleItem> attribute) {
        if (attribute == null) return "[]";
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize sale items", e);
        }
    }

    @Override
    public List<SaleItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return List.of();
        try {
            return MAPPER.readValue(dbData, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize sale items", e);
        }
    }
}
