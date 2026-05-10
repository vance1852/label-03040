package com.apksigner.controller;

import com.apksigner.service.ApkSigningService;
import com.apksigner.service.FileStorageService;
import com.apksigner.service.SignHistoryService;
import com.apksigner.util.CryptoUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApkSigningService apkSigningService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private SignHistoryService signHistoryService;

    @BeforeEach
    void setUp() {
        CryptoUtil.setKeyForTesting("TestKey!16Bytes!");
    }

    @AfterEach
    void tearDown() {
        CryptoUtil.resetKey();
    }

    @Nested
    @DisplayName("上传接口返回正确响应")
    class UploadTests {

        @Test
        @DisplayName("上传合法 APK 应返回 200 和记录信息")
        void upload_validApk_returns200() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "demo.apk", "application/vnd.android.package-archive",
                    "fake-apk-content".getBytes());

            mockMvc.perform(multipart("/api/sign/upload").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.originalFilename").value("demo.apk"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.downloadCode").isNotEmpty());
        }

        @Test
        @DisplayName("上传记录应包含 fileSize")
        void upload_shouldContainFileSize() throws Exception {
            byte[] content = "hello-world-apk".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "size.apk", "application/octet-stream", content);

            mockMvc.perform(multipart("/api/sign/upload").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.fileSize").value(content.length));
        }
    }

    @Nested
    @DisplayName("参数校验失败返回 400")
    class ValidationTests {

        @Test
        @DisplayName("签名请求缺少 historyId 应返回 400")
        void execute_missingHistoryId_returns400() throws Exception {
            String json = "{\"signType\":\"V2\",\"keyAlias\":\"apk-key\",\"validityYears\":25}";

            mockMvc.perform(post("/api/sign/execute")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("签名请求缺少 signType 应返回 400")
        void execute_missingSignType_returns400() throws Exception {
            String json = "{\"historyId\":1,\"validityYears\":25}";

            mockMvc.perform(post("/api/sign/execute")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("validityYears 超出范围应返回 400")
        void execute_invalidValidityYears_returns400() throws Exception {
            String json = "{\"historyId\":1,\"signType\":\"V2\",\"validityYears\":100}";

            mockMvc.perform(post("/api/sign/execute")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("上传非 APK 文件应返回 400")
        void upload_nonApk_returns400() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "data".getBytes());

            mockMvc.perform(multipart("/api/sign/upload").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    @Nested
    @DisplayName("查询不存在的记录返回 404")
    class NotFoundTests {

        @Test
        @DisplayName("查询不存在的签名记录状态应返回 404")
        void getStatus_nonExistentId_returns404() throws Exception {
            mockMvc.perform(get("/api/sign/status/99999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404))
                    .andExpect(jsonPath("$.message").value("记录不存在"));
        }
    }

    @Nested
    @DisplayName("历史列表分页正常")
    class HistoryPaginationTests {

        @Test
        @DisplayName("空数据时返回分页结果")
        void list_emptyData_returnsPagedResult() throws Exception {
            mockMvc.perform(get("/api/history/list")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.total").isNumber());
        }

        @Test
        @DisplayName("上传后历史列表包含该记录")
        void list_afterUpload_containsRecord() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "page-test.apk", "application/octet-stream",
                    "content".getBytes());

            mockMvc.perform(multipart("/api/sign/upload").file(file))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/history/list")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$.data.records[0].originalFilename").value("page-test.apk"));
        }

        @Test
        @DisplayName("分页参数 size=1 只返回一条记录")
        void list_pageSize1_returnsOneRecord() throws Exception {
            for (int i = 0; i < 3; i++) {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "multi" + i + ".apk", "application/octet-stream",
                        ("content" + i).getBytes());
                fileStorageService.storeUploadFile(file);
                signHistoryService.uploadFile(file);
            }

            mockMvc.perform(get("/api/history/list")
                            .param("page", "1")
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records", hasSize(1)))
                    .andExpect(jsonPath("$.data.size").value(1));
        }
    }

    @Nested
    @DisplayName("密码字段在响应中被脱敏")
    class PasswordMaskingTests {

        @Test
        @DisplayName("上传记录中密码字段应为脱敏值")
        void upload_passwordFieldsAreMasked() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "mask-test.apk", "application/octet-stream",
                    "data".getBytes());

            mockMvc.perform(multipart("/api/sign/upload").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.storePassword").doesNotExist())
                    .andExpect(jsonPath("$.data.keyPassword").doesNotExist());
        }

        @Test
        @DisplayName("查询签名状态时密码字段应被脱敏")
        void getStatus_passwordFieldsAreMasked() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "status-mask.apk", "application/octet-stream",
                    "data".getBytes());

            String response = mockMvc.perform(multipart("/api/sign/upload").file(file))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            Map<?, ?> data = objectMapper.readTree(response).get("data").isEmpty() ? null
                    : objectMapper.convertValue(objectMapper.readTree(response).get("data"), Map.class);
            Integer id = data != null ? (Integer) data.get("id") : null;

            if (id != null) {
                mockMvc.perform(get("/api/sign/status/" + id))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.storePassword", not(containsString("android"))))
                        .andExpect(jsonPath("$.data.filePath").doesNotExist())
                        .andExpect(jsonPath("$.data.signedFilePath").doesNotExist());
            }
        }

        @Test
        @DisplayName("历史列表中密码字段应被脱敏")
        void historyList_passwordFieldsAreMasked() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "hist-mask.apk", "application/octet-stream",
                    "data".getBytes());

            mockMvc.perform(multipart("/api/sign/upload").file(file))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/history/list")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records[0].filePath").doesNotExist())
                    .andExpect(jsonPath("$.data.records[0].signedFilePath").doesNotExist());
        }
    }
}
