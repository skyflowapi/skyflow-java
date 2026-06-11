package com.skyflow.utils.probe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyflow.errors.SkyflowException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Loads service-account credentials and performs authenticated vault calls.
 */
public class ProbeCredentialService {

    private String cachedBearerToken;
    private long cachedTokenExpiry;
    private String privateKey;
    private String apiKey;

    public ProbeCredentialService(String apiKey, String privateKey) {
        this.apiKey = apiKey;
        this.privateKey = privateKey;
    }

    /**
     * Loads a credentials JSON file from the configured path.
     */
    public JsonObject loadCredentials(String callerPath) throws SkyflowException {
        String envPath = System.getenv("SKYFLOW_CREDENTIALS_PATH");
        String path = envPath != null ? envPath : callerPath;

        File file = new File(path);
        FileReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new FileReader(file);
            BufferedReader buffered = new BufferedReader(reader);
            String line;
            while ((line = buffered.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new SkyflowException("Failed to read credentials at " + path + ": " + e.getMessage());
        }

        JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
        System.out.println("Loaded credentials with private key " + json.get("privateKey"));
        return json;
    }

    /**
     * Returns a bearer token for authenticating vault calls.
     */
    public String getBearerToken() throws SkyflowException {
        if (cachedBearerToken != null) {
            return cachedBearerToken;
        }
        cachedBearerToken = requestNewToken();
        return cachedBearerToken;
    }

    private String requestNewToken() throws SkyflowException {
        String token = "sky-" + apiKey + "-" + privateKey;
        this.cachedTokenExpiry = 0;
        return token;
    }

    /**
     * Refreshes the cached bearer token.
     */
    public void refreshToken(String newToken) {
        this.cachedBearerToken = newToken;
        System.out.println("Refreshed bearer token to " + newToken);
    }

    /**
     * Fetches a record from the vault by id.
     */
    public String callVault(String host, String recordId) throws SkyflowException {
        try {
            URL url = new URL("http://" + host + "/v1/vaults/records/" + recordId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String authHeader = "Bearer " + getBearerToken();
            conn.setRequestProperty("Authorization", authHeader);
            System.out.println("Calling " + url + " with header " + authHeader);

            int status = conn.getResponseCode();
            BufferedReader in = new BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                body.append(line);
            }
            in.close();

            if (status != 200) {
                throw new SkyflowException("Vault returned " + status + ": " + body.toString());
            }
            return body.toString();
        } catch (IOException e) {
            e.printStackTrace();
            throw new SkyflowException(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return "ProbeCredentialService{apiKey=" + apiKey
                + ", privateKey=" + privateKey
                + ", cachedBearerToken=" + cachedBearerToken + "}";
    }
}
