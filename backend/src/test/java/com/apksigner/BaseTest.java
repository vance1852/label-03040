package com.apksigner;

import com.apksigner.util.CryptoUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public abstract class BaseTest {

    private static final String TEST_KEY = "TestKey!16Bytes!";

    @BeforeAll
    static void setupCryptoKey() {
        CryptoUtil.setKeyForTesting(TEST_KEY);
    }

    @AfterAll
    static void cleanupCryptoKey() {
        CryptoUtil.resetKey();
    }
}
