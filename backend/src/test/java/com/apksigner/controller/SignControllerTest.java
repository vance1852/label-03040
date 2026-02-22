package com.apksigner.controller;

import com.apksigner.util.CryptoUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SignController 集成测试：文件上传、参数校验、安全脱敏
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_KEY = "IntTestKey!16byt";

    @BeforeAll
    static void initKey() {
        CryptoUtil.setKeyForTesting(TEST_KEY);
    }

    @AfterAll
    static void cleanKey() {
        CryptoUtil.resetKey();
    }

    @Test
    @Order(1)
    @DisplayName("上传 APK 文件 - 成功")
    void upload_validApk_shouldReturn200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.apk", "application/octet-stream",
                "PK\003\004fake-apk-content".getBytes());

        MvcResult result = mockMvc.perform(multipart("/api/sign/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.originalFilename").value("test.apk"))
                // 验证敏感字段已脱敏
                .andExpect(jsonPath("$.data.filePath").doesNotExist())
                .andExpect(jsonPath("$.data.signedFilePath").doesNotExist())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        Assertions.assertNotNull(node.get("data").get("downloadCode").asText());
    }

    @Test
    @Order(2)
    @DisplayName("上传非 APK 文件 - 应拒绝")
    void upload_nonApk_shouldReturn400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "readme.txt", "text/plain",
                "not an apk".getBytes());

        mockMvc.perform(multipart("/api/sign/upload").file(file))
                .andExpect(status().isOk()) // GlobalExceptionHandler 返回 200 + code:400
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("仅支持 .apk 格式文件"));
    }

    @Test
    @Order(3)
    @DisplayName("上传空文件 - 应拒绝")
    void upload_emptyFile_shouldReturn400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.apk", "application/octet-stream",
                new byte[0]);

        mockMvc.perform(multipart("/api/sign/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(4)
    @DisplayName("签名请求参数校验 - signType 为空")
    void execute_emptySignType_shouldReturn400() throws Exception {
        String body = """
                {"historyId": 1, "signType": "", "keyAlias": "k", "validityYears": 10,
                 "storePassword": "p", "keyPassword": "p"}
                """;

        mockMvc.perform(post("/api/sign/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(5)
    @DisplayName("签名请求参数校验 - validityYears 超出范围")
    void execute_invalidValidity_shouldReturn400() throws Exception {
        String body = """
                {"historyId": 1, "signType": "V1", "keyAlias": "k", "validityYears": 100,
                 "storePassword": "p", "keyPassword": "p"}
                """;

        mockMvc.perform(post("/api/sign/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(6)
    @DisplayName("查询不存在的记录 - 应返回 404")
    void getStatus_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/sign/status/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("记录不存在"));
    }

    @Test
    @Order(7)
    @DisplayName("历史记录查询 - 应返回分页数据")
    void historyList_shouldReturnPaginated() throws Exception {
        mockMvc.perform(get("/api/history/list").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    @Order(8)
    @DisplayName("历史记录中密码字段应脱敏")
    void historyList_passwordsShouldBeMasked() throws Exception {
        // 先上传一个文件确保有记录
        MockMultipartFile file = new MockMultipartFile(
                "file", "mask-test.apk", "application/octet-stream",
                "PK\003\004fake".getBytes());
        mockMvc.perform(multipart("/api/sign/upload").file(file))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/history/list")
                        .param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode records = objectMapper.readTree(body).get("data").get("records");
        for (JsonNode record : records) {
            // filePath 应为 null
            Assertions.assertTrue(
                    record.get("filePath").isNull(),
                    "filePath 不应暴露给前端");
            Assertions.assertTrue(
                    record.get("signedFilePath").isNull(),
                    "signedFilePath 不应暴露给前端");
        }
    }

    @Test
    @Order(9)
    @DisplayName("下载不存在的文件 - 应返回错误")
    void download_notFound_shouldFail() throws Exception {
        mockMvc.perform(get("/api/file/download/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @Order(10)
    @DisplayName("无效下载码 - 应返回 404")
    void downloadByCode_invalid_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/file/download/code/invalid_code_here"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("下载码无效"));
    }
}
