/*
 * Skyflow Management API
 * # Management API  This API controls aspects of your account and schema, including workspaces, vaults, keys, users, permissions, and more.  The Management API is available from two base URIs:<ul><li><b>Sandbox:</b> https://manage.skyflowapis-preview.com</li><li><b>Production:</b> https://manage.skyflowapis.com</li></ul>  When you make an API call, you need to add two headers: <table><tr><th>Header</th><th>Value</th><th>Example</th></tr><tr><td>Authorization</td><td>A Bearer Token. See <a href='/api-authentication/'>API Authentication</a>.</td><td><code>Authorization: Bearer eyJhbGciOiJSUzI...1NiIsJdfPA</code></td></tr><tr><td>X-SKYFLOW-ACCOUNT-ID</td><td>Your Skyflow account ID.</td><td><code>X-SKYFLOW-ACCOUNT-ID: h451b763713e4424a7jke1bbkbbc84ef</code></td></tr><table/>
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
 * V1GetAuthTokenRequest
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-03T10:28:16.037563+05:30[Asia/Kolkata]", comments = "Generator version: 7.8.0")
public class V1GetAuthTokenRequest {
  public static final String SERIALIZED_NAME_GRANT_TYPE = "grant_type";
  @SerializedName(SERIALIZED_NAME_GRANT_TYPE)
  private String grantType;

  public static final String SERIALIZED_NAME_ASSERTION = "assertion";
  @SerializedName(SERIALIZED_NAME_ASSERTION)
  private String assertion;

  public static final String SERIALIZED_NAME_SUBJECT_TOKEN = "subject_token";
  @SerializedName(SERIALIZED_NAME_SUBJECT_TOKEN)
  private String subjectToken;

  public static final String SERIALIZED_NAME_SUBJECT_TOKEN_TYPE = "subject_token_type";
  @SerializedName(SERIALIZED_NAME_SUBJECT_TOKEN_TYPE)
  private String subjectTokenType;

  public static final String SERIALIZED_NAME_REQUESTED_TOKEN_USE = "requested_token_use";
  @SerializedName(SERIALIZED_NAME_REQUESTED_TOKEN_USE)
  private String requestedTokenUse;

  public static final String SERIALIZED_NAME_SCOPE = "scope";
  @SerializedName(SERIALIZED_NAME_SCOPE)
  private String scope;

  public V1GetAuthTokenRequest() {
  }

  public V1GetAuthTokenRequest grantType(String grantType) {
    this.grantType = grantType;
    return this;
  }

  /**
   * Grant type of the request. Set this to &#x60;urn:ietf:params:oauth:grant-type:jwt-bearer&#x60;.
   * @return grantType
   */
  @javax.annotation.Nonnull
  public String getGrantType() {
    return grantType;
  }

  public void setGrantType(String grantType) {
    this.grantType = grantType;
  }


  public V1GetAuthTokenRequest assertion(String assertion) {
    this.assertion = assertion;
    return this;
  }

  /**
   * User-signed JWT token that contains the following fields: &lt;br/&gt; &lt;ul&gt;&lt;li&gt;&lt;code&gt;iss&lt;/code&gt;: Issuer of the JWT.&lt;/li&gt;&lt;li&gt;&lt;code&gt;key&lt;/code&gt;: Unique identifier for the key.&lt;/li&gt;&lt;li&gt;&lt;code&gt;aud&lt;/code&gt;: Recipient the JWT is intended for.&lt;/li&gt;&lt;li&gt;&lt;code&gt;exp&lt;/code&gt;: Time the JWT expires.&lt;/li&gt;&lt;li&gt;&lt;code&gt;sub&lt;/code&gt;: Subject of the JWT.&lt;/li&gt;&lt;li&gt;&lt;code&gt;ctx&lt;/code&gt;: (Optional) Value for &lt;a href&#x3D;&#39;/context-aware-overview/&#39;&gt;Context-aware authorization&lt;/a&gt;.&lt;/li&gt;&lt;/ul&gt;
   * @return assertion
   */
  @javax.annotation.Nonnull
  public String getAssertion() {
    return assertion;
  }

  public void setAssertion(String assertion) {
    this.assertion = assertion;
  }


  public V1GetAuthTokenRequest subjectToken(String subjectToken) {
    this.subjectToken = subjectToken;
    return this;
  }

  /**
   * Subject token.
   * @return subjectToken
   */
  @javax.annotation.Nullable
  public String getSubjectToken() {
    return subjectToken;
  }

  public void setSubjectToken(String subjectToken) {
    this.subjectToken = subjectToken;
  }


  public V1GetAuthTokenRequest subjectTokenType(String subjectTokenType) {
    this.subjectTokenType = subjectTokenType;
    return this;
  }

  /**
   * Subject token type.
   * @return subjectTokenType
   */
  @javax.annotation.Nullable
  public String getSubjectTokenType() {
    return subjectTokenType;
  }

  public void setSubjectTokenType(String subjectTokenType) {
    this.subjectTokenType = subjectTokenType;
  }


  public V1GetAuthTokenRequest requestedTokenUse(String requestedTokenUse) {
    this.requestedTokenUse = requestedTokenUse;
    return this;
  }

  /**
   * Token use type. Either &#x60;delegation&#x60; or &#x60;impersonation&#x60;.
   * @return requestedTokenUse
   */
  @javax.annotation.Nullable
  public String getRequestedTokenUse() {
    return requestedTokenUse;
  }

  public void setRequestedTokenUse(String requestedTokenUse) {
    this.requestedTokenUse = requestedTokenUse;
  }


  public V1GetAuthTokenRequest scope(String scope) {
    this.scope = scope;
    return this;
  }

  /**
   * Subset of available &lt;a href&#x3D;&#39;#Roles&#39;&gt;roles&lt;/a&gt; to associate with the requested token. Uses the format \&quot;role:\\&lt;roleID1\\&gt; role:\\&lt;roleID2\\&gt;\&quot;.
   * @return scope
   */
  @javax.annotation.Nullable
  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1GetAuthTokenRequest v1GetAuthTokenRequest = (V1GetAuthTokenRequest) o;
    return Objects.equals(this.grantType, v1GetAuthTokenRequest.grantType) &&
        Objects.equals(this.assertion, v1GetAuthTokenRequest.assertion) &&
        Objects.equals(this.subjectToken, v1GetAuthTokenRequest.subjectToken) &&
        Objects.equals(this.subjectTokenType, v1GetAuthTokenRequest.subjectTokenType) &&
        Objects.equals(this.requestedTokenUse, v1GetAuthTokenRequest.requestedTokenUse) &&
        Objects.equals(this.scope, v1GetAuthTokenRequest.scope);
  }

  @Override
  public int hashCode() {
    return Objects.hash(grantType, assertion, subjectToken, subjectTokenType, requestedTokenUse, scope);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1GetAuthTokenRequest {\n");
    sb.append("    grantType: ").append(toIndentedString(grantType)).append("\n");
    sb.append("    assertion: ").append(toIndentedString(assertion)).append("\n");
    sb.append("    subjectToken: ").append(toIndentedString(subjectToken)).append("\n");
    sb.append("    subjectTokenType: ").append(toIndentedString(subjectTokenType)).append("\n");
    sb.append("    requestedTokenUse: ").append(toIndentedString(requestedTokenUse)).append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
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
    openapiFields.add("grant_type");
    openapiFields.add("assertion");
    openapiFields.add("subject_token");
    openapiFields.add("subject_token_type");
    openapiFields.add("requested_token_use");
    openapiFields.add("scope");

    // a set of required properties/fields (JSON key names)
    openapiRequiredFields = new HashSet<String>();
    openapiRequiredFields.add("grant_type");
    openapiRequiredFields.add("assertion");
  }

  /**
   * Validates the JSON Element and throws an exception if issues found
   *
   * @param jsonElement JSON Element
   * @throws IOException if the JSON Element is invalid with respect to V1GetAuthTokenRequest
   */
  public static void validateJsonElement(JsonElement jsonElement) throws IOException {
      if (jsonElement == null) {
        if (!V1GetAuthTokenRequest.openapiRequiredFields.isEmpty()) { // has required fields but JSON element is null
          throw new IllegalArgumentException(String.format("The required field(s) %s in V1GetAuthTokenRequest is not found in the empty JSON string", V1GetAuthTokenRequest.openapiRequiredFields.toString()));
        }
      }

      Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
      // check to see if the JSON string contains additional fields
      for (Map.Entry<String, JsonElement> entry : entries) {
        if (!V1GetAuthTokenRequest.openapiFields.contains(entry.getKey())) {
          throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `V1GetAuthTokenRequest` properties. JSON: %s", entry.getKey(), jsonElement.toString()));
        }
      }

      // check to make sure all required properties/fields are present in the JSON string
      for (String requiredField : V1GetAuthTokenRequest.openapiRequiredFields) {
        if (jsonElement.getAsJsonObject().get(requiredField) == null) {
          throw new IllegalArgumentException(String.format("The required field `%s` is not found in the JSON string: %s", requiredField, jsonElement.toString()));
        }
      }
        JsonObject jsonObj = jsonElement.getAsJsonObject();
      if (!jsonObj.get("grant_type").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `grant_type` to be a primitive type in the JSON string but got `%s`", jsonObj.get("grant_type").toString()));
      }
      if (!jsonObj.get("assertion").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `assertion` to be a primitive type in the JSON string but got `%s`", jsonObj.get("assertion").toString()));
      }
      if ((jsonObj.get("subject_token") != null && !jsonObj.get("subject_token").isJsonNull()) && !jsonObj.get("subject_token").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `subject_token` to be a primitive type in the JSON string but got `%s`", jsonObj.get("subject_token").toString()));
      }
      if ((jsonObj.get("subject_token_type") != null && !jsonObj.get("subject_token_type").isJsonNull()) && !jsonObj.get("subject_token_type").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `subject_token_type` to be a primitive type in the JSON string but got `%s`", jsonObj.get("subject_token_type").toString()));
      }
      if ((jsonObj.get("requested_token_use") != null && !jsonObj.get("requested_token_use").isJsonNull()) && !jsonObj.get("requested_token_use").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `requested_token_use` to be a primitive type in the JSON string but got `%s`", jsonObj.get("requested_token_use").toString()));
      }
      if ((jsonObj.get("scope") != null && !jsonObj.get("scope").isJsonNull()) && !jsonObj.get("scope").isJsonPrimitive()) {
        throw new IllegalArgumentException(String.format("Expected the field `scope` to be a primitive type in the JSON string but got `%s`", jsonObj.get("scope").toString()));
      }
  }

  public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
       if (!V1GetAuthTokenRequest.class.isAssignableFrom(type.getRawType())) {
         return null; // this class only serializes 'V1GetAuthTokenRequest' and its subtypes
       }
       final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
       final TypeAdapter<V1GetAuthTokenRequest> thisAdapter
                        = gson.getDelegateAdapter(this, TypeToken.get(V1GetAuthTokenRequest.class));

       return (TypeAdapter<T>) new TypeAdapter<V1GetAuthTokenRequest>() {
           @Override
           public void write(JsonWriter out, V1GetAuthTokenRequest value) throws IOException {
             JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
             elementAdapter.write(out, obj);
           }

           @Override
           public V1GetAuthTokenRequest read(JsonReader in) throws IOException {
             JsonElement jsonElement = elementAdapter.read(in);
             validateJsonElement(jsonElement);
             return thisAdapter.fromJsonTree(jsonElement);
           }

       }.nullSafe();
    }
  }

  /**
   * Create an instance of V1GetAuthTokenRequest given an JSON string
   *
   * @param jsonString JSON string
   * @return An instance of V1GetAuthTokenRequest
   * @throws IOException if the JSON string is invalid with respect to V1GetAuthTokenRequest
   */
  public static V1GetAuthTokenRequest fromJson(String jsonString) throws IOException {
    return JSON.getGson().fromJson(jsonString, V1GetAuthTokenRequest.class);
  }

  /**
   * Convert an instance of V1GetAuthTokenRequest to an JSON string
   *
   * @return JSON string
   */
  public String toJson() {
    return JSON.getGson().toJson(this);
  }
}
