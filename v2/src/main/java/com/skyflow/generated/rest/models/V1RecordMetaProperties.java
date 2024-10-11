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
 * V1RecordMetaProperties
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-09-25T11:34:22.684345+05:30[Asia/Kolkata]", comments = "Generator version: 7.8.0")
public class V1RecordMetaProperties {
  public static final String SERIALIZED_NAME_SKYFLOW_ID = "skyflow_id";
  @SerializedName(SERIALIZED_NAME_SKYFLOW_ID)
  private String skyflowId;

  public static final String SERIALIZED_NAME_TOKENS = "tokens";
  @SerializedName(SERIALIZED_NAME_TOKENS)
  private Object tokens;

  public V1RecordMetaProperties() {
  }

  public V1RecordMetaProperties skyflowId(String skyflowId) {
    this.skyflowId = skyflowId;
    return this;
  }

  /**
   * ID of the inserted record.
   * @return skyflowId
   */
  @javax.annotation.Nullable
  public String getSkyflowId() {
    return skyflowId;
  }

  public void setSkyflowId(String skyflowId) {
    this.skyflowId = skyflowId;
  }


  public V1RecordMetaProperties tokens(Object tokens) {
    this.tokens = tokens;
    return this;
  }

  /**
   * Tokens for the record.
   * @return tokens
   */
  @javax.annotation.Nullable
  public Object getTokens() {
    return tokens;
  }

  public void setTokens(Object tokens) {
    this.tokens = tokens;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1RecordMetaProperties v1RecordMetaProperties = (V1RecordMetaProperties) o;
    return Objects.equals(this.skyflowId, v1RecordMetaProperties.skyflowId) &&
        Objects.equals(this.tokens, v1RecordMetaProperties.tokens);
  }

  @Override
  public int hashCode() {
    return Objects.hash(skyflowId, tokens);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1RecordMetaProperties {\n");
    sb.append("    skyflowId: ").append(toIndentedString(skyflowId)).append("\n");
    sb.append("    tokens: ").append(toIndentedString(tokens)).append("\n");
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
    openapiFields.add("skyflow_id");
    openapiFields.add("tokens");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to V1RecordMetaProperties
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!V1RecordMetaProperties.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in V1RecordMetaProperties is not found in the empty JSON string", V1RecordMetaProperties.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!V1RecordMetaProperties.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `V1RecordMetaProperties` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if ((jsonObj.get("skyflow_id") != null && !jsonObj.get("skyflow_id").isJsonNull()) && !jsonObj.get("skyflow_id").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `skyflow_id` to be a primitive type in the JSON string but got `%s`", jsonObj.get("skyflow_id").toString()));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!V1RecordMetaProperties.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'V1RecordMetaProperties' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<V1RecordMetaProperties> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(V1RecordMetaProperties.class));

       return (TypeAdapter<T>) new TypeAdapter<V1RecordMetaProperties>() {
           @Override
           public void write(JsonWriter out, V1RecordMetaProperties value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public V1RecordMetaProperties read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of V1RecordMetaProperties given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of V1RecordMetaProperties
   * @throws IOException if the JSON string is invalid with respect to V1RecordMetaProperties
   */
  public static V1RecordMetaProperties fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, V1RecordMetaProperties.class);
  }

  /**
   * Convert an instance of V1RecordMetaProperties to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}
