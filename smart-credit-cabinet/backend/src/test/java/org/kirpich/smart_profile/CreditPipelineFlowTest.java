package org.kirpich.smart_profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kirpich.smart_profile.integration.RegistryGateway;
import org.kirpich.smart_profile.service.PipelineService;
import org.kirpich.smart_profile.support.TestPolicy;
import org.kirpich.smart_profile.support.WiremockFileRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import tools.jackson.databind.json.JsonMapper;

/** Сквозные сценарии демо через API: H2 + ответы источников из mocks/. Конвейер двигаем вручную. */
@SpringBootTest
@ActiveProfiles("test")
@Import(CreditPipelineFlowTest.FakeRegistry.class)
class CreditPipelineFlowTest {

    @TestConfiguration
    static class FakeRegistry {

        @Bean
        @Primary
        RegistryGateway wiremockFileRegistry(JsonMapper mapper) {
            return new WiremockFileRegistry(TestPolicy.MOCKS, mapper, 3_000);
        }
    }

    @Autowired
    WebApplicationContext context;

    @Autowired
    PipelineService pipeline;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void petrovGetsCounterOfferSignsAndGetsMoney() throws Exception {
        String client = login("petrov");
        long id = createAndSubmit(client, "BUSINESS_CREDIT", "investment", 8_000_000, 36, 14_000_000);

        assertThat(runPipeline(client, id)).isEqualTo("OFFERED");
        perform(client, get("/api/v1/applications/{id}", id))
                .andExpect(jsonPath("$.offer.kind").value("COUNTER"))
                .andExpect(jsonPath("$.offer.amount").value(7_000_000))
                .andExpect(jsonPath("$.decision.factors").isNotEmpty());

        String code = JsonPath.read(body(perform(client, post("/api/v1/applications/{id}/offer/accept", id))), "$.signingCode");
        perform(client, post("/api/v1/applications/{id}/offer/sign", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"%s\"}".formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.status").value("ISSUED"));
    }

    @Test
    void stroyUploadsDocumentsThenUnderwriterAsksAndApproves() throws Exception {
        String client = login("stroy");
        long id = createAndSubmit(client, "BUSINESS_CREDIT", "investment", 20_000_000, 36, 42_000_000);

        assertThat(runPipeline(client, id)).isEqualTo("NEEDS_INFO");
        String detail = body(perform(client, get("/api/v1/applications/{id}", id)));
        assertThat((String) JsonPath.read(detail, "$.infoRequest.items[0].key")).isEqualTo("financial_statement");
        int systemRequest = JsonPath.read(detail, "$.infoRequest.id");

        perform(client, multipart("/api/v1/applications/{id}/documents", id)
                .file(new MockMultipartFile("file", "otchetnost-2025.pdf", "application/pdf", "%PDF-1.4 demo".getBytes()))
                .param("docType", "financial_statement"))
                .andExpect(status().isOk());
        respond(client, id, systemRequest, "{}").andExpect(jsonPath("$.summary.status").value("CHECKING"));
        assertThat(runPipeline(client, id)).isEqualTo("MANUAL_REVIEW");

        String underwriter = login("underwriter");
        perform(underwriter, get("/api/v1/bank/applications").param("status", "MANUAL_REVIEW"))
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(id)).exists());
        String bankDetail = body(perform(underwriter, post("/api/v1/bank/applications/{id}/info-requests", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"label\":\"Поясните просрочку по кредиту на технику\",\"type\":\"TEXT\"}]}")));
        assertThat((String) JsonPath.read(bankDetail, "$.application.summary.status")).isEqualTo("NEEDS_INFO");

        String request = body(perform(client, get("/api/v1/applications/{id}", id)));
        int bankRequest = JsonPath.read(request, "$.infoRequest.id");
        String key = JsonPath.read(request, "$.infoRequest.items[0].key");
        respond(client, id, bankRequest, "{\"answers\":{\"%s\":\"Задержка оплаты от заказчика, погашено\"}}".formatted(key))
                .andExpect(jsonPath("$.summary.status").value("MANUAL_REVIEW"));

        perform(underwriter, post("/api/v1/bank/applications/{id}/decision", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"outcome\":\"APPROVE\",\"note\":\"Просрочка объяснена, бизнес стабильный\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.summary.status").value("OFFERED"))
                .andExpect(jsonPath("$.application.offer.amount").value(20_000_000));
    }

    @Test
    void zakatIsDeclinedWithExplanation() throws Exception {
        String client = login("zakat");
        long id = createAndSubmit(client, "OVERDRAFT", "cash_gap", 1_000_000, 12, 8_000_000);

        assertThat(runPipeline(client, id)).isEqualTo("DECLINED");
        perform(client, get("/api/v1/applications/{id}", id))
                .andExpect(jsonPath("$.decision.reasons[0]").value("Компания в процессе ликвидации"));
    }

    @Test
    void tishinaGoesToManualReviewWhenBkiTimesOut() throws Exception {
        String client = login("tishina");
        long id = createAndSubmit(client, "CREDIT_LINE", "working_capital", 3_000_000, 24, 30_000_000);

        assertThat(runPipeline(client, id)).isEqualTo("MANUAL_REVIEW");
        perform(login("underwriter"), get("/api/v1/bank/applications/{id}", id))
                .andExpect(jsonPath("$.sources[?(@.step == 'BKI')].status").value("FAILED"))
                .andExpect(jsonPath("$.sources[?(@.step == 'BKI')].attempts").value(3));
    }

    @Test
    void onlyDirectorCanSignContract() throws Exception {
        String accountant = login("romashka-buh");
        long id = createAndSubmit(accountant, "BUSINESS_CREDIT", "investment", 3_000_000, 24, 60_000_000);
        assertThat(runPipeline(accountant, id)).isEqualTo("OFFERED");

        String code = JsonPath.read(body(perform(accountant, post("/api/v1/applications/{id}/offer/accept", id))), "$.signingCode");
        String signBody = "{\"code\":\"%s\"}".formatted(code);
        perform(accountant, post("/api/v1/applications/{id}/offer/sign", id).contentType(MediaType.APPLICATION_JSON).content(signBody))
                .andExpect(status().isForbidden());
        perform(login("romashka"), post("/api/v1/applications/{id}/offer/sign", id).contentType(MediaType.APPLICATION_JSON).content(signBody))
                .andExpect(jsonPath("$.summary.status").value("ISSUED"));
    }

    @Test
    void clientSeesOnlyOwnCompanyAndNoBankApi() throws Exception {
        String romashka = login("romashka");
        long id = createAndSubmit(romashka, "BUSINESS_CREDIT", "investment", 1_000_000, 12, 60_000_000);

        String petrov = login("petrov");
        perform(petrov, get("/api/v1/applications/{id}", id)).andExpect(status().isNotFound());
        perform(petrov, get("/api/v1/bank/applications")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/applications")).andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private String login(String login) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"%s\",\"password\":\"demo\"}".formatted(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(response, "$.accessToken");
    }

    private long createAndSubmit(String token, String product, String purpose, long amount, int term, long revenue) throws Exception {
        String created = body(perform(token, post("/api/v1/applications").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"productCode":"%s","purpose":"%s","amount":%d,"termMonths":%d,"revenue12m":%d}
                        """.formatted(product, purpose, amount, term, revenue)))
                .andExpect(status().isCreated()));
        long id = ((Number) JsonPath.read(created, "$.summary.id")).longValue();
        perform(token, post("/api/v1/applications/{id}/submit", id).contentType(MediaType.APPLICATION_JSON).content("{\"bkiConsent\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.status").value("CHECKING"));
        return id;
    }

    private String runPipeline(String token, long id) throws Exception {
        for (int tick = 0; tick < 20; tick++) {
            pipeline.processTick();
            String status = JsonPath.read(body(perform(token, get("/api/v1/applications/{id}", id))), "$.summary.status");
            if (!"CHECKING".equals(status)) {
                return status;
            }
        }
        throw new AssertionError("Заявка застряла в CHECKING");
    }

    private ResultActions respond(String token, long id, int requestId, String json) throws Exception {
        return perform(token, post("/api/v1/applications/{id}/info-requests/{rid}/response", id, requestId)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    private ResultActions perform(String token, AbstractMockHttpServletRequestBuilder<?> request) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
