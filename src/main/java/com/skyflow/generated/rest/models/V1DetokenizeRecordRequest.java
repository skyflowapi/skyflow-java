/*
 * Skyflow Data API
 * # Data API  This API inserts, retrieves, and otherwise manages data in a vault.  The Data API is available from two base URIs. *identifier* is the identifier in your vault's URL.<ul><li><b>Sandbox:</b> https://_*identifier*.vault.skyflowapis-preview.com</li><li><b>Production:</b> https://_*identifier*.vault.skyflowapis.com</li></ul>  When you make an API call, you need to add a header: <table><tr><th>Header</th><th>Value</th><th>Example</th></tr><tr><td>Authorization</td><td>A Bearer Token. See <a href='/api-authentication/'>API Authentication</a>.</td><td><code>Authorization: Bearer eyJhbGciOiJSUzI...1NiIsJdfPA</code></td></tr><table/>
 *
 * The version of the OpenAPI document: v1
 * Contact: support@skyflow.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.skyflow.generated.rest.models;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.skyflow.generated.rest.models.RedactionEnumREDACTION;
import java.io.IOException;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.skyflow.generated.rest.JSON;

/**
 * V1DetokenizeRecordRequest
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-09-25T11:34:22.684345+05:30[Asia/Kolkata]", comments = "Generator version: 7.8.0")
public class V1DetokenizeRecordRequest {
  public static final String SERIALIZED_NAME_TOKEN = "token";
  @SerializedName(SERIALIZED_NAME_TOKEN)
  private String token;

  public static final String SERIALIZED_NAME_REDACTION = "redaction";
  @SerializedName(SERIALIZED_NAME_REDACTION)
  private RedactionEnumREDACTION redaction = RedactionEnumREDACTION.DEFAULT;

  public V1DetokenizeRecordRequest() {
  }

  public V1DetokenizeRecordRequest token(String token) {
    this.token = token;
    return this;
  }

  /**
   * Token that identifies the record to detokenize.
   * @return token
   */
  @javax.annotation.Nullable
  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }


  public V1DetokenizeRecordRequest redaction(RedactionEnumREDACTION redaction) {
    this.redaction = redaction;
    return this;
  }

  /**
   * Get redaction
   * @return redaction
   */
  @javax.annotation.Nullable
  public RedactionEnumREDACTION getRedaction() {
    return redaction;
  }

  public void setRedaction(RedactionEnumREDACTION redaction) {
    this.redaction = redaction;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1DetokenizeRecordRequest v1DetokenizeRecordRequest = (V1DetokenizeRecordRequest) o;
    return Objects.equals(this.token, v1DetokenizeRecordRequest.token) &&
        Objects.equals(this.redaction, v1DetokenizeRecordRequest.redaction);
  }

  @Override
  public int hashCode() {
    return Objects.hash(token, redaction);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1DetokenizeRecordRequest {\n");
    sb.append("    token: ").append(toIndentedString(token)).append("\n");
    sb.append("    redaction: ").append(toIndentedString(redaction)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }


  public static HashSet<String> openapiFields;
  public static HashSet<String> openapiRequiredFields;

  static {
    // a set of all properties/fields (JSON key names)
    openapiFields = new HashSet<String>();
    openapiFields.add("token");
    openapiFields.add("redaction");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to V1DetokenizeRecordRequest
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!V1DetokenizeRecordRequest.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in V1DetokenizeRecordRequest is not found in the empty JSON string", V1DetokenizeRecordRequest.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!V1DetokenizeRecordRequest.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `V1DetokenizeRecordRequest` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if ((jsonObj.get("token") != null && !jsonObj.get("token").isJsonNull()) && !jsonObj.get("token").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `token` to be a primitive type in the JSON string but got `%s`", jsonObj.get("token").toString()));
      }
      // validate the optional field `redaction`
      if (jsonObj.get("redaction") != null && !jsonObj.get("redaction").isJsonNull()) {
        RedactionEnumREDACTION.validateJsonElement(jsonObj.get("redaction"));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!V1DetokenizeRecordRequest.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'V1DetokenizeRecordRequest' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<V1DetokenizeRecordRequest> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(V1DetokenizeRecordRequest.class));

       return (TypeAdapter<T>) new TypeAdapter<V1DetokenizeRecordRequest>() {
           @Override
           public void write(JsonWriter out, V1DetokenizeRecordRequest value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public V1DetokenizeRecordRequest read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of V1DetokenizeRecordRequest given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of V1DetokenizeRecordRequest
   * @throws IOException if the JSON string is invalid with respect to V1DetokenizeRecordRequest
   */
  public static V1DetokenizeRecordRequest fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, V1DetokenizeRecordRequest.class);
  }

  /**
   * Convert an instance of V1DetokenizeRecordRequest to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}
