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
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import com.google.gson.TypeAdapter;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Token insertion behavior.   - DISABLE: Tokens aren&#39;t allowed for any fields. If tokens are specified, the request fails.  - ENABLE: Tokens are allowed—but not required—for all fields. If tokens are specified, they&#39;re inserted.  - ENABLE_STRICT: Tokens are required for all fields. If tokens are specified, they&#39;re inserted. If not, the request fails.
 */
@JsonAdapter(V1BYOT.Adapter.class)
public enum V1BYOT {
  
  DISABLE("DISABLE"),
  
  ENABLE("ENABLE"),
  
  ENABLE_STRICT("ENABLE_STRICT");

  private String value;

  V1BYOT(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static V1BYOT fromValue(String value) {
    for (V1BYOT b : V1BYOT.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  public static class Adapter extends TypeAdapter<V1BYOT> {
    @Override
    public void write(final JsonWriter jsonWriter, final V1BYOT enumeration) throws IOException {
      jsonWriter.value(enumeration.getValue());
    }

    @Override
    public V1BYOT read(final JsonReader jsonReader) throws IOException {
      String value = jsonReader.nextString();
      return V1BYOT.fromValue(value);
    }
  }

  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
    String value = jsonElement.getAsString();
    V1BYOT.fromValue(value);
  }
}
