package com.apksigner.service;

import com.apksigner.config.AppConfig;
import com.apksigner.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileStorageService 文件服务测试")
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;
    private AppConfig appConfig;

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
    @DisplayName("上传 APK 文件成功")
    void testStoreUploadFile_Success() throws IOException {
        byte[] content = "fake-apk-content".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.apk",
                "application/vnd.android.package-archive",
                content
        );

        String storedPath = fileStorageService.storeUploadFile(file);

        assertNotNull(storedPath);
        assertTrue(Files.exists(Path.of(storedPath)));
        assertArrayEquals(content, Files.readAllBytes(Path.of(storedPath)));
        assertTrue(storedPath.contains("test.apk"));
    }

    @Test
    @DisplayName("非 APK 格式文件拒绝")
    void testStoreUploadFile_NotApk() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "content".getBytes()
        );

        BizException exception = assertThrows(BizException.class,
                () -> fileStorageService.storeUploadFile(file));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("仅支持 .apk 格式文件"));
    }

    @Test
    @DisplayName("大写 APK 后缀允许")
    void testStoreUploadFile_UpperCaseApk() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "TEST.APK",
                "application/vnd.android.package-archive",
                "content".getBytes()
        );

        String storedPath = fileStorageService.storeUploadFile(file);
        assertNotNull(storedPath);
        assertTrue(storedPath.contains("TEST.APK"));
    }

    @Test
    @DisplayName("空文件拒绝")
    void testStoreUploadFile_EmptyFile() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.apk",
                "application/vnd.android.package-archive",
                new byte[0]
        );

        BizException exception = assertThrows(BizException.class,
                () -> fileStorageService.storeUploadFile(file));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("请选择要上传的文件"));
    }

    @Test
    @DisplayName("null 文件拒绝")
    void testStoreUploadFile_NullFile() {
        BizException exception = assertThrows(BizException.class,
                () -> fileStorageService.storeUploadFile(null));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("请选择要上传的文件"));
    }

    @Test
    @DisplayName("存储路径正确生成 - 包含 UUID 前缀")
    void testStoreUploadFile_PathContainsUuid() {
        MultipartFile file1 = new MockMultipartFile(
                "file",
                "app.apk",
                "application/vnd.android.package-archive",
                "content1".getBytes()
        );
        MultipartFile file2 = new MockMultipartFile(
                "file",
                "app.apk",
                "application/vnd.android.package-archive",
                "content2".getBytes()
        );

        String path1 = fileStorageService.storeUploadFile(file1);
        String path2 = fileStorageService.storeUploadFile(file2);

        assertNotEquals(path1, path2, "相同文件名应该生成不同的存储路径");
        assertTrue(path1.endsWith("app.apk"));
        assertTrue(path2.endsWith("app.apk"));
    }

    @Test
    @DisplayName("签名文件路径正确生成")
    void testGetSignedFilePath() {
        String originalPath = tempDir.resolve("uploads").resolve("abc123_test.apk").toString();
        Path signedPath = fileStorageService.getSignedFilePath(originalPath);

        assertEquals(tempDir.resolve("signed").resolve("signed_abc123_test.apk"), signedPath);
    }

    @Test
    @DisplayName("删除文件成功")
    void testDeleteFile_Exists() throws IOException {
        Path testFile = tempDir.resolve("uploads").resolve("test.apk");
        Files.writeString(testFile, "content");

        fileStorageService.deleteFile(testFile.toString());

        assertFalse(Files.exists(testFile));
    }

    @Test
    @DisplayName("删除不存在的文件不抛异常")
    void testDeleteFile_NotExists() {
        assertDoesNotThrow(() ->
                fileStorageService.deleteFile(tempDir.resolve("nonexistent.apk").toString()));
    }

    @Test
    @DisplayName("文件大小超过限制拒绝")
    void testValidateApkFile_TooLarge() {
        byte[] largeContent = new byte[(int) (201L * 1024 * 1024)];
        MultipartFile file = new MockMultipartFile(
                "file",
                "large.apk",
                "application/vnd.android.package-archive",
                largeContent
        );

        BizException exception = assertThrows(BizException.class,
                () -> fileStorageService.validateApkFile(file));
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("文件大小超过限制"));
    }
}
