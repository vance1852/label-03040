package com.apksigner.service;

import com.apksigner.config.AppConfig;
import com.apksigner.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private AppConfig appConfig;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        appConfig = new AppConfig();
        appConfig.setUploadDir(tempDir.resolve("uploads").toString());
        appConfig.setSignedDir(tempDir.resolve("signed").toString());
        appConfig.setKeystoreDir(tempDir.resolve("keystores").toString());

        fileStorageService = new FileStorageService(appConfig);
        fileStorageService.init();
    }

    @Test
    @DisplayName("上传 APK 文件成功 - 文件应保存到正确位置")
    void testStoreUploadFileSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-app.apk",
                "application/vnd.android.package-archive",
                "fake apk content".getBytes()
        );

        String storedPath = fileStorageService.storeUploadFile(file);

        assertNotNull(storedPath);
        assertTrue(storedPath.endsWith("test-app.apk"));
        assertTrue(storedPath.contains(appConfig.getUploadDir()));

        File savedFile = new File(storedPath);
        assertTrue(savedFile.exists(), "文件应被保存到磁盘");
        assertEquals(file.getSize(), savedFile.length(), "文件大小应一致");
    }

    @Test
    @DisplayName("存储路径生成 - 文件名应包含 UUID 前缀")
    void testStoreUploadFileHasUuidPrefix() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "myapp.apk",
                "application/vnd.android.package-archive",
                "content".getBytes()
        );

        String storedPath = fileStorageService.storeUploadFile(file);
        Path path = Path.of(storedPath);
        String fileName = path.getFileName().toString();

        assertTrue(fileName.endsWith("myapp.apk"));
        assertTrue(fileName.length() > "myapp.apk".length(), "文件名应包含 UUID 前缀");
        assertTrue(fileName.contains("_"), "文件名应包含下划线分隔符");
    }

    @Test
    @DisplayName("非 APK 格式拒绝 - .txt 文件应抛出异常")
    void testValidateApkFileRejectsNonApk() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "not an apk".getBytes()
        );

        BizException exception = assertThrows(BizException.class,
                () -> fileStorageService.storeUploadFile(file));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains(".apk"), "错误信息应提及 APK 格式");
    }

    @Test
    @DisplayName("非 APK 格式拒绝 - 无扩展名文件应抛出异常")
    void testValidateApkFileRejectsNoExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "readme",
                "application/octet-stream",
                "content".getBytes()
        );

        BizException exception = assertThrows(BizException.class,
                () -> fileStorageService.storeUploadFile(file));

        assertEquals(400, exception.getCode());
    }

    @Test
    @DisplayName("非 APK 格式拒绝 - 扩展名大小写不敏感（.APK 应允许）")
    void testValidateApkFileCaseInsensitive() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "APP.APK",
                "application/octet-stream",
                "content".getBytes()
        );

        String storedPath = fileStorageService.storeUploadFile(file);
        assertNotNull(storedPath);
        assertTrue(new File(storedPath).exists());
    }

    @Test
    @DisplayName("空文件拒绝 - 空文件应抛出异常")
    void testValidateApkFileRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.apk",
                "application/octet-stream",
                new byte[0]
        );

        BizException exception = assertThrows(BizException.class,
                () -> fileStorageService.storeUploadFile(file));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("文件"));
    }

    @Test
    @DisplayName("空文件拒绝 - null 文件应抛出异常")
    void testValidateApkFileRejectsNullFile() {
        BizException exception = assertThrows(BizException.class,
                () -> fileStorageService.storeUploadFile(null));

        assertEquals(400, exception.getCode());
    }

    @Test
    @DisplayName("存储路径正确生成 - getSignedFilePath 应返回正确路径")
    void testGetSignedFilePath() {
        String originalPath = tempDir.resolve("uploads").resolve("abc123_test.apk").toString();

        Path signedPath = fileStorageService.getSignedFilePath(originalPath);

        assertEquals(appConfig.getSignedDir(), signedPath.getParent().toString());
        assertEquals("signed_abc123_test.apk", signedPath.getFileName().toString());
    }

    @Test
    @DisplayName("删除文件 - 存在的文件应被删除")
    void testDeleteFileSuccess() throws Exception {
        Path testFile = tempDir.resolve("test_delete.txt");
        Files.writeString(testFile, "content");
        assertTrue(Files.exists(testFile));

        fileStorageService.deleteFile(testFile.toString());

        assertFalse(Files.exists(testFile), "文件应被删除");
    }

    @Test
    @DisplayName("删除文件 - 不存在的文件不应抛出异常")
    void testDeleteFileNotExists() {
        String nonExistentPath = tempDir.resolve("does_not_exist.txt").toString();

        assertDoesNotThrow(() -> fileStorageService.deleteFile(nonExistentPath));
    }

    @Test
    @DisplayName("超大文件拒绝 - 超过 200MB 应抛出异常")
    void testValidateApkFileRejectsOversized() {
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "big.apk",
                "application/octet-stream",
                new byte[0]
        ) {
            @Override
            public long getSize() {
                return 201L * 1024 * 1024;
            }
        };

        BizException exception = assertThrows(BizException.class,
                () -> fileStorageService.validateApkFile(largeFile));

        assertEquals(400, exception.getCode());
    }
}
