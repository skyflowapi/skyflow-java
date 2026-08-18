package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One token-group outcome for a single column value, as returned inside
 * {@link InsertResponseRecord#getTokens()}. A column tokenized against more than one
 * token group comes back as a list of these, one per group. {@link #getPath()} identifies
 * which part of a structured column value a token corresponds to, when applicable.
 */
public class Token {
    @Expose(serialize = true)
    private final String token;
    @Expose(serialize = true)
    private final String tokenGroupName;
    @Expose(serialize = true)
    private final String path;

    public Token(String token, String tokenGroupName) {
        this(token, tokenGroupName, null);
    }

    /**
     * @param path the location, within the column's own (structured) value, that this token
     *             corresponds to — e.g. {@code "street"} or {@code "phone_numbers[0].type"} for a
     *             token generated from a nested field. {@code null} when the column's value isn't
     *             structured, so there is nothing to point into.
     */
    public Token(String token, String tokenGroupName, String path) {
        this.token = token;
        this.tokenGroupName = tokenGroupName;
        this.path = path;
    }

    public String getToken() {
        return token;
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }

    /**
     * Parses the API's raw, generically-typed per-column token data (as returned by the wire
     * type, {@code Map<String, Object>}) into {@code Map<String, List<Token>>}. The API models
     * a column's tokens generically to stay flexible, so this parses every shape that generic
     * value is known to take — a list of {@code {token, tokenGroupName, path}} entries (a column
     * tokenized against more than one group), a single such entry, or a bare token value with
     * no group information — into a consistently-typed {@code List<Token>} per column.
     * {@code path} is only present when the column's own value is itself structured (e.g. a
     * nested object or array) and identifies which part of it a given token came from.
     *
     * <p>Returns {@code null} when {@code rawTokens} is {@code null} (e.g. a failed record). A
     * column whose raw value cannot be parsed into any of the above shapes is omitted, rather
     * than throwing. A {@code null} element inside a column's list is skipped the same way.
     */
    public static Map<String, List<Token>> parseTokens(Map<String, Object> rawTokens) {
        if (rawTokens == null) {
            return null;
        }
        Map<String, List<Token>> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawTokens.entrySet()) {
            List<Token> tokens = parseTokenEntries(entry.getValue());
            if (tokens != null) {
                parsed.put(entry.getKey(), tokens);
            }
        }
        return parsed;
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
            Object path = entryMap.get("path");
            return new Token(token != null ? token.toString() : null,
                    tokenGroupName != null ? tokenGroupName.toString() : null,
                    path != null ? path.toString() : null);
        }
        if (entry != null) {
            // A column tokenized against a single, unnamed group can come back as a bare value.
            return new Token(entry.toString(), null);
        }
        return null;
    }

    /**
     * The inverse of {@link #parseTokens(Map)}: renders parsed {@link Token} objects back into
     * the generic {@code Map<String, Object>} shape {@code getFields()} returned before it was
     * deprecated, for callers who haven't migrated to {@link InsertResponseRecord#getTokens()}
     * yet. Each column's value becomes a {@code List<Map<String, Object>>}, one map per
     * {@code Token} with {@code "token"}/{@code "tokenGroupName"} keys (plus {@code "path"} when
     * present) — this doesn't reproduce
     * the exact original wire shape (a single-group column may originally have been a bare
     * value or an unwrapped map rather than a one-element list), since that distinction is lost
     * once parsed, but it's a consistent, self-describing shape every caller can read the same
     * way regardless of how many groups a column has.
     *
     * <p>Returns {@code null} when {@code tokens} is {@code null}.
     */
    static Map<String, Object> toRawTokens(Map<String, List<Token>> tokens) {
        if (tokens == null) {
            return null;
        }
        Map<String, Object> raw = new LinkedHashMap<>();
        for (Map.Entry<String, List<Token>> entry : tokens.entrySet()) {
            List<Map<String, Object>> rawEntries = new ArrayList<>();
            for (Token token : entry.getValue()) {
                Map<String, Object> rawEntry = new LinkedHashMap<>();
                rawEntry.put("token", token.getToken());
                rawEntry.put("tokenGroupName", token.getTokenGroupName());
                // Only added when present, unlike token/tokenGroupName - so a path-less round trip
                // (the common case) stays exactly as lossless as it was before path existed.
                if (token.getPath() != null) {
                    rawEntry.put("path", token.getPath());
                }
                rawEntries.add(rawEntry);
            }
            raw.put(entry.getKey(), rawEntries);
        }
        return raw;
    }
}
