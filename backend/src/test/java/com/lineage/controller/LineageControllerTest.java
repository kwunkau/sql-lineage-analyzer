package com.lineage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lineage.dto.request.AnalyzeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LineageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/lineage/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("SQL Lineage Analyzer is running"));
    }

    @Test
    void testAnalyzeSuccess() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setSql("SELECT id, name FROM users");
        request.setDbType("mysql");

        mockMvc.perform(post("/api/lineage/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.tables[0]").value("users"))
                .andExpect(jsonPath("$.data.fieldDependencies").isArray())
                .andExpect(jsonPath("$.data.fieldDependencies.length()").value(2));
    }

    @Test
    void testAnalyzeWithAggregation() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setSql("SELECT COUNT(*) AS total FROM users");
        request.setDbType("mysql");

        mockMvc.perform(post("/api/lineage/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.fieldDependencies[0].aggregation").value(true));
    }

    @Test
    void testAnalyzeWithBlankSql() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setSql("");
        request.setDbType("mysql");

        mockMvc.perform(post("/api/lineage/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAnalyzeWithBlankDbType() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setSql("SELECT * FROM users");
        request.setDbType("");

        mockMvc.perform(post("/api/lineage/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAnalyzeWithInvalidSQL() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setSql("SELEC * FROM users"); // 拼写错误
        request.setDbType("mysql");

        mockMvc.perform(post("/api/lineage/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testAnalyzeWithNonSelectStatement() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setSql("INSERT INTO users (id, name) VALUES (1, 'test')");
        request.setDbType("mysql");

        mockMvc.perform(post("/api/lineage/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Only SELECT statements are supported"));
    }

    @Test
    void testAnalyzeWithDifferentDbTypes() throws Exception {
        String sql = "SELECT id, name FROM users";

        AnalyzeRequest mysqlRequest = new AnalyzeRequest();
        mysqlRequest.setSql(sql);
        mysqlRequest.setDbType("mysql");

        mockMvc.perform(post("/api/lineage/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mysqlRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));

        AnalyzeRequest hiveRequest = new AnalyzeRequest();
        hiveRequest.setSql(sql);
        hiveRequest.setDbType("hive");

        mockMvc.perform(post("/api/lineage/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hiveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    void testAnalyzeWithComplexSQL() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setSql("SELECT u.id AS user_id, u.name AS user_name FROM users u");
        request.setDbType("mysql");

        mockMvc.perform(post("/api/lineage/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.tables[0]").value("users"))
                .andExpect(jsonPath("$.data.fieldDependencies.length()").value(2));
    }
}
