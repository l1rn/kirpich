package org.kirpich.smart_profile.config;

import org.springframework.stereotype.Component;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** JSON-снимки в text-колонках: ответы источников, факторы скоринга, предложения. */
@Component
public class JsonCodec {

    private final JsonMapper mapper;

    public JsonCodec(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public String write(Object value) {
        return value == null ? null : mapper.writeValueAsString(value);
    }

    public <T> T read(String json, Class<T> type) {
        return json == null ? null : mapper.readValue(json, type);
    }

    public <T> T read(String json, TypeReference<T> type) {
        return json == null ? null : mapper.readValue(json, type);
    }
}
