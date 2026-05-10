package com.apksigner.controller;

import com.apksigner.config.AppConfig;
import com.apksigner.dto.R;
import com.apksigner.dto.SignRequest;
import com.apksigner.entity.SignHistory;
import com.apksigner.service.ApkSigningService;
import com.apksigner.service.FileStorageService;
import com.apksigner.service.SignHistoryService;
import com.apksigner.util.CryptoUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppConfig appConfig;

    @MockBean
    private ApkSigningService apkSigningService;

    @Autowired
    private SignHistoryService signHistoryService;

    @Autowired
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() throws Exception {
        CryptoUtil.setKeyForTesting("TestKey!16Bytes!");
        Files.createDirectories(Path.of(appConfig.getUploadDir()));
        Files.createDirectories(Path.of(appConfig.getSignedDir()));
        Files.createDirectories(Path.of(appConfig.getKeystoreDir()));
    }

    @AfterEach
    void tearDown() {
        CryptoUtil.resetKey();
    }

    @Test
    @DisplayName("上传接口 - 上传 APK 文件应返回 200 和正确数据")
    void testUploadApkSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-app.apk",
                "application/vnd.android.package-archive",
                "fake apk content".getBytes()
        );

        MvcResult result = mockMvc.perform(multipart("/api/sign/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.originalFilename").value("test-app.apk"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.downloadCode").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("上传响应: " + response);
    }

    @Test
    @DisplayName("上传接口 - 上传非 APK 文件应返回 400 错误")
    void testUploadNonApkRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "not an apk".getBytes()
        );

        mockMvc.perform(multipart("/api/sign/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("上传接口 - 空文件应返回 400 错误")
    void testUploadEmptyFileRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.apk",
                "application/octet-stream",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/sign/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("执行签名 - 参数校验失败应返回 400")
    void testExecuteSignValidationFailure() throws Exception {
        SignRequest request = new SignRequest();
        request.setHistoryId(null);
        request.setSignType("");

        mockMvc.perform(post("/api/sign/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("执行签名 - 有效期小于 1 年应返回 400")
    void testExecuteSignValidityTooShort() throws Exception {
        SignRequest request = new SignRequest();
        request.setHistoryId(1L);
        request.setSignType("RELEASE");
        request.setValidityYears(0);

        mockMvc.perform(post("/api/sign/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("执行签名 - Mock 签名服务验证流程")
    void testExecuteSignWithMockedService() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-sign.apk",
                "application/vnd.android.package-archive",
                "apk content".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/sign/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andReturn();

        R<?> uploadR = objectMapper.readValue(uploadResult.getResponse().getContentAsString(), R.class);
        Integer historyId = (Integer) ((java.util.Map<String, Object>) uploadR.getData()).get("id");

        when(apkSigningService.signApk(
                anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyString()
        )).thenReturn("/fake/output/path.apk");

        SignRequest request = new SignRequest();
        request.setHistoryId(historyId.longValue());
        request.setSignType("RELEASE");
        request.setKeyAlias("my-key");
        request.setValidityYears(25);
        request.setStorePassword("my-store-pass");
        request.setKeyPassword("my-key-pass");

        MvcResult result = mockMvc.perform(post("/api/sign/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.signedFilename").value("signed_test-sign.apk"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("签名响应: " + response);
    }

    @Test
    @DisplayName("状态查询 - 不存在的记录应返回 404")
    void testGetStatusNotFound() throws Exception {
        mockMvc.perform(get("/api/sign/status/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("记录不存在"));
    }

    @Test
    @DisplayName("状态查询 - 密码字段应被脱敏为 ******")
    void testGetStatusPasswordMasked() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "mask-test.apk",
                "application/vnd.android.package-archive",
                "content".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/sign/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andReturn();

        R<?> uploadR = objectMapper.readValue(uploadResult.getResponse().getContentAsString(), R.class);
        Integer historyId = (Integer) ((java.util.Map<String, Object>) uploadR.getData()).get("id");

        when(apkSigningService.signApk(
                anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyString()
        )).thenReturn("/fake/path.apk");

        SignRequest request = new SignRequest();
        request.setHistoryId(historyId.longValue());
        request.setSignType("RELEASE");
        request.setStorePassword("super-secret-123");
        request.setKeyPassword("another-secret-456");

        mockMvc.perform(post("/api/sign/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sign/status/" + historyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.storePassword").value("******"))
                .andExpect(jsonPath("$.data.keyPassword").value("******"));
    }

    @Test
    @DisplayName("历史列表 - 分页正常，默认第1页10条")
    void testHistoryListPaginationDefault() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "app-" + i + ".apk",
                    "application/vnd.android.package-archive",
                    ("content-" + i).getBytes()
            );
            mockMvc.perform(multipart("/api/sign/upload").file(file)).andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/history/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records.length()").value(5));
    }

    @Test
    @DisplayName("历史列表 - 自定义分页参数 page=2, size=2")
    void testHistoryListCustomPagination() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "paginate-" + i + ".apk",
                    "application/vnd.android.package-archive",
                    ("content-" + i).getBytes()
            );
            mockMvc.perform(multipart("/api/sign/upload").file(file)).andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/history/list")
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2));
    }

    @Test
    @DisplayName("历史列表 - 所有记录的密码字段都应脱敏")
    void testHistoryListAllPasswordsMasked() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "masked-" + i + ".apk",
                    "application/vnd.android.package-archive",
                    ("content-" + i).getBytes()
            );
            mockMvc.perform(multipart("/api/sign/upload").file(file)).andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/history/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].storePassword").value("******"))
                .andExpect(jsonPath("$.data.records[0].keyPassword").value("******"));
    }

    @Test
    @DisplayName("状态查询 - 文件路径不应暴露给前端")
    void testGetStatusFilePathNotExposed() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "path-test.apk",
                "application/vnd.android.package-archive",
                "content".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/sign/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andReturn();

        R<?> uploadR = objectMapper.readValue(uploadResult.getResponse().getContentAsString(), R.class);
        Integer historyId = (Integer) ((java.util.Map<String, Object>) uploadR.getData()).get("id");

        mockMvc.perform(get("/api/sign/status/" + historyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.filePath").doesNotExist())
                .andExpect(jsonPath("$.data.signedFilePath").doesNotExist());
    }

    @Test
    @DisplayName("批量签名 - 参数校验失败应返回 400")
    void testBatchSignValidationFailure() throws Exception {
        String body = "{\"historyIds\":[],\"signType\":\"\"}";

        mockMvc.perform(post("/api/sign/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
