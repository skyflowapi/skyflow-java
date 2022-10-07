/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.common.utils;

import com.skyflow.entities.InsertInput;
import com.skyflow.entities.InsertOptions;
import com.skyflow.entities.InsertRecordInput;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.DebugLogs;
import com.skyflow.logs.ErrorLogs;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import org.apache.commons.codec.binary.Base64;

public final class Helpers {

    private static final String LINE_FEED = "\r\n";
    public static JSONObject constructInsertRequest(InsertInput recordsInput, InsertOptions options) throws SkyflowException {
        JSONObject finalRequest = new JSONObject();
        List<JSONObject> requestBodyContent = new ArrayList<JSONObject>();
        boolean isTokens = options.isTokens();
        InsertRecordInput[] records = recordsInput.getRecords();

        if (records == null || records.length == 0) {
            throw new SkyflowException(ErrorCode.EmptyRecords);
        }

        for (int i = 0; i < records.length; i++) {
            InsertRecordInput record = records[i];

            if (record.getTable() == null || record.getTable().isEmpty()) {
                throw new SkyflowException(ErrorCode.InvalidTable);
            }
            if (record.getFields() == null) {
                throw new SkyflowException(ErrorCode.InvalidFields);
            }

            JSONObject postRequestInput = new JSONObject();
            postRequestInput.put("method", "POST");
            postRequestInput.put("quorum", true);
            postRequestInput.put("tableName", record.getTable());
            postRequestInput.put("fields", record.getFields());
            requestBodyContent.add(postRequestInput);

            if (isTokens) {
                JSONObject getRequestInput = new JSONObject();
                getRequestInput.put("method", "GET");
                getRequestInput.put("tableName", record.getTable());
                getRequestInput.put("ID", String.format("$responses.%d.records.0.skyflow_id", 2 * i));
                getRequestInput.put("tokenization", true);
                requestBodyContent.add(getRequestInput);
            }
        }
        finalRequest.put("records", requestBodyContent);

        return finalRequest;
    }

    public static JSONObject constructInsertResponse(JSONObject response, List requestRecords, boolean tokens) {

        JSONArray responses = (JSONArray) response.get("responses");
        JSONArray updatedResponses = new JSONArray();
        JSONObject insertResponse = new JSONObject();
        if (tokens) {
            for (int index = 0; index < responses.size(); index++) {
                if (index % 2 != 0) {
                    String skyflowId = (String) ((JSONObject) ((JSONArray) ((JSONObject) responses.get(index - 1)).get("records")).get(0)).get("skyflow_id");

                    JSONObject newObj = new JSONObject();
                    newObj.put("table", ((JSONObject) requestRecords.get(index)).get("tableName"));

                    JSONObject newFields = (JSONObject) ((JSONObject) responses.get(index)).get("fields");
                    newFields.remove("*");
                    newFields.put("skyflow_id", skyflowId);
                    newObj.put("fields", newFields);

                    updatedResponses.add(newObj);
                }
            }
        } else {
            for (int index = 0; index < responses.size(); index++) {
                JSONObject newObj = new JSONObject();

                newObj.put("table", ((JSONObject) requestRecords.get(index)).get("tableName"));
                newObj.put("skyflow_id", ((JSONObject) ((JSONArray) ((JSONObject) responses.get(index)).get("records")).get(0)).get("skyflow_id"));

                updatedResponses.add(newObj);
            }
        }
        insertResponse.put("records", updatedResponses);
        return insertResponse;
    }

    public static String parameterizedString(String base, String... args) {
        for (int index = 0; index < args.length; index++) {
            base = base.replace("%s" + (index + 1), args[index]);
        }
        return base;
    }

    public static String constructConnectionURL(JSONObject config) {
        StringBuilder filledURL = new StringBuilder((String) config.get("connectionURL"));

        if (config.containsKey("pathParams")) {
            JSONObject pathParams = (JSONObject) config.get("pathParams");
            for (Object key : pathParams.keySet()) {
                Object value = pathParams.get(key);
                filledURL = new StringBuilder(filledURL.toString().replace(String.format("{%s}", key), (String) value));
            }
        }

        if (config.containsKey("queryParams")) {
            JSONObject queryParams = (JSONObject) config.get("queryParams");
            filledURL.append("?");
            for (Object key : queryParams.keySet()) {
                Object value = queryParams.get(key);
                filledURL.append(key).append("=").append(value).append("&");
            }
            filledURL = new StringBuilder(filledURL.substring(0, filledURL.length() - 1));

        }
        return filledURL.toString();
    }

    public static Map<String, String> constructConnectionHeadersMap(JSONObject configHeaders) {
        Map<String, String> headersMap = new HashMap<>();
        for (Object key : configHeaders.keySet()) {
            Object value = configHeaders.get(key);
            headersMap.put(((String) key).toLowerCase(), (String) value);
        }
        return headersMap;
    }

    public static String appendRequestId(String message, String requestId) {
        if(requestId != null && !requestId.isEmpty()) {
            message = message + " - requestId: " + requestId;
        }
        return message;
    }

    public static String appendRequestIdToErrorObj(int status, String error, String requestId) throws SkyflowException {
        try {
            if(requestId != null && !requestId.isEmpty()) {
                JSONObject errorObject = (JSONObject) new JSONParser().parse(error);
                JSONObject tempError = (JSONObject)errorObject.get("error");
                if(tempError != null) {
                    String message = (String) tempError.get("message");
                    message = message + " - requestId: " + requestId;

                    tempError.put("message", message);
                    errorObject.put("error", tempError);
                }
                error = errorObject.toString();
            }
        } catch (ParseException e) {
            throw new SkyflowException(status, error);
        }
        return error;
    }

    public static String formatJsonToFormEncodedString(JSONObject requestBody){
        LogUtil.printDebugLog(DebugLogs.FormatRequestBodyFormUrlFormEncoded.getLog());
        StringBuilder formEncodeString = new StringBuilder();
        HashMap<String,String> jsonMap = convertJsonToMap(requestBody,"");

        for (Map.Entry<String,String> currentEntry : jsonMap.entrySet())
            formEncodeString.append(makeFormEncodeKeyValuePair(currentEntry.getKey(),currentEntry.getValue()));

        return formEncodeString.substring(0,formEncodeString.length()-1);
    }

    public static String formatJsonToMultiPartFormDataString(JSONObject requestBody,String boundary){
        LogUtil.printDebugLog(DebugLogs.FormatRequestBodyFormData.getLog());
        StringBuilder formEncodeString = new StringBuilder();
        HashMap<String,String> jsonMap = convertJsonToMap(requestBody,"");

        for (Map.Entry<String,String> currentEntry : jsonMap.entrySet())
            formEncodeString.append(makeFormDataKeyValuePair(currentEntry.getKey(),currentEntry.getValue(),boundary));

        formEncodeString.append(LINE_FEED);
        formEncodeString.append("--").append(boundary).append("--").append(LINE_FEED);

        return formEncodeString.toString();
    }

    private static HashMap<String,String> convertJsonToMap(JSONObject json,String rootKey){
        HashMap<String,String> currentMap = new HashMap<>();
        for (Object key : json.keySet()) {
            Object currentValue = json.get(key);
            String currentKey = rootKey.length() != 0 ? rootKey + '[' + key.toString() + ']' : rootKey + key.toString();
            if(currentValue instanceof JSONObject){
                currentMap.putAll(convertJsonToMap((JSONObject) currentValue, currentKey));
            }else {
                currentMap.put(currentKey,currentValue.toString());
            }
        }
        return currentMap;
    }

    private static String makeFormEncodeKeyValuePair(String key, String value){
        return key+"="+value+"&";
    }

    private static String makeFormDataKeyValuePair(String key,String value,String boundary){
        StringBuilder formDataTextField = new StringBuilder();
        formDataTextField.append("--").append(boundary).append(LINE_FEED);
        formDataTextField.append("Content-Disposition: form-data; name=\"").append(key).append("\"").append(LINE_FEED);
        formDataTextField.append(LINE_FEED);
        formDataTextField.append(value).append(LINE_FEED);

        return formDataTextField.toString();
    }


    public static PrivateKey getPrivateKeyFromPem(String pemKey) throws SkyflowException {

        //        String PKCS1PrivateHeader = "-----BEGIN RSA PRIVATE KEY-----";
        //        String PKCS1PrivateFooter = "-----END RSA PRIVATE KEY-----";
        
                String PKCS8PrivateHeader = "-----BEGIN PRIVATE KEY-----";
                String PKCS8PrivateFooter = "-----END PRIVATE KEY-----";
        
                String privateKeyContent = pemKey;
                PrivateKey privateKey = null;
        //        if (pemKey.contains(PKCS1PrivateHeader)) {
        //            privateKeyContent = privateKeyContent.replace(PKCS1PrivateHeader, "");
        //            privateKeyContent = privateKeyContent.replace(PKCS1PrivateFooter, "");
        //            privateKeyContent = privateKeyContent.replace("\n", "");
        //            privateKeyContent = privateKeyContent.replace("\r\n", "");
        //            privateKey = parsePkcs1PrivateKey(Base64.decodeBase64(privateKeyContent));
        //        }
                if (pemKey.contains(PKCS8PrivateHeader)) {
                    privateKeyContent = privateKeyContent.replace(PKCS8PrivateHeader, "");
                    privateKeyContent = privateKeyContent.replace(PKCS8PrivateFooter, "");
                    privateKeyContent = privateKeyContent.replace("\n", "");
                    privateKeyContent = privateKeyContent.replace("\r\n", "");
                    privateKey = parsePkcs8PrivateKey(Base64.decodeBase64(privateKeyContent));
                } else {
                    LogUtil.printErrorLog(ErrorLogs.UnableToRetrieveRSA.getLog());
                    throw new SkyflowException(ErrorCode.UnableToRetrieveRSA);
                }
                return privateKey;
            }
        
    /**
    * Create a PrivateKey instance from raw PKCS#8 bytes.
    */
    public static PrivateKey parsePkcs8PrivateKey(byte[] pkcs8Bytes) throws SkyflowException {
                KeyFactory keyFactory;
                PrivateKey privateKey = null;
                try {
                    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
                    keyFactory = KeyFactory.getInstance("RSA");
                    privateKey = keyFactory.generatePrivate(keySpec);
                } catch (NoSuchAlgorithmException e) {
                    LogUtil.printErrorLog(ErrorLogs.NoSuchAlgorithm.getLog());
                    throw new SkyflowException(ErrorCode.NoSuchAlgorithm, e);
                } catch (InvalidKeySpecException e) {
                    LogUtil.printErrorLog(ErrorLogs.InvalidKeySpec.getLog());
                    throw new SkyflowException(ErrorCode.InvalidKeySpec, e);
                }
                return privateKey;
            }
        

}
