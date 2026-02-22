package com.apksigner.service;

import com.apksigner.config.AppConfig;
import com.apksigner.exception.BizException;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileStorageService 单元测试：文件校验、存储、清理
 */
class FileStorageServiceTest {

    private FileStorageService service;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("apk-test");
        AppConfig config = new AppConfig();
        config.setUploadDir(tempDir.resolve("uploads").toString());
        config.setSignedDir(tempDir.resolve("signed").toString());
        config.setKeystoreDir(tempDir.resolve("keystores").toString());
        config.setFileRetentionHours(1);

        service = new FileStorageService(config);
        service.init();
    }

    @AfterEach
    void tearDown() throws IOException {
        // 清理临时目录
        Files.walk(tempDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
    }

    @Test
    @DisplayName("上传合法 APK 文件 - 成功")
    void storeUploadFile_validApk_shouldSucceed() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "app.apk", "application/octet-stream",
                "fake-apk-content".getBytes());

        String path = service.storeUploadFile(file);

        assertNotNull(path);
        assertTrue(Files.exists(Path.of(path)), "文件应存在于磁盘");
        assertTrue(path.endsWith("app.apk"));
    }

    @Test
    @DisplayName("上传非 APK 文件 - 应拒绝")
    void validateApkFile_nonApk_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "readme.txt", "text/plain", "text".getBytes());

        BizException ex = assertThrows(BizException.class,
                () -> service.validateApkFile(file));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains(".apk"));
    }

    @Test
    @DisplayName("上传空文件 - 应拒绝")
    void validateApkFile_empty_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.apk", "application/octet-stream", new byte[0]);

        BizException ex = assertThrows(BizException.class,
                () -> service.validateApkFile(file));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("上传 null 文件 - 应拒绝")
    void validateApkFile_null_shouldThrow() {
        assertThrows(BizException.class, () -> service.validateApkFile(null));
    }

    @Test
    @DisplayName("签名文件路径生成")
    void getSignedFilePath_shouldPrefixWithSigned() {
        Path result = service.getSignedFilePath("/data/uploads/uuid_app.apk");
        assertTrue(result.getFileName().toString().startsWith("signed_"));
    }

    @Test
    @DisplayName("删除文件 - 不应抛异常")
    void deleteFile_nonExistent_shouldNotThrow() {
        assertDoesNotThrow(() -> service.deleteFile("/nonexistent/path.apk"));
    }

    @Test
    @DisplayName("清理过期文件 - 不应抛异常")
    void cleanExpiredFiles_shouldNotThrow() {
        assertDoesNotThrow(() -> service.cleanExpiredFiles());
    }
}
