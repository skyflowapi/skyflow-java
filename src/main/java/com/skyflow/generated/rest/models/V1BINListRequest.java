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
import com.skyflow.generated.rest.models.V1VaultSchemaConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * Request to return specific card metadata.
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-09-25T11:34:22.684345+05:30[Asia/Kolkata]", comments = "Generator version: 7.8.0")
public class V1BINListRequest {
  public static final String SERIALIZED_NAME_FIELDS = "fields";
  @SerializedName(SERIALIZED_NAME_FIELDS)
  private List<String> fields = new ArrayList<>();

  public static final String SERIALIZED_NAME_B_I_N = "BIN";
  @SerializedName(SERIALIZED_NAME_B_I_N)
  private String BIN;

  public static final String SERIALIZED_NAME_VAULT_SCHEMA_CONFIG = "vault_schema_config";
  @SerializedName(SERIALIZED_NAME_VAULT_SCHEMA_CONFIG)
  private V1VaultSchemaConfig vaultSchemaConfig;

  public static final String SERIALIZED_NAME_SKYFLOW_ID = "skyflow_id";
  @SerializedName(SERIALIZED_NAME_SKYFLOW_ID)
  private String skyflowId;

  public V1BINListRequest() {
  }

  public V1BINListRequest fields(List<String> fields) {
    this.fields = fields;
    return this;
  }

  public V1BINListRequest addFieldsItem(String fieldsItem) {
    if (this.fields == null) {
      this.fields = new ArrayList<>();
    }
    this.fields.add(fieldsItem);
    return this;
  }

  /**
   * Fields to return. If not specified, all fields are returned.
   * @return fields
   */
  @javax.annotation.Nullable
  public List<String> getFields() {
    return fields;
  }

  public void setFields(List<String> fields) {
    this.fields = fields;
  }


  public V1BINListRequest BIN(String BIN) {
    this.BIN = BIN;
    return this;
  }

  /**
   * BIN of the card.
   * @return BIN
   */
  @javax.annotation.Nullable
  public String getBIN() {
    return BIN;
  }

  public void setBIN(String BIN) {
    this.BIN = BIN;
  }


  public V1BINListRequest vaultSchemaConfig(V1VaultSchemaConfig vaultSchemaConfig) {
    this.vaultSchemaConfig = vaultSchemaConfig;
    return this;
  }

  /**
   * Get vaultSchemaConfig
   * @return vaultSchemaConfig
   */
  @javax.annotation.Nullable
  public V1VaultSchemaConfig getVaultSchemaConfig() {
    return vaultSchemaConfig;
  }

  public void setVaultSchemaConfig(V1VaultSchemaConfig vaultSchemaConfig) {
    this.vaultSchemaConfig = vaultSchemaConfig;
  }


  public V1BINListRequest skyflowId(String skyflowId) {
    this.skyflowId = skyflowId;
    return this;
  }

  /**
   * &lt;code&gt;skyflow_id&lt;/code&gt; of the record.
   * @return skyflowId
   */
  @javax.annotation.Nullable
  public String getSkyflowId() {
    return skyflowId;
  }

  public void setSkyflowId(String skyflowId) {
    this.skyflowId = skyflowId;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1BINListRequest v1BINListRequest = (V1BINListRequest) o;
    return Objects.equals(this.fields, v1BINListRequest.fields) &&
        Objects.equals(this.BIN, v1BINListRequest.BIN) &&
        Objects.equals(this.vaultSchemaConfig, v1BINListRequest.vaultSchemaConfig) &&
        Objects.equals(this.skyflowId, v1BINListRequest.skyflowId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fields, BIN, vaultSchemaConfig, skyflowId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1BINListRequest {\n");
    sb.append("    fields: ").append(toIndentedString(fields)).append("\n");
    sb.append("    BIN: ").append(toIndentedString(BIN)).append("\n");
    sb.append("    vaultSchemaConfig: ").append(toIndentedString(vaultSchemaConfig)).append("\n");
    sb.append("    skyflowId: ").append(toIndentedString(skyflowId)).append("\n");
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
    openapiFields.add("fields");
    openapiFields.add("BIN");
    openapiFields.add("vault_schema_config");
    openapiFields.add("skyflow_id");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to V1BINListRequest
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!V1BINListRequest.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in V1BINListRequest is not found in the empty JSON string", V1BINListRequest.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!V1BINListRequest.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `V1BINListRequest` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      // ensure the optional json data is an array if present
      if (jsonObj.get("fields") != null && !jsonObj.get("fields").isJsonNull() && !jsonObj.get("fields").isJsonArray()) {
        throw new IllegalArgumentException(String.format("Expected the field `fields` to be an array in the JSON string but got `%s`", jsonObj.get("fields").toString()));
      }
      if ((jsonObj.get("BIN") != null && !jsonObj.get("BIN").isJsonNull()) && !jsonObj.get("BIN").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `BIN` to be a primitive type in the JSON string but got `%s`", jsonObj.get("BIN").toString()));
      }
      // validate the optional field `vault_schema_config`
      if (jsonObj.get("vault_schema_config") != null && !jsonObj.get("vault_schema_config").isJsonNull()) {
        V1VaultSchemaConfig.validateJsonElement(jsonObj.get("vault_schema_config"));
      }
      if ((jsonObj.get("skyflow_id") != null && !jsonObj.get("skyflow_id").isJsonNull()) && !jsonObj.get("skyflow_id").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `skyflow_id` to be a primitive type in the JSON string but got `%s`", jsonObj.get("skyflow_id").toString()));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!V1BINListRequest.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'V1BINListRequest' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<V1BINListRequest> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(V1BINListRequest.class));

       return (TypeAdapter<T>) new TypeAdapter<V1BINListRequest>() {
           @Override
           public void write(JsonWriter out, V1BINListRequest value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public V1BINListRequest read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of V1BINListRequest given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of V1BINListRequest
   * @throws IOException if the JSON string is invalid with respect to V1BINListRequest
   */
  public static V1BINListRequest fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, V1BINListRequest.class);
  }

  /**
   * Convert an instance of V1BINListRequest to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

