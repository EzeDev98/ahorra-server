package com.ahorra.feature.capture.parser;

import com.ahorra.feature.capture.dto.ParsedEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A note can describe several separate spend or income events, so the providers are asked for
 * { "entries": [ ... ] }, each element shaped like EntryParserSupport.entrySchema.
 */
final class EntryListSupport {

    private EntryListSupport() {
    }

    static ObjectNode entriesSchema(ObjectMapper objectMapper) {
        return wrap(objectMapper, EntryParserSupport.entrySchema(objectMapper), UnaryOperator.identity());
    }

    /** For a provider that has already adjusted the single-entry schema (Groq allows null for optional strings). */
    static ObjectNode entriesSchema(ObjectMapper objectMapper, ObjectNode entrySchema) {
        return wrap(objectMapper, entrySchema, UnaryOperator.identity());
    }

    /** Same, with each type name passed through typeName first (Gemini wants OBJECT, ARRAY and so on). */
    static ObjectNode entriesSchema(ObjectMapper objectMapper, UnaryOperator<String> typeName) {
        return wrap(objectMapper, EntryParserSupport.entrySchema(objectMapper, typeName), typeName);
    }

    private static ObjectNode wrap(ObjectMapper objectMapper, ObjectNode entrySchema, UnaryOperator<String> typeName) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", typeName.apply("object"));
        ObjectNode entries = schema.putObject("properties").putObject("entries");
        entries.put("type", typeName.apply("array"));
        entries.put("description", "One element per separate spend or income event, in the order they were mentioned.");
        entries.set("items", entrySchema);
        schema.putArray("required").add("entries");
        return schema;
    }

    /** Reads { entries: [...] }. A model that answers with one bare entry is still understood. */
    static List<ParsedEntry> toParsedEntries(JsonNode input) {
        List<ParsedEntry> parsed = new ArrayList<>();
        JsonNode entries = input.path("entries");
        if (entries.isArray()) {
            for (JsonNode entry : entries) {
                parsed.add(EntryParserSupport.toParsedEntry(entry));
            }
        } else if (input.has("items")) {
            parsed.add(EntryParserSupport.toParsedEntry(input));
        }
        return parsed;
    }
}
