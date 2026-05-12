package com.apksigner.controller;

import com.apksigner.BaseTest;
import com.apksigner.dto.SignRequest;
import com.apksigner.entity.SignHistory;
import com.apksigner.mapper.SignHistoryMapper;
import com.apksigner.service.ApkSigningService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SignController 接口层测试")
class SignControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SignHistoryMapper signHistoryMapper;

    @MockBean
    private ApkSigningService apkSigningService;

    @BeforeEach
    void setUp() {
        signHistoryMapper.delete(null);
    }

    @Test
    @DisplayName("上传接口返回正确响应")
    void testUpload_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-app.apk",
                "application/vnd.android.package-archive",
                "fake-apk-content".getBytes()
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
                .andExpect(jsonPath("$.data.fileSize").value(16))
                .andExpect(jsonPath("$.data.filePath").isEmpty())
                .andExpect(jsonPath("$.data.signedFilePath").isEmpty())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains("/uploads/"), "实际上传路径不应在响应中暴露");
        assertFalse(responseBody.contains("/tmp/"), "临时文件路径不应在响应中暴露");
    }

    @Test
    @DisplayName("上传空文件返回 400")
    void testUpload_EmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.apk",
                "application/vnd.android.package-archive",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/sign/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请选择要上传的文件"));
    }

    @Test
    @DisplayName("上传非 APK 文件返回 400")
    void testUpload_NotApk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "content".getBytes()
        );

        mockMvc.perform(multipart("/api/sign/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("仅支持 .apk 格式文件"));
    }

    @Test
    @DisplayName("执行签名接口 - 参数校验失败返回 400")
    void testExecute_ValidationFail() throws Exception {
        SignRequest request = new SignRequest();

        mockMvc.perform(post("/api/sign/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("执行签名接口 - 成功返回脱敏数据")
    void testExecute_Success() throws Exception {
        SignHistory history = createTestHistory();

        SignRequest request = new SignRequest();
        request.setHistoryId(history.getId());
        request.setSignType("RELEASE");
        request.setStorePassword("my-secret-password");
        request.setKeyPassword("my-key-password");

        when(apkSigningService.signApk(
                anyString(), anyString(), anyString(), anyString(), anyInt(), anyString(), anyString()))
                .thenReturn("/tmp/signed.apk");

        MvcResult result = mockMvc.perform(post("/api/sign/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("******"), "密码字段应该被脱敏");
        assertFalse(responseBody.contains("my-secret-password"), "原始密码不应在响应中");
        assertFalse(responseBody.contains("my-key-password"), "原始密钥密码不应在响应中");
    }

    @Test
    @DisplayName("查询不存在的记录返回 404")
    void testGetStatus_NotFound() throws Exception {
        mockMvc.perform(get("/api/sign/status/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("记录不存在"));
    }

    @Test
    @DisplayName("查询存在的记录返回脱敏数据")
    void testGetStatus_Found() throws Exception {
        SignHistory history = createTestHistory();
        history.setStorePassword("encrypted-password");
        history.setKeyPassword("encrypted-key-password");
        signHistoryMapper.updateById(history);

        MvcResult result = mockMvc.perform(get("/api/sign/status/" + history.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(history.getId()))
                .andExpect(jsonPath("$.data.storePassword").value("******"))
                .andExpect(jsonPath("$.data.keyPassword").value("******"))
                .andExpect(jsonPath("$.data.filePath").isEmpty())
                .andExpect(jsonPath("$.data.signedFilePath").isEmpty())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains("encrypted-password"), "加密密码不应明文返回");
        assertFalse(responseBody.contains("/tmp/"), "实际文件路径不应在响应中暴露");
    }

    @Test
    @DisplayName("历史列表分页正常")
    void testHistoryList_Pagination() throws Exception {
        for (int i = 1; i <= 15; i++) {
            SignHistory history = new SignHistory();
            history.setOriginalFilename("app" + i + ".apk");
            history.setFilePath("/tmp/" + i + ".apk");
            history.setFileSize(1000L * i);
            history.setStatus("PENDING");
            history.setSignType("RELEASE");
            history.setDownloadCode("code" + i);
            history.setCreatedAt(LocalDateTime.now().minusMinutes(i));
            signHistoryMapper.insert(history);
        }

        mockMvc.perform(get("/api/history/list")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.pages").value(3))
                .andExpect(jsonPath("$.data.records.length()").value(5));

        mockMvc.perform(get("/api/history/list")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(5));
    }

    @Test
    @DisplayName("历史列表按创建时间倒序")
    void testHistoryList_OrderByDesc() throws Exception {
        for (int i = 1; i <= 3; i++) {
            SignHistory history = new SignHistory();
            history.setOriginalFilename("app" + i + ".apk");
            history.setFilePath("/tmp/" + i + ".apk");
            history.setFileSize(1000L);
            history.setStatus("PENDING");
            history.setSignType("RELEASE");
            history.setDownloadCode("code" + i);
            history.setCreatedAt(LocalDateTime.now().minusMinutes(i));
            signHistoryMapper.insert(history);
            Thread.sleep(10);
        }

        mockMvc.perform(get("/api/history/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].originalFilename").value("app1.apk"))
                .andExpect(jsonPath("$.data.records[1].originalFilename").value("app2.apk"))
                .andExpect(jsonPath("$.data.records[2].originalFilename").value("app3.apk"));
    }

    @Test
    @DisplayName("历史列表密码字段脱敏")
    void testHistoryList_PasswordMasked() throws Exception {
        SignHistory history = createTestHistory();
        history.setStorePassword("encrypted-store-pass");
        history.setKeyPassword("encrypted-key-pass");
        signHistoryMapper.updateById(history);

        mockMvc.perform(get("/api/history/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].storePassword").value("******"))
                .andExpect(jsonPath("$.data.records[0].keyPassword").value("******"));
    }

    private SignHistory createTestHistory() {
        SignHistory history = new SignHistory();
        history.setOriginalFilename("test.apk");
        history.setFilePath("/tmp/test.apk");
        history.setFileSize(1024L);
        history.setStatus("PENDING");
        history.setSignType("RELEASE");
        history.setDownloadCode("test-download-code");
        signHistoryMapper.insert(history);
        return history;
    }
}
