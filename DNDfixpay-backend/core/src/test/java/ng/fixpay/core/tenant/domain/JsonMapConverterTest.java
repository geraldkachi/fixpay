package ng.fixpay.core.tenant.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonMapConverterTest {

    private final JsonMapConverter converter = new JsonMapConverter();

    @Test
    void convertToDatabaseColumn_nullOrEmpty_returnsEmptyObject() {
        assertEquals("{}", converter.convertToDatabaseColumn(null));
        assertEquals("{}", converter.convertToDatabaseColumn(Map.of()));
    }

    @Test
    void convertToDatabaseColumn_nestedMap_serializes() {
        String json = converter.convertToDatabaseColumn(Map.of(
                "enabled", true,
                "limits", Map.of("daily", 1000)
        ));

        assertTrue(json.contains("\"enabled\":true"));
        assertTrue(json.contains("\"daily\":1000"));
    }

    @Test
    void convertToEntityAttribute_blank_returnsEmptyMap() {
        assertTrue(converter.convertToEntityAttribute(null).isEmpty());
        assertTrue(converter.convertToEntityAttribute(" ").isEmpty());
    }

    @Test
    void convertToEntityAttribute_validJson_returnsMap() {
        Map<String, Object> map = converter.convertToEntityAttribute("{\"walletTransfers\":true,\"limit\":1500}");
        assertEquals(true, map.get("walletTransfers"));
        assertEquals(1500, map.get("limit"));
    }

    @Test
    void convertToEntityAttribute_malformed_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> converter.convertToEntityAttribute("{bad-json}"));
    }
}
