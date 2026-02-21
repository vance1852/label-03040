package com.apksigner.service;

import com.apksigner.config.AppConfig;
import com.apksigner.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApkSigningService {

    private final AppConfig appConfig;

    /**
     * 执行APK签名
     * 使用 apksigner (Android SDK Build Tools) 进行签名
     * 如果环境中没有 apksigner，则使用 jarsigner 作为后备方案
     */
    public String signApk(String inputPath, String outputPath, String signType,
                          String keyAlias, int validityYears,
                          String storePassword, String keyPassword) {
        log.info("开始签名: type={}, input={}", signType, inputPath);

        Path keystorePath = generateKeystore(keyAlias, validityYears, storePassword, keyPassword);

        try {
            // 先复制文件到输出路径
            Files.copy(Path.of(inputPath), Path.of(outputPath), StandardCopyOption.REPLACE_EXISTING);

            switch (signType.toUpperCase()) {
                case "DEBUG":
                    signWithDebugKey(inputPath, outputPath);
                    break;
                case "V1":
                    signWithJarsigner(outputPath, keystorePath.toString(), keyAlias, storePassword, keyPassword);
                    break;
                case "V2":
                    signWithApksigner(inputPath, outputPath, keystorePath.toString(), keyAlias, storePassword, keyPassword, true, false);
                    break;
                case "V3":
                    signWithApksigner(inputPath, outputPath, keystorePath.toString(), keyAlias, storePassword, keyPassword, true, true);
                    break;
                case "RELEASE":
                default:
                    signWithApksigner(inputPath, outputPath, keystorePath.toString(), keyAlias, storePassword, keyPassword, true, false);
                    break;
            }

            log.info("签名完成: output={}", outputPath);
            return outputPath;
        } catch (Exception e) {
            log.error("签名失败: {}", e.getMessage(), e);
            // 清理失败的输出文件
            try { Files.deleteIfExists(Path.of(outputPath)); } catch (Exception ignored) {}
            throw new BizException("签名失败: " + e.getMessage());
        }
    }

    private Path generateKeystore(String keyAlias, int validityYears,
                                   String storePassword, String keyPassword) {
        Path keystorePath = Path.of(appConfig.getKeystoreDir(), keyAlias + ".jks");
        if (Files.exists(keystorePath)) {
            return keystorePath;
        }

        log.info("生成新的 Keystore: alias={}", keyAlias);
        List<String> cmd = new ArrayList<>(List.of(
                "keytool", "-genkeypair",
                "-alias", keyAlias,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", String.valueOf(validityYears * 365),
                "-keystore", keystorePath.toString(),
                "-storepass", storePassword,
                "-keypass", keyPassword,
                "-dname", "CN=APK Signer, OU=Dev, O=ApkSigner, L=Beijing, ST=Beijing, C=CN"
        ));

        executeCommand(cmd, 30);
        return keystorePath;
    }

    private void signWithDebugKey(String inputPath, String outputPath) {
        // 使用 debug keystore 签名
        Path debugKeystore = Path.of(appConfig.getKeystoreDir(), "debug.keystore");
        if (!Files.exists(debugKeystore)) {
            List<String> cmd = List.of(
                    "keytool", "-genkeypair",
                    "-alias", "androiddebugkey",
                    "-keyalg", "RSA", "-keysize", "2048",
                    "-validity", "36500",
                    "-keystore", debugKeystore.toString(),
                    "-storepass", "android",
                    "-keypass", "android",
                    "-dname", "CN=Android Debug, OU=Debug, O=Android, L=Unknown, ST=Unknown, C=US"
            );
            executeCommand(cmd, 30);
        }
        signWithJarsigner(outputPath, debugKeystore.toString(), "androiddebugkey", "android", "android");
    }

    private void signWithJarsigner(String apkPath, String keystorePath,
                                    String keyAlias, String storePassword, String keyPassword) {
        // 先用 zipalign 对齐（如果可用）
        tryZipalign(apkPath);

        List<String> cmd = List.of(
                "jarsigner",
                "-verbose",
                "-sigalg", "SHA256withRSA",
                "-digestalg", "SHA-256",
                "-keystore", keystorePath,
                "-storepass", storePassword,
                "-keypass", keyPassword,
                apkPath,
                keyAlias
        );
        executeCommand(cmd, 120);
    }

    private void signWithApksigner(String inputPath, String outputPath,
                                    String keystorePath, String keyAlias,
                                    String storePassword, String keyPassword,
                                    boolean v2Enabled, boolean v3Enabled) {
        // 尝试使用 apksigner
        if (isCommandAvailable("apksigner")) {
            List<String> cmd = new ArrayList<>(List.of(
                    "apksigner", "sign",
                    "--ks", keystorePath,
                    "--ks-key-alias", keyAlias,
                    "--ks-pass", "pass:" + storePassword,
                    "--key-pass", "pass:" + keyPassword,
                    "--v1-signing-enabled", "true",
                    "--v2-signing-enabled", String.valueOf(v2Enabled),
                    "--v3-signing-enabled", String.valueOf(v3Enabled),
                    "--out", outputPath,
                    inputPath
            ));
            executeCommand(cmd, 120);
        } else {
            // 回退到 jarsigner
            log.warn("apksigner 不可用，回退到 jarsigner (仅支持V1签名)");
            signWithJarsigner(outputPath, keystorePath, keyAlias, storePassword, keyPassword);
        }
    }

    private void tryZipalign(String apkPath) {
        if (!isCommandAvailable("zipalign")) {
            log.debug("zipalign 不可用，跳过对齐步骤");
            return;
        }
        String alignedPath = apkPath + ".aligned";
        try {
            List<String> cmd = List.of("zipalign", "-f", "4", apkPath, alignedPath);
            executeCommand(cmd, 60);
            Files.move(Path.of(alignedPath), Path.of(apkPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.warn("zipalign 执行失败，继续签名: {}", e.getMessage());
            try { Files.deleteIfExists(Path.of(alignedPath)); } catch (Exception ignored) {}
        }
    }

    private boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", command);
            Process process = pb.start();
            return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void executeCommand(List<String> command, int timeoutSeconds) {
        try {
            log.debug("执行命令: {}", String.join(" ", command));
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BizException("命令执行超时");
            }

            if (process.exitValue() != 0) {
                log.error("命令执行失败, exitCode={}, output={}", process.exitValue(), output);
                throw new BizException("签名工具执行失败: " + output.toString().trim());
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("命令执行异常: " + e.getMessage());
        }
    }
}
