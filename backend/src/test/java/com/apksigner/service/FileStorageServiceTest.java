package com.apksigner.service;

import com.apksigner.config.AppConfig;
import com.apksigner.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private AppConfig appConfig;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        appConfig = new AppConfig();
        Path uploadDir = tempDir.resolve("uploads");
        Path signedDir = tempDir.resolve("signed");
        Path keystoreDir = tempDir.resolve("keystores");
        Files.createDirectories(uploadDir);
        Files.createDirectories(signedDir);
        Files.createDirectories(keystoreDir);

        appConfig.setUploadDir(uploadDir.toString());
        appConfig.setSignedDir(signedDir.toString());
        appConfig.setKeystoreDir(keystoreDir.toString());
        appConfig.setFileRetentionHours(1);

        fileStorageService = new FileStorageService(appConfig);
        fileStorageService.init();
    }

    @Nested
    @DisplayName("上传 APK 成功")
    class UploadApkTests {

        @Test
        @DisplayName("上传合法 APK 文件应成功并返回存储路径")
        void storeUploadFile_validApk_success() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.apk", "application/vnd.android.package-archive",
                    "fake-apk-content".getBytes());

            String storedPath = fileStorageService.storeUploadFile(file);

            assertNotNull(storedPath);
            assertTrue(storedPath.contains("test.apk"));
            assertTrue(Files.exists(Path.of(storedPath)));
        }

        @Test
        @DisplayName("上传后文件内容应完整保存")
        void storeUploadFile_contentPreserved() throws IOException {
            byte[] content = "hello apk world".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "content.apk", "application/octet-stream", content);

            String storedPath = fileStorageService.storeUploadFile(file);
            byte[] saved = Files.readAllBytes(Path.of(storedPath));
            assertArrayEquals(content, saved);
        }
    }

    @Nested
    @DisplayName("非 APK 格式拒绝")
    class NonApkRejectionTests {

        @Test
        @DisplayName("上传 .txt 文件应抛出 BizException")
        void storeUploadFile_txtFile_throwsBizException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "data".getBytes());

            BizException ex = assertThrows(BizException.class,
                    () -> fileStorageService.storeUploadFile(file));
            assertEquals(400, ex.getCode());
            assertTrue(ex.getMessage().contains(".apk"));
        }

        @Test
        @DisplayName("上传 .zip 文件应抛出 BizException")
        void storeUploadFile_zipFile_throwsBizException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "archive.zip", "application/zip", "data".getBytes());

            BizException ex = assertThrows(BizException.class,
                    () -> fileStorageService.storeUploadFile(file));
            assertEquals(400, ex.getCode());
        }

        @Test
        @DisplayName("文件名无扩展名应抛出 BizException")
        void storeUploadFile_noExtension_throwsBizException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "noextension", "application/octet-stream", "data".getBytes());

            BizException ex = assertThrows(BizException.class,
                    () -> fileStorageService.storeUploadFile(file));
            assertEquals(400, ex.getCode());
        }

        @Test
        @DisplayName("大写 .APK 扩展名应被接受")
        void storeUploadFile_uppercaseApk_accepted() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "TEST.APK", "application/octet-stream", "data".getBytes());

            String storedPath = assertDoesNotThrow(() -> fileStorageService.storeUploadFile(file));
            assertNotNull(storedPath);
        }
    }

    @Nested
    @DisplayName("空文件拒绝")
    class EmptyFileTests {

        @Test
        @DisplayName("上传空文件应抛出 BizException")
        void storeUploadFile_emptyFile_throwsBizException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "empty.apk", "application/octet-stream", new byte[0]);

            BizException ex = assertThrows(BizException.class,
                    () -> fileStorageService.storeUploadFile(file));
            assertEquals(400, ex.getCode());
        }

        @Test
        @DisplayName("传入 null MultipartFile 应抛出 BizException")
        void storeUploadFile_nullFile_throwsBizException() {
            BizException ex = assertThrows(BizException.class,
                    () -> fileStorageService.storeUploadFile(null));
            assertEquals(400, ex.getCode());
        }
    }

    @Nested
    @DisplayName("存储路径正确生成")
    class PathGenerationTests {

        @Test
        @DisplayName("存储路径应包含 uploadDir 前缀")
        void storeUploadFile_pathStartsWithUploadDir() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "path-test.apk", "application/octet-stream", "data".getBytes());

            String storedPath = fileStorageService.storeUploadFile(file);
            assertTrue(storedPath.startsWith(appConfig.getUploadDir()));
        }

        @Test
        @DisplayName("存储文件名应包含 UUID 前缀以避免冲突")
        void storeUploadFile_filenameContainsUuidPrefix() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "uuid-test.apk", "application/octet-stream", "data".getBytes());

            String storedPath = fileStorageService.storeUploadFile(file);
            String filename = Path.of(storedPath).getFileName().toString();
            assertTrue(filename.endsWith("_uuid-test.apk"));
            assertTrue(filename.length() > "uuid-test.apk".length());
        }

        @Test
        @DisplayName("两次上传同名文件应生成不同存储路径")
        void storeUploadFile_sameName_differentPaths() {
            MockMultipartFile file1 = new MockMultipartFile(
                    "file", "same.apk", "application/octet-stream", "data1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "file", "same.apk", "application/octet-stream", "data2".getBytes());

            String path1 = fileStorageService.storeUploadFile(file1);
            String path2 = fileStorageService.storeUploadFile(file2);

            assertNotEquals(path1, path2);
        }

        @Test
        @DisplayName("getSignedFilePath 应生成正确的签名文件路径")
        void getSignedFilePath_correctPath() {
            String originalPath = appConfig.getUploadDir() + "/abc_test.apk";
            Path signedPath = fileStorageService.getSignedFilePath(originalPath);

            assertEquals(appConfig.getSignedDir(), signedPath.getParent().toString());
            assertTrue(signedPath.getFileName().toString().startsWith("signed_"));
            assertTrue(signedPath.getFileName().toString().endsWith("abc_test.apk"));
        }
    }
}
