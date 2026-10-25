package org.kirpich.smart_profile.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.kirpich.smart_profile.domain.sources.BkiReport;
import org.kirpich.smart_profile.domain.sources.EgrulCompany;
import org.kirpich.smart_profile.domain.sources.FnsBusiness;
import org.kirpich.smart_profile.integration.RegistryGateway;
import org.kirpich.smart_profile.integration.SourceNotFoundException;
import org.kirpich.smart_profile.integration.SourceUnavailableException;

import tools.jackson.databind.json.JsonMapper;

/**
 * Шлюз для тестов: отвечает по тем же файлам mocks/, что и WireMock в docker compose.
 * Так тесты заодно проверяют, что заглушки и сценарии не разъехались.
 */
public class WiremockFileRegistry implements RegistryGateway {

    private final Path root;
    private final JsonMapper mapper;
    private final long timeoutMs;
    private final List<Stub> stubs;

    record Stub(String method, String urlPath, String urlPathPattern, String inn, int priority, int status, String bodyFileName, long delayMs) {

        boolean matches(String method, String path, String inn) {
            boolean pathMatches = urlPath != null ? urlPath.equals(path) : path.matches(urlPathPattern);
            return this.method.equals(method) && pathMatches && (this.inn == null || this.inn.equals(inn));
        }
    }

    public WiremockFileRegistry(Path root, JsonMapper mapper, long timeoutMs) {
        this.root = root;
        this.mapper = mapper;
        this.timeoutMs = timeoutMs;
        try (Stream<Path> files = Files.list(root.resolve("mappings"))) {
            this.stubs = files.filter(p -> p.toString().endsWith(".json")).map(this::parse).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public EgrulCompany egrulCompany(String inn) {
        return call("GET", "/egrul/v1/companies/" + inn, null, EgrulCompany.class);
    }

    @Override
    public FnsBusiness fnsBusiness(String inn) {
        return call("GET", "/fns/v1/business/" + inn, null, FnsBusiness.class);
    }

    @Override
    public BkiReport bkiReport(String inn, String consentId) {
        return call("POST", "/bki/v1/reports", inn, BkiReport.class);
    }

    private <T> T call(String method, String path, String bodyInn, Class<T> type) {
        Stub stub = stubs.stream()
                .filter(s -> s.matches(method, path, bodyInn))
                .min(Comparator.comparingInt(Stub::priority))
                .orElseThrow(() -> new AssertionError("Нет заглушки для " + method + " " + path));
        if (stub.delayMs() > timeoutMs) {
            throw new SourceUnavailableException("таймаут или нет соединения", null);
        }
        if (stub.status() == 404) {
            throw new SourceNotFoundException(path);
        }
        if (stub.status() >= 500) {
            throw new SourceUnavailableException("ответ " + stub.status(), null);
        }
        try {
            return mapper.readValue(Files.readString(root.resolve("__files").resolve(stub.bodyFileName())), type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Stub parse(Path file) {
        try {
            Map<String, Object> mapping = mapper.readValue(Files.readString(file), Map.class);
            Map<String, Object> request = (Map<String, Object>) mapping.get("request");
            Map<String, Object> response = (Map<String, Object>) mapping.get("response");
            String inn = null;
            List<Map<String, Object>> bodyPatterns = (List<Map<String, Object>>) request.get("bodyPatterns");
            if (bodyPatterns != null) {
                Map<String, Object> jsonPath = (Map<String, Object>) bodyPatterns.getFirst().get("matchesJsonPath");
                inn = (String) jsonPath.get("equalTo");
            }
            Number delay = (Number) response.get("fixedDelayMilliseconds");
            return new Stub(
                    (String) request.get("method"),
                    (String) request.get("urlPath"),
                    (String) request.get("urlPathPattern"),
                    inn,
                    ((Number) mapping.getOrDefault("priority", 5)).intValue(),
                    ((Number) response.get("status")).intValue(),
                    (String) response.get("bodyFileName"),
                    delay == null ? 0 : delay.longValue());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
