package com.skyflow.vault.data;

import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InsertResponseRecord {
    private final String tableName;
    private final String skyflowId;
    private final Map<String, Object> tokens;
    private final Map<String, Object> data;
    private final Map<String, Object> hashedData;
    private final int httpCode;
    private final String error;

    /**
     * @deprecated Use {@link #InsertResponseRecord(String, String, Map, Map, Map, int, String)} instead,
     * which also lets you populate {@code data}. This overload always leaves {@code data} null.
     */
    @Deprecated(since = "1.0.2", forRemoval = true)
    public InsertResponseRecord(String tableName, String skyflowId, Map<String, Object> tokens,
                                 Map<String, Object> hashedData, int httpCode, String error) {
        this(tableName, skyflowId, tokens, null, hashedData, httpCode, error);
    }

    public InsertResponseRecord(String tableName, String skyflowId, Map<String, Object> tokens,
                                 Map<String, Object> data, Map<String, Object> hashedData, int httpCode, String error) {
        this.tableName = tableName;
        this.skyflowId = skyflowId;
        this.tokens = tokens;
        this.data = data;
        this.hashedData = hashedData;
        this.httpCode = httpCode;
        this.error = error;
    }

    public String getTableName() {
        return tableName;
    }

    public String getSkyflowId() {
        return skyflowId;
    }

    public Map<String, Object> getTokens() {
        return tokens;
    }

    /**
     * @deprecated Response key 'fields' is deprecated. Use {@link #getTokens()} instead.
     */
    @Deprecated(since = "1.0.2", forRemoval = true)
    public Map<String, Object> getFields() {
        LogUtil.printWarningLog(InfoLogs.DEPRECATED_INSERT_FIELDS_GETTER.getLog());
        return getTokens();
    }

    /**
     * A typed view of {@link #getTokens()}: the same per-column token data, parsed into
     * {@link Token} objects instead of raw {@code Object}s. The API models a column's tokens
     * generically to stay flexible (see {@link #getTokens()}), so this parses every shape that
     * generic value is known to take — a list of {@code {token, tokenGroupName}} entries (a
     * column tokenized against more than one group), a single such entry, or a bare token value
     * with no group information — into a consistently-typed {@code List<Token>} per column.
     *
     * <p>Returns {@code null} when {@link #getTokens()} is {@code null} (e.g. a failed record).
     * A column whose raw value cannot be parsed into any of the above shapes is omitted, rather
     * than throwing.
     */
    public Map<String, List<Token>> getTokenDetails() {
        if (tokens == null) {
            return null;
        }
        Map<String, List<Token>> details = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : tokens.entrySet()) {
            List<Token> parsed = parseTokenEntries(entry.getValue());
            if (parsed != null) {
                details.put(entry.getKey(), parsed);
            }
        }
        return details;
    }

    private static List<Token> parseTokenEntries(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        List<Token> parsed = new ArrayList<>();
        if (rawValue instanceof List) {
            for (Object entry : (List<?>) rawValue) {
                Token token = toToken(entry);
                if (token != null) {
                    parsed.add(token);
                }
            }
        } else {
            Token token = toToken(rawValue);
            if (token != null) {
                parsed.add(token);
            }
        }
        return parsed;
    }

    private static Token toToken(Object entry) {
        if (entry instanceof Map) {
            Map<?, ?> entryMap = (Map<?, ?>) entry;
            Object token = entryMap.get("token");
            Object tokenGroupName = entryMap.get("tokenGroupName");
            return new Token(token != null ? token.toString() : null,
                    tokenGroupName != null ? tokenGroupName.toString() : null);
        }
        if (entry != null) {
            // A column tokenized against a single, unnamed group can come back as a bare value.
            return new Token(entry.toString(), null);
        }
        return null;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Map<String, Object> getHashedData() {
        return hashedData;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String getError() {
        return error;
    }
}
