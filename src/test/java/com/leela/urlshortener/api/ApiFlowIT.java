package com.leela.urlshortener.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leela.urlshortener.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiFlowIT {

    private static final String API_KEY = "test-test-test-key";
    private final JsonMapper json = JsonMapper.shared();

    @Autowired
    private MockMvc mvc;

    @Test
    void healthDoesNotRequireAuth() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void createRequiresApiKey() throws Exception {
        mvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://example.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnsafeDestination() throws Exception {
        mvc.perform(post("/api/v1/links")
                        .header("Authorization", "Bearer " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createRedirectStatsAndDelete() throws Exception {
        MvcResult created = mvc.perform(post("/api/v1/links")
                        .header("Authorization", "Bearer " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://example.com/launch\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/launch"))
                .andReturn();

        String code = json.readTree(created.getResponse().getContentAsString()).get("code").asString();

        mvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/launch"))
                .andExpect(header().string("Cache-Control", "private, no-store"));

        mvc.perform(get("/api/v1/links/" + code).header("Authorization", "Bearer " + API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code));

        mvc.perform(delete("/api/v1/links/" + code).header("Authorization", "Bearer " + API_KEY))
                .andExpect(status().isNoContent());

        mvc.perform(get("/" + code)).andExpect(status().isNotFound());
    }

    @Test
    void customAliasIsAccepted() throws Exception {
        mvc.perform(post("/api/v1/links")
                        .header("Authorization", "Bearer " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://example.com/alias\",\"alias\":\"launch-day\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("launch-day"))
                .andExpect(jsonPath("$.customAlias").value(true));

        mvc.perform(get("/launch-day"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/alias"));
    }
}
