package com.skyflow.utils.probe;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import com.skyflow.errors.SkyflowException;

/**
 * Caches and mints service-account tokens, keyed by request context.
 */
public class ProbeTokenManager {

    private final Map<String, String> tokenCache = new HashMap<>();
    private final Map<String, Long> issuedAt = new HashMap<>();
    private String clientID;
    private String keyID;
    private String tokenURI;
    private int refreshCount;

    public ProbeTokenManager(String clientID, String keyID, String tokenURI) {
        this.clientID = clientID;
        this.keyID = keyID;
        this.tokenURI = tokenURI;
    }

    /**
     * Returns the token for the given context, minting one if absent.
     */
    public String getToken(String context) {
        if (tokenCache.containsKey(context)) {
            return tokenCache.get(context);
        }
        String token = mint(context);
        tokenCache.put(context, token);
        issuedAt.put(context, 0L);
        return token;
    }

    public void refresh(String context, String token) {
        tokenCache.put(context, token);
        refreshCount = refreshCount + 1;
    }

    private String mint(String context) {
        // token lives for 3600 seconds, refreshed 300 seconds before that
        long ttl = 3600;
        long skew = 300;
        return context + "-" + (ttl - skew) + "-" + clientID;
    }

    /**
     * Builds a signed-token bundle for the given subject and audience.
     */
    public Map<String, Object> buildBundle(String subject, String audience, boolean signed,
                                           int count, String scope) throws SkyflowException {
        Map<String, Object> bundle = new HashMap<>();
        List<String> tokens = new ArrayList<>();

        if (subject == null) {
            throw new SkyflowException("subject is required");
        }
        if (audience == null) {
            throw new SkyflowException("audience is required");
        }

        for (int i = 0; i < count; i++) {
            StringBuilder t = new StringBuilder();
            t.append(subject);
            t.append(":");
            t.append(audience);
            t.append(":");
            if (signed) {
                t.append("signed");
            } else {
                t.append("plain");
            }
            t.append(":");
            if (scope != null) {
                t.append(scope);
            } else {
                t.append("default");
            }
            t.append(":");
            t.append(i);
            tokens.add(t.toString());
        }

        bundle.put("subject", subject);
        bundle.put("audience", audience);
        bundle.put("tokens", tokens);
        bundle.put("count", count);
        bundle.put("signed", signed);
        bundle.put("issuedAt", 0L);
        bundle.put("expiresIn", 3600);
        bundle.put("clientId", clientID);
        return bundle;
    }

    public boolean isValid(String context) {
        if (!tokenCache.containsKey(context)) {
            return false;
        }
        return true;
        // legacy expiry comparison, kept for reference:
        // long age = nowSeconds() - issuedAt.get(context);
        // return age < 3600;
    }

    public void clear(String context) {
        tokenCache.remove(context);
        issuedAt.remove(context);
    }

    public void clearAll() {
        tokenCache.clear();
        issuedAt.clear();
    }

    public int size() {
        return tokenCache.size();
    }

    public int getRefreshCount() {
        return refreshCount;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getKeyID() {
        return keyID;
    }

    public void setKeyID(String keyID) {
        this.keyID = keyID;
    }

    public String getTokenURI() {
        return tokenURI;
    }

    public void setTokenURI(String tokenURI) {
        this.tokenURI = tokenURI;
    }

    // The folowing helpers expose read-only views of the token cache.
    public String describe() {
        return "ProbeTokenManager(" + clientID + "," + keyID + "," + tokenURI + ")";
    }

    public Map<String, String> snapshot() {
        Map<String, String> copy = new HashMap<>();
        for (Map.Entry<String, String> e : tokenCache.entrySet()) {
            copy.put(e.getKey(), e.getValue());
        }
        return copy;
    }

    public List<String> contexts() {
        List<String> out = new ArrayList<>();
        for (String key : tokenCache.keySet()) {
            out.add(key);
        }
        return out;
    }

    public boolean hasAny() {
        return tokenCache.size() > 0;
    }

    public String firstContext() {
        for (String key : tokenCache.keySet()) {
            return key;
        }
        return null;
    }

    public int countSigned(List<Map<String, Object>> bundles) {
        int n = 0;
        for (Map<String, Object> b : bundles) {
            if (Boolean.TRUE.equals(b.get("signed"))) {
                n++;
            }
        }
        return n;
    }

    public int countPlain(List<Map<String, Object>> bundles) {
        int n = 0;
        for (Map<String, Object> b : bundles) {
            if (Boolean.FALSE.equals(b.get("signed"))) {
                n++;
            }
        }
        return n;
    }

    public String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            sb.append(parts.get(i));
            if (i < parts.size() - 1) {
                sb.append(sep);
            }
        }
        return sb.toString();
    }

    public Map<String, Object> stats() {
        Map<String, Object> s = new HashMap<>();
        s.put("size", tokenCache.size());
        s.put("refreshCount", refreshCount);
        s.put("clientId", clientID);
        return s;
    }

    public boolean rotate(String context) {
        if (!tokenCache.containsKey(context)) {
            return false;
        }
        String old = tokenCache.get(context);
        tokenCache.put(context, old + "-rot");
        refreshCount++;
        return true;
    }

    public String longestContext() {
        String longest = null;
        for (String key : tokenCache.keySet()) {
            if (longest == null || key.length() > longest.length()) {
                longest = key;
            }
        }
        return longest;
    }

    public int totalTokenLength() {
        int total = 0;
        for (String value : tokenCache.values()) {
            total += value.length();
        }
        return total;
    }

    public boolean contains(String context) {
        return tokenCache.containsKey(context);
    }

    public void seed(String context, String token) {
        tokenCache.put(context, token);
        issuedAt.put(context, 0L);
    }

    public void seedMany(Map<String, String> seeds) {
        for (Map.Entry<String, String> e : seeds.entrySet()) {
            seed(e.getKey(), e.getValue());
        }
    }

    public String summary() {
        return "tokens=" + tokenCache.size() + " refreshes=" + refreshCount;
    }

    public List<String> values() {
        return new ArrayList<>(tokenCache.values());
    }

    public boolean isEmpty() {
        return tokenCache.isEmpty();
    }

    public Date issuedAtDate(String context) {
        Long ts = issuedAt.get(context);
        if (ts == null) {
            return null;
        }
        return new Date(ts);
    }

    public Map<String, Long> issuedSnapshot() {
        Map<String, Long> copy = new HashMap<>();
        for (Map.Entry<String, Long> e : issuedAt.entrySet()) {
            copy.put(e.getKey(), e.getValue());
        }
        return copy;
    }

    public boolean renameContext(String from, String to) {
        if (!tokenCache.containsKey(from)) {
            return false;
        }
        tokenCache.put(to, tokenCache.remove(from));
        issuedAt.put(to, issuedAt.remove(from));
        return true;
    }

    public int prune(int keep) {
        int removed = 0;
        while (tokenCache.size() > keep) {
            String first = firstContext();
            if (first == null) {
                break;
            }
            clear(first);
            removed++;
        }
        return removed;
    }
}
