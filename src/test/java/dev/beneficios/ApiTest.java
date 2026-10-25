package dev.beneficios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String openWallet() throws Exception {
        String body = mvc.perform(post("/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiaryName\":\"Maria\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asText();
    }

    private void credit(String walletId, String key, String category, long cents) throws Exception {
        mvc.perform(post("/wallets/" + walletId + "/credits").header("Idempotency-Key", walletId + ":" + key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"" + category + "\",\"amountCents\":" + cents + "}"))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions authorize(String walletId, String key,
                                                                         String merchant, long cents) throws Exception {
        return mvc.perform(post("/authorizations").header("Idempotency-Key", walletId + ":" + key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"walletId\":\"" + walletId + "\",\"merchantType\":\"" + merchant
                        + "\",\"amountCents\":" + cents + "}"));
    }

    @Test
    void ca_01_1_opensWalletWithZeroBalances() throws Exception {
        mvc.perform(post("/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiaryName\":\"Maria\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balancesCents.REFEICAO").value(0))
                .andExpect(jsonPath("$.balancesCents.ALIMENTACAO").value(0));
    }

    @Test
    void ca_01_2_blankNameIs400ProblemDetails() throws Exception {
        mvc.perform(post("/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiaryName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void ca_02_1_and_02_4_creditIsIdempotent() throws Exception {
        String id = openWallet();

        credit(id, "c1", "REFEICAO", 50_000);
        credit(id, "c1", "REFEICAO", 50_000);

        mvc.perform(get("/wallets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balancesCents.REFEICAO").value(50_000))
                .andExpect(jsonPath("$.balancesCents.ALIMENTACAO").value(0));
    }

    @Test
    void ca_02_3_zeroCreditIs400() throws Exception {
        String id = openWallet();

        mvc.perform(post("/wallets/" + id + "/credits").header("Idempotency-Key", id + ":c")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"REFEICAO\",\"amountCents\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creditWithoutKeyIs400() throws Exception {
        String id = openWallet();

        mvc.perform(post("/wallets/" + id + "/credits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"REFEICAO\",\"amountCents\":100}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creditOnUnknownWalletIs404() throws Exception {
        mvc.perform(post("/wallets/" + UUID.randomUUID() + "/credits").header("Idempotency-Key", "x:c")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"REFEICAO\",\"amountCents\":100}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void creditSameKeyOtherAmountIs422() throws Exception {
        String id = openWallet();
        credit(id, "c1", "REFEICAO", 100);

        mvc.perform(post("/wallets/" + id + "/credits").header("Idempotency-Key", id + ":c1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"REFEICAO\",\"amountCents\":200}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void ca_05_2_unknownWalletIs404() throws Exception {
        mvc.perform(get("/wallets/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void ca_03_1_approvesAndDebits() throws Exception {
        String id = openWallet();
        credit(id, "c1", "REFEICAO", 10_000);

        authorize(id, "a1", "RESTAURANTE", 3_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(get("/wallets/" + id)).andExpect(jsonPath("$.balancesCents.REFEICAO").value(7_000));
    }

    @Test
    void ca_03_2_deniesInsufficientBalance() throws Exception {
        String id = openWallet();
        credit(id, "c1", "REFEICAO", 10_000);

        authorize(id, "a1", "RESTAURANTE", 10_001)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DENIED"))
                .andExpect(jsonPath("$.denialReason").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    void ca_03_3_deniesOtherMerchants() throws Exception {
        String id = openWallet();

        authorize(id, "a1", "OUTROS", 100)
                .andExpect(jsonPath("$.denialReason").value("MERCHANT_NOT_ACCEPTED"));
    }

    @Test
    void ca_03_4_deniesWhenAntifraudRejects() throws Exception {
        String id = openWallet();
        credit(id, "c1", "ALIMENTACAO", 500_000);

        // The simulated antifraud denies purchases above R$ 1.000,00.
        authorize(id, "a1", "SUPERMERCADO", 100_001)
                .andExpect(jsonPath("$.denialReason").value("FRAUD_SUSPECTED"));
        mvc.perform(get("/wallets/" + id)).andExpect(jsonPath("$.balancesCents.ALIMENTACAO").value(500_000));
    }

    @Test
    void ca_03_5_and_03_6_idempotency() throws Exception {
        String id = openWallet();
        credit(id, "c1", "REFEICAO", 10_000);

        String first = authorize(id, "a1", "RESTAURANTE", 3_000).andReturn().getResponse().getContentAsString();
        String second = authorize(id, "a1", "RESTAURANTE", 3_000).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(second).isEqualTo(first);

        authorize(id, "a1", "RESTAURANTE", 4_000).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/wallets/" + id)).andExpect(jsonPath("$.balancesCents.REFEICAO").value(7_000));
    }

    @Test
    void authorizeOnUnknownWalletIs404() throws Exception {
        authorize(UUID.randomUUID().toString(), "a1", "RESTAURANTE", 100).andExpect(status().isNotFound());
    }

    @Test
    void ca_04_reversalFlow() throws Exception {
        String id = openWallet();
        credit(id, "c1", "REFEICAO", 10_000);
        JsonNode approved = json.readTree(authorize(id, "a1", "RESTAURANTE", 3_000)
                .andReturn().getResponse().getContentAsString());
        String authId = approved.get("id").asText();

        mvc.perform(post("/authorizations/" + authId + "/reversal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVERSED"));
        mvc.perform(get("/wallets/" + id)).andExpect(jsonPath("$.balancesCents.REFEICAO").value(10_000));

        mvc.perform(post("/authorizations/" + authId + "/reversal")).andExpect(status().isConflict());
        mvc.perform(get("/wallets/" + id)).andExpect(jsonPath("$.balancesCents.REFEICAO").value(10_000));
    }

    @Test
    void ca_04_3_deniedReversalIs409() throws Exception {
        String id = openWallet();
        JsonNode denied = json.readTree(authorize(id, "a1", "OUTROS", 100)
                .andReturn().getResponse().getContentAsString());

        mvc.perform(post("/authorizations/" + denied.get("id").asText() + "/reversal"))
                .andExpect(status().isConflict());
    }

    @Test
    void ca_04_4_unknownReversalIs404() throws Exception {
        mvc.perform(post("/authorizations/" + UUID.randomUUID() + "/reversal")).andExpect(status().isNotFound());
    }

    @Test
    void openApiIsPublished() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/authorizations']").exists());
    }
}
