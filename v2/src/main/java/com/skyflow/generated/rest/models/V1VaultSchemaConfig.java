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
import com.skyflow.generated.rest.models.V1VaultFieldMapping;
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
 * Details of the vault that stores additional card details.
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-09-25T11:34:22.684345+05:30[Asia/Kolkata]", comments = "Generator version: 7.8.0")
public class V1VaultSchemaConfig {
  public static final String SERIALIZED_NAME_ID = "id";
  @SerializedName(SERIALIZED_NAME_ID)
  private String id;

  public static final String SERIALIZED_NAME_TABLE_NAME = "table_name";
  @SerializedName(SERIALIZED_NAME_TABLE_NAME)
  private String tableName;

  public static final String SERIALIZED_NAME_MAPPING = "mapping";
  @SerializedName(SERIALIZED_NAME_MAPPING)
  private V1VaultFieldMapping mapping;

  public V1VaultSchemaConfig() {
  }

  public V1VaultSchemaConfig id(String id) {
    this.id = id;
    return this;
  }

  /**
   * ID of the vault that stores card details.
   * @return id
   */
  @javax.annotation.Nullable
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }


  public V1VaultSchemaConfig tableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  /**
   * Name of the table that stores card details.
   * @return tableName
   */
  @javax.annotation.Nullable
  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }


  public V1VaultSchemaConfig mapping(V1VaultFieldMapping mapping) {
    this.mapping = mapping;
    return this;
  }

  /**
   * Get mapping
   * @return mapping
   */
  @javax.annotation.Nullable
  public V1VaultFieldMapping getMapping() {
    return mapping;
  }

  public void setMapping(V1VaultFieldMapping mapping) {
    this.mapping = mapping;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1VaultSchemaConfig v1VaultSchemaConfig = (V1VaultSchemaConfig) o;
    return Objects.equals(this.id, v1VaultSchemaConfig.id) &&
        Objects.equals(this.tableName, v1VaultSchemaConfig.tableName) &&
        Objects.equals(this.mapping, v1VaultSchemaConfig.mapping);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, tableName, mapping);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1VaultSchemaConfig {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    tableName: ").append(toIndentedString(tableName)).append("\n");
    sb.append("    mapping: ").append(toIndentedString(mapping)).append("\n");
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
    openapiFields.add("id");
    openapiFields.add("table_name");
    openapiFields.add("mapping");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to V1VaultSchemaConfig
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!V1VaultSchemaConfig.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in V1VaultSchemaConfig is not found in the empty JSON string", V1VaultSchemaConfig.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!V1VaultSchemaConfig.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `V1VaultSchemaConfig` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if ((jsonObj.get("id") != null && !jsonObj.get("id").isJsonNull()) && !jsonObj.get("id").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `id` to be a primitive type in the JSON string but got `%s`", jsonObj.get("id").toString()));
      }
      if ((jsonObj.get("table_name") != null && !jsonObj.get("table_name").isJsonNull()) && !jsonObj.get("table_name").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `table_name` to be a primitive type in the JSON string but got `%s`", jsonObj.get("table_name").toString()));
      }
      // validate the optional field `mapping`
      if (jsonObj.get("mapping") != null && !jsonObj.get("mapping").isJsonNull()) {
        V1VaultFieldMapping.validateJsonElement(jsonObj.get("mapping"));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!V1VaultSchemaConfig.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'V1VaultSchemaConfig' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<V1VaultSchemaConfig> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(V1VaultSchemaConfig.class));

       return (TypeAdapter<T>) new TypeAdapter<V1VaultSchemaConfig>() {
           @Override
           public void write(JsonWriter out, V1VaultSchemaConfig value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public V1VaultSchemaConfig read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of V1VaultSchemaConfig given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of V1VaultSchemaConfig
   * @throws IOException if the JSON string is invalid with respect to V1VaultSchemaConfig
   */
  public static V1VaultSchemaConfig fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, V1VaultSchemaConfig.class);
  }

  /**
   * Convert an instance of V1VaultSchemaConfig to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}

