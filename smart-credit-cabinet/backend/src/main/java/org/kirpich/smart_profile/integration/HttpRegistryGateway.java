package org.kirpich.smart_profile.integration;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.function.Supplier;

import org.kirpich.smart_profile.config.AppProperties;
import org.kirpich.smart_profile.domain.sources.BkiReport;
import org.kirpich.smart_profile.domain.sources.EgrulCompany;
import org.kirpich.smart_profile.domain.sources.FnsBusiness;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/** HTTP-клиент к ЕГРЮЛ, ФНС и БКИ. В демо их заменяет WireMock (каталог mocks/ в корне репозитория). */
@Component
public class HttpRegistryGateway implements RegistryGateway {

    private final RestClient http;

    public HttpRegistryGateway(AppProperties props) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(props.registry().timeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(props.registry().timeout());
        this.http = RestClient.builder().baseUrl(props.registry().baseUrl()).requestFactory(factory).build();
    }

    @Override
    public EgrulCompany egrulCompany(String inn) {
        return call("ЕГРЮЛ", () -> http.get().uri("/egrul/v1/companies/{inn}", inn)
                .retrieve().body(EgrulCompany.class));
    }

    @Override
    public FnsBusiness fnsBusiness(String inn) {
        return call("ФНС", () -> http.get().uri("/fns/v1/business/{inn}", inn)
                .retrieve().body(FnsBusiness.class));
    }

    @Override
    public BkiReport bkiReport(String inn, String consentId) {
        return call("БКИ", () -> http.post().uri("/bki/v1/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("inn", inn, "consentId", consentId))
                .retrieve().body(BkiReport.class));
    }

    private static <T> T call(String source, Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException.NotFound e) {
            throw new SourceNotFoundException(source + ": сведения не найдены");
        } catch (HttpServerErrorException e) {
            throw new SourceUnavailableException("ответ " + e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new SourceUnavailableException("таймаут или нет соединения", e);
        }
    }
}
