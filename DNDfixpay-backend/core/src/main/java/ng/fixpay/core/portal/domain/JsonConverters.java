package ng.fixpay.core.portal.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;
import java.util.Map;

/**
 * JPA converters for JSONB columns — avoids the Hypersistence Utils dependency.
 * All store as TEXT in DB; PostgreSQL stores as jsonb via column DDL.
 */
public final class JsonConverters {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonConverters() {}

    @Converter
    public static class DirectorsConverter
            implements AttributeConverter<List<Map<String, String>>, String> {

        @Override
        public String convertToDatabaseColumn(List<Map<String, String>> attr) {
            if (attr == null) return "[]";
            try { return MAPPER.writeValueAsString(attr); }
            catch (Exception e) { throw new IllegalArgumentException("Cannot serialize directors", e); }
        }

        @Override
        public List<Map<String, String>> convertToEntityAttribute(String db) {
            if (db == null || db.isBlank()) return List.of();
            try {
                return MAPPER.readValue(db, new TypeReference<>() {});
            } catch (Exception e) { throw new IllegalArgumentException("Cannot deserialize directors", e); }
        }
    }

    @Converter
    public static class StringMapConverter
            implements AttributeConverter<Map<String, String>, String> {

        @Override
        public String convertToDatabaseColumn(Map<String, String> attr) {
            if (attr == null) return "{}";
            try { return MAPPER.writeValueAsString(attr); }
            catch (Exception e) { throw new IllegalArgumentException("Cannot serialize map", e); }
        }

        @Override
        public Map<String, String> convertToEntityAttribute(String db) {
            if (db == null || db.isBlank()) return Map.of();
            try {
                return MAPPER.readValue(db, new TypeReference<>() {});
            } catch (Exception e) { throw new IllegalArgumentException("Cannot deserialize map", e); }
        }
    }
}

