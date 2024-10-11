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
import com.skyflow.generated.rest.models.V1AuditAfterOptions;
import com.skyflow.generated.rest.models.V1AuditResponseEvent;
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
 * V1AuditResponse
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-09-25T11:34:22.684345+05:30[Asia/Kolkata]", comments = "Generator version: 7.8.0")
public class V1AuditResponse {
  public static final String SERIALIZED_NAME_EVENT = "event";
  @SerializedName(SERIALIZED_NAME_EVENT)
  private List<V1AuditResponseEvent> event = new ArrayList<>();

  public static final String SERIALIZED_NAME_NEXT_OPS = "nextOps";
  @SerializedName(SERIALIZED_NAME_NEXT_OPS)
  private V1AuditAfterOptions nextOps;

  public V1AuditResponse() {
  }

  public V1AuditResponse event(List<V1AuditResponseEvent> event) {
    this.event = event;
    return this;
  }

  public V1AuditResponse addEventItem(V1AuditResponseEvent eventItem) {
    if (this.event == null) {
      this.event = new ArrayList<>();
    }
    this.event.add(eventItem);
    return this;
  }

  /**
   * Events matching the query.
   * @return event
   */
  @javax.annotation.Nullable
  public List<V1AuditResponseEvent> getEvent() {
    return event;
  }

  public void setEvent(List<V1AuditResponseEvent> event) {
    this.event = event;
  }


  public V1AuditResponse nextOps(V1AuditAfterOptions nextOps) {
    this.nextOps = nextOps;
    return this;
  }

  /**
   * Get nextOps
   * @return nextOps
   */
  @javax.annotation.Nullable
  public V1AuditAfterOptions getNextOps() {
    return nextOps;
  }

  public void setNextOps(V1AuditAfterOptions nextOps) {
    this.nextOps = nextOps;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1AuditResponse v1AuditResponse = (V1AuditResponse) o;
    return Objects.equals(this.event, v1AuditResponse.event) &&
        Objects.equals(this.nextOps, v1AuditResponse.nextOps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(event, nextOps);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1AuditResponse {\n");
    sb.append("    event: ").append(toIndentedString(event)).append("\n");
    sb.append("    nextOps: ").append(toIndentedString(nextOps)).append("\n");
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
    openapiFields.add("event");
    openapiFields.add("nextOps");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to V1AuditResponse
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!V1AuditResponse.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in V1AuditResponse is not found in the empty JSON string", V1AuditResponse.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!V1AuditResponse.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `V1AuditResponse` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if (jsonObj.get("event") != null && !jsonObj.get("event").isJsonNull()) {
        JsonArray jsonArrayevent = jsonObj.getAsJsonArray("event");
        if (jsonArrayevent != null) {
          // ensure the json data is an array
          if (!jsonObj.get("event").isJsonArray()) {
            throw new IllegalArgumentException(String.format("Expected the field `event` to be an array in the JSON string but got `%s`", jsonObj.get("event").toString()));
          }

          // validate the optional field `event` (array)
          for (int i = 0; i < jsonArrayevent.size(); i++) {
            V1AuditResponseEvent.validateJsonElement(jsonArrayevent.get(i));
          };
        }
      }
      // validate the optional field `nextOps`
      if (jsonObj.get("nextOps") != null && !jsonObj.get("nextOps").isJsonNull()) {
        V1AuditAfterOptions.validateJsonElement(jsonObj.get("nextOps"));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!V1AuditResponse.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'V1AuditResponse' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<V1AuditResponse> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(V1AuditResponse.class));

       return (TypeAdapter<T>) new TypeAdapter<V1AuditResponse>() {
           @Override
           public void write(JsonWriter out, V1AuditResponse value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public V1AuditResponse read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of V1AuditResponse given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of V1AuditResponse
   * @throws IOException if the JSON string is invalid with respect to V1AuditResponse
   */
  public static V1AuditResponse fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, V1AuditResponse.class);
  }

  /**
   * Convert an instance of V1AuditResponse to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}
