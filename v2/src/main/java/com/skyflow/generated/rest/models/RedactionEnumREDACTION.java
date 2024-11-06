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
 * Redaction type. Subject to policies assigned to the API caller. When used for detokenization, only supported for vaults that support [column groups](/tokenization-column-groups/).
 */
@JsonAdapter(RedactionEnumREDACTION.Adapter.class)
public enum RedactionEnumREDACTION {
  
  DEFAULT("DEFAULT"),
  
  REDACTED("REDACTED"),
  
  MASKED("MASKED"),
  
  PLAIN_TEXT("PLAIN_TEXT");

  private String value;

  RedactionEnumREDACTION(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static RedactionEnumREDACTION fromValue(String value) {
    for (RedactionEnumREDACTION b : RedactionEnumREDACTION.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  public static class Adapter extends TypeAdapter<RedactionEnumREDACTION> {
    @Override
    public void write(final JsonWriter jsonWriter, final RedactionEnumREDACTION enumeration) throws IOException {
      jsonWriter.value(enumeration.getValue());
    }

    @Override
    public RedactionEnumREDACTION read(final JsonReader jsonReader) throws IOException {
      String value = jsonReader.nextString();
      return RedactionEnumREDACTION.fromValue(value);
    }
  }

  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
    String value = jsonElement.getAsString();
    RedactionEnumREDACTION.fromValue(value);
  }
}

