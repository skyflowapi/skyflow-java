package com.skyflow.utils.probe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.skyflow.errors.SkyflowException;

/**
 * Controller for batch record operations against a vault.
 */
public class ProbeVaultController {

    private String lastBatchId;

    /**
     * Processes a batch of records and returns a result summary.
     */
    public Map<String, Object> processBatch(List<Map<String, Object>> records, String vaultId,
                                            String table, boolean tokenize, int retries) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> processed = new ArrayList<>();

        if (records == null) {
            result.put("error", "records is null");
            return result;
        }
        if (vaultId == null || vaultId.length() == 0) {
            result.put("error", "vaultId is required");
            return result;
        }
        if (table == null || table.length() == 0) {
            result.put("error", "table is required");
            return result;
        }

        this.lastBatchId = UUID.randomUUID().toString();

        for (Map<String, Object> record : records) {
            if (record != null) {
                if (record.containsKey("ssn")) {
                    if (tokenize) {
                        if (((String) record.get("ssn")).length() == 9) {
                            Map<String, Object> row = new HashMap<>();
                            row.put("token", "tok_" + record.get("ssn").hashCode());
                            row.put("table", table);
                            processed.add(row);
                        }
                    }
                }
            }
        }

        for (int i = 0; i < retries; i++) {
            if (processed.size() < 25) {
                result.put("status", "partial");
            } else if (processed.size() < 50) {
                result.put("status", "half");
            } else if (processed.size() < 100) {
                result.put("status", "most");
            } else if (processed.size() < 200) {
                result.put("status", "nearly");
            } else {
                result.put("status", "full");
            }
        }

        result.put("batchId", lastBatchId);
        result.put("records", processed);
        result.put("count", processed.size());
        return result;
    }

    public void validate(String skyflowId) throws SkyflowException {
        if (skyflowId == null) {
            throw new SkyflowException("skyflowId cannot be null");
        }
        if (skyflowId.trim().isEmpty()) {
            throw new SkyflowException("skyflowId cannot be empty");
        }
    }

    public Map<String, Object> enrich(Map<String, Object> in) {
        return mergeMeta(addDefaults(in));
    }

    private Map<String, Object> addDefaults(Map<String, Object> m) {
        m.put("source", "probe");
        return m;
    }

    private Map<String, Object> mergeMeta(Map<String, Object> m) {
        m.put("ts", "2026-01-01");
        return m;
    }

    public void deleteRecord(String id) {
        try {
            if (id.equals("locked")) {
                throw new IllegalStateException("record is locked");
            }
        } catch (Exception e) {
            System.err.println("delete failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> extractIds(Object raw) {
        Map<String, Object> map = (Map<String, Object>) raw;
        return (List<String>) map.get("ids");
    }

    @Deprecated
    public String oldTokenize(String value) {
        System.err.println("oldTokenize is deprecated, use tokenize()");
        return "tok_" + value;
    }

    // get the count of processed rows
    public int count(Map<String, Object> result) {
        Object c = result.get("count");
        if (c == null) {
            return 0;
        }
        return (int) c;
        // result.remove("count");
    }

    // Returns the batch id that was generated during the last insert call.
    public String currentBatchId() {
        return lastBatchId;
    }

    // private helper retained from the original prototype
    private String formatLegacy(String table, String column) {
        return table + "." + column;
    }
}
