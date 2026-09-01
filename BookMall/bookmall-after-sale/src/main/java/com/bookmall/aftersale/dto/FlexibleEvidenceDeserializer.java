package com.bookmall.aftersale.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Accepts both the documented evidence array and the legacy single-string form.
 */
public class FlexibleEvidenceDeserializer extends JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node == null || node.isNull()) {
            return new ArrayList<>();
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode item : node) {
                if (item != null && item.isTextual() && !item.asText().isBlank()) {
                    values.add(item.asText().trim());
                }
            }
            return values;
        }
        if (node.isTextual() && !node.asText().isBlank()) {
            return List.of(node.asText().trim());
        }
        throw new JsonMappingException(parser, "evidence must be a string or an array of strings");
    }
}
