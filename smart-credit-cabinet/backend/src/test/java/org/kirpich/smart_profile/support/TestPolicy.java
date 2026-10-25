package org.kirpich.smart_profile.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.kirpich.smart_profile.domain.policy.CreditPolicy;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.databind.json.JsonMapper;

/** Политика из credit-policy.yml и ответы источников из mocks/ — без поднятия Spring-контекста. */
public final class TestPolicy {

    public static final Path MOCKS = Path.of("..", "mocks");

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private TestPolicy() {
    }

    public static CreditPolicy load() {
        try {
            var sources = new YamlPropertySourceLoader().load("credit-policy", new ClassPathResource("credit-policy.yml"));
            return new Binder(ConfigurationPropertySources.from(sources)).bind("credit", CreditPolicy.class).get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T mockBody(String source, String inn, Class<T> type) {
        try {
            return MAPPER.readValue(Files.readString(MOCKS.resolve("__files").resolve(source).resolve(inn + ".json")), type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
