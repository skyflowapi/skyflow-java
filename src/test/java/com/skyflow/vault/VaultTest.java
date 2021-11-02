package com.skyflow.vault;

import com.skyflow.entities.LogLevel;
import com.skyflow.entities.Options;
import com.skyflow.entities.SkyflowConfiguration;
import com.skyflow.entities.TokenProvider;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

class DemoTokenProvider implements TokenProvider {
    @Override
    public String getBearerToken() throws SkyflowException {
        return "auth_token";
    }
}

public class VaultTest {

    @Test
    public void testValidConfig() {
        SkyflowConfiguration testConfig = new SkyflowConfiguration("test_vault_id", "https://valid.url.com", new DemoTokenProvider());
        try {
            Skyflow skyflow = Skyflow.init(testConfig);
            assert (skyflow instanceof Skyflow);
        } catch (SkyflowException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testValidConfigWithOptions() {
        SkyflowConfiguration testConfig = new SkyflowConfiguration("test_vault_id", "https://valid.url.com", new DemoTokenProvider(), new Options(LogLevel.DEBUG));
        try {
            Skyflow skyflow = Skyflow.init(testConfig);
            assert (skyflow instanceof Skyflow);
        } catch (SkyflowException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testInvalidConfigWithEmptyVaultID() {
        SkyflowConfiguration testConfig = new SkyflowConfiguration("", "https://valid.url.com", new DemoTokenProvider());
        try {
            Skyflow skyflow = Skyflow.init(testConfig);
        } catch (SkyflowException e) {
            assertEquals(e.getCode(), ErrorCode.EmptyVaultID.getCode());
            assertEquals(e.getMessage(), ErrorCode.EmptyVaultID.getDescription());
        }
    }

    @Test
    public void testInvalidConfigWithInvalidVaultURL() {
        SkyflowConfiguration testConfig = new SkyflowConfiguration("test-vault-id", "//valid.url.com", new DemoTokenProvider());
        try {
            Skyflow skyflow = Skyflow.init(testConfig);
        } catch (SkyflowException e) {
            assertEquals(e.getCode(), ErrorCode.InvalidVaultURL.getCode());
            assertEquals(e.getMessage(), ErrorCode.InvalidVaultURL.getDescription());
        }
    }

    @Test
    public void testInvalidConfigWithHttpVaultURL() {
        SkyflowConfiguration testConfig = new SkyflowConfiguration("test-vault-id", "http://valid.url.com", new DemoTokenProvider());
        try {
            Skyflow skyflow = Skyflow.init(testConfig);
        } catch (SkyflowException e) {
            assertEquals(e.getCode(), ErrorCode.InvalidVaultURL.getCode());
            assertEquals(e.getMessage(), ErrorCode.InvalidVaultURL.getDescription());
        }
    }

}
