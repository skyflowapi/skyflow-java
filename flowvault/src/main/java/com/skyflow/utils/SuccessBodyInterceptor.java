package com.skyflow.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.skyflow.generated.rest.core.ObjectMappers;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Normalizes 2xx bodies for query, getTokens, uploadFiles and deleteFiles before the generated
 * client parses them. A blank body becomes {@code {}}, an empty response, instead of a
 * deserialization failure. A body that is not a JSON object fails the call with
 * {@link UnparseableResponseException}, which keeps the real status and body.
 */
public final class SuccessBodyInterceptor implements Interceptor {

    static final Set<String> PATHS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "/v2/query", "/v2/records/getTokens", "/v2/files/upload", "/v2/files/delete")));

    /** Bodies longer than this are cut short in the error message. */
    static final int MAX_BODY_IN_ERROR = 512;

    private static final MediaType JSON = MediaType.parse("application/json");

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        if (!response.isSuccessful() || !appliesTo(chain.request().url().encodedPath())) {
            return response;
        }
        ResponseBody body = response.body();
        String text = body == null ? "" : body.string();
        if (text.trim().isEmpty()) {
            return response.newBuilder().body(ResponseBody.create("{}", JSON)).build();
        }
        if (!isJsonObject(text)) {
            response.close();
            throw new UnparseableResponseException(response.code(), truncate(text), response.headers().toMultimap());
        }
        MediaType contentType = body.contentType() != null ? body.contentType() : JSON;
        return response.newBuilder().body(ResponseBody.create(text, contentType)).build();
    }

    static boolean appliesTo(String path) {
        for (String suffix : PATHS) {
            if (path.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isJsonObject(String text) {
        try {
            return ObjectMappers.JSON_MAPPER.readTree(text).isObject();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private static String truncate(String text) {
        return text.length() <= MAX_BODY_IN_ERROR ? text : text.substring(0, MAX_BODY_IN_ERROR) + "...";
    }
}
