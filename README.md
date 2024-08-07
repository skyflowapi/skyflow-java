# Skyflow Java
The Skyflow Java SDK is designed to help with integrating Skyflow into a Java backend.

[![CI](https://img.shields.io/static/v1?label=CI&message=passing&color=green?style=plastic&logo=github)](https://github.com/skyflowapi/skyflow-java/actions)
[![GitHub release](https://img.shields.io/github/v/release/skyflowapi/skyflow-java.svg)](https://mvnrepository.com/artifact/com.skyflow/skyflow-java)
[![License](https://img.shields.io/github/license/skyflowapi/skyflow-java)](https://github.com/skyflowapi/skyflow-java/blob/main/LICENSE)

# Table of Contents

- [Skyflow Java](#skyflow-java)
- [Table of Contents](#table-of-contents)
  - [Features](#features)
  - [Installation](#installation)
    - [Requirements](#requirements)
    - [Configuration](#configuration)
      - [Gradle users](#gradle-users)
      - [Maven users](#maven-users)
  - [Service Account Bearer Token Generation](#service-account-bearer-token-generation)
  - [Service Account Bearer Token with Context Generation](#service-account-bearer-token-with-context-generation)
  - [Service Account Scoped Bearer Token Generation](#service-account-scoped-bearer-token-generation)
  - [Signed Data Tokens Generation](#signed-data-tokens-generation)
  - [Vault APIs](#vault-apis)
    - [Insert](#insert)
    - [InsertBulk](#insertbulk)
    - [Detokenize](#detokenize)
    - [Get](#get)
      - [Use Skyflow IDs](#use-skyflow-ids)
      - [Use column name and values](#use-column-name-and-values)
      - [Redaction types](#redaction-types)
      - [Examples](#examples)
    - [GetById](#getbyid)
    - [Update](#update)
    - [Delete](#delete)
    - [Invoke Connection](#invoke-connection)
    - [Query](#query)
  - [Logging](#logging)
  - [Reporting a Vulnerability](#reporting-a-vulnerability)

## Features

- Authenticate with a Skyflow service account and generate a bearer token.
- Insert, retrieve and tokenize sensitive data.
- Invoke connections to call downstream third party APIs without directly handling sensitive data.

## Installation

### Requirements
- Java 1.8 and above

### Configuration
---
#### Gradle users

Add this dependency to your project's build file:
```
implementation 'com.skyflow:skyflow-java:1.15.0'
```

#### Maven users
Add this dependency to your project's POM:

```xml
    <dependency>
        <groupId>com.skyflow</groupId>
        <artifactId>skyflow-java</artifactId>
        <version>1.15.0</version>
    </dependency>
```
---

## Service Account Bearer Token Generation
The [Service Account](https://github.com/skyflowapi/skyflow-java/tree/main/src/main/java/com/skyflow/serviceaccount) java module is used to generate service account tokens from service account credentials file which is downloaded upon creation of service account. The token generated from this module is valid for 60 minutes and can be used to make API calls to vault services as well as management API(s) based on the permissions of the service account.

The `generateBearerToken(filepath)` function takes the credentials file path for token generation, alternatively, you can also send the entire credentials as string, by using `generateBearerTokenFromCreds(credentials)` 

[Example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/TokenGenerationExample.java
):

```java

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.entities.ResponseToken;

public class TokenGenerationUtil {

    private static String bearerToken = null;

    public static String getSkyflowBearerToken() {
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            if(Token.isExpired(bearerToken)) {
                ResponseToken response = Token.generateBearerToken(filePath);
                // or Token.generateBearerTokenFromCreds(credentialsString) 
                bearerToken = response.getAccessToken();
            }
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

        return bearerToken;
    }
}
```

## Service Account Bearer Token with Context Generation 

Context-Aware Authorization enables you to embed context values into a Bearer token when you generate it, and reference those values in your policies for more dynamic access control of data in the vault or validating signed data tokens during detokenization. It can be used to track end user identity when making API calls using service accounts.

The service account generated with `context_id` identifier enabled can be used to generate bearer tokens with `context`, which is a `jwt` claim for a skyflow generated bearer token. The token generated from this service account will have a `context_identifier` claim and is valid for 60 minutes and can be used to make API calls to vault services as well as management API(s) based on the permissions of the service account.

[Example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/BearerTokenWithContextGenerationExample.java):

``` java
import com.skyflow.entities.ResponseToken;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;
import java.io.File;

public class BearerTokenWithContextGeneration {
    public static void main(String args[]) {
        String bearerToken = null;
        // Generate a bearer token using a service account key file with a context value of "abc".
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials(new File(filePath))
                .setContext("abc")
                .build();

            bearerToken = token.getBearerToken();
            System.out.println(bearerToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Generate a bearer token using a service account key string with a context value of "abc".
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials(fileContents)
                .setContext("abc")
                .build();

            bearerToken = token.getBearerToken();
            System.out.println(bearerToken);

        } catch (SkyflowException e) {
            e.printStackTrace();
        }
    }
}
```

Note: 
- You can pass either a service account key credentials file path or a service account key credentials as string to the `setCredentials` method of the BearerTokenBuilder class.
- If you pass both a file path and string to the `setCredentials` method, the last method used takes precedence.
- To generate multiple bearer tokens using a thread, see this [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/BearerTokenGenerationUsingThreadsExample.java)

## Service Account Scoped Bearer Token Generation

A service account that has multiple roles can generate bearer tokens with access restricted to a specific role by providing the appropriate `roleID`. Generated bearer tokens are valid for 60 minutes and can only perform operations with the permissions associated with the specified role.

[Example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/ScopedTokenGenerationExample.java):

```java
import java.io.File;

public class ScopedTokenGeneration {
    public static void main(String args[]) {
        String scopedToken = null;
        // Generate a bearer token using a service account file path scoped to a specific role.
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            BearerToken token = new BearerToken.BearerTokenBuilder()
                .setCredentials(new File(filePath))
                .setRoles(new String[] {
                    "roleID"
                })
                .build();

            scopedToken = token.getBearerToken();
            System.out.println(scopedToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
Note: 
- You can pass either a service account key credentials file path or a service account key credentials as string to the `setCredentials` method of the BearerTokenBuilder class.
- If you pass both a file path and string to the `setCredentials` method, the last method used takes precedence.
## Signed Data Tokens Generation

Skyflow generates data tokens when sensitive data is inserted into the vault. These data tokens can be digitally signed with the private key of the service account credentials, which adds an additional layer of protection. Signed tokens can be detokenized by passing the signed data token and a bearer token generated from service account credentials. The service account must have appropriate permissions and context to detokenize the signed data tokens.


[Example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/SignedTokenGenerationExample.java):

``` java
import com.skyflow.errors.SkyflowException;
import java.io.File;
import java.util.List;

public class SignedTokenGeneration {
    public static void main(String args[]) {
        List < SignedDataTokenResponse > signedTokenValue;
        // Generate signed data tokens using a service account file path, context information, and a time to live.
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            String context = "abc";
            SignedDataTokens signedToken = new SignedDataTokens.SignedDataTokensBuilder()
                .setCredentials(new File(filePath))
                .setContext(context)
                .setTimeToLive(30.0) // Time to live set in seconds.
                .setDataTokens(new String[] {
                    "dataToken1"
                }).build();

            signedTokenValue = signedToken.getSignedDataTokens();
            System.out.println(signedTokenValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Generate signed data tokens using a service account key string, context information, and a time to live.
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            String context = "abc";
            SignedDataTokens signedToken = new SignedDataTokens.SignedDataTokensBuilder()
                .setCredentials(fileContents)
                .setContext(context)
                .setTimeToLive(30.0) // Time to live set in seconds.
                .setDataTokens(new String[] {
                    "dataToken1"
                }).build();

            signedTokenValue = signedToken.getSignedDataTokens();
            System.out.println(signedTokenValue);

        } catch (SkyflowException e) {
            e.printStackTrace();
        }
    }
}
```
Response:

``` java
[
    {
        "token":"5530-4316-0674-5748",
        "signedToken":"signed_token_eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJzLCpZjA"
    }
]
```
Note: 
- You can pass either a service account key credentials file path or a service account key credentials as string to the `setCredentials` method of the SignedDataTokensBuilder class.
- If you pass both a file path and string to the `setCredentials` method, the last method used takes precedence.
- Time to live value expects time as seconds.
- The default time to live value is 60 seconds.

## Vault APIs
The [Vault](https://github.com/skyflowapi/skyflow-java/tree/main/src/main/java/com/skyflow/vault) module is used to perform operations on the vault such as inserting records, detokenizing tokens, retrieving tokens for a skyflow_id and to invoke a connection.

To use this module, the skyflow client must first be initialized as follows.
```java
import com.skyflow.vault.Skyflow;
import com.skyflow.entities.SkyflowConfiguration;

// DemoTokenProvider class is an implementation of the TokenProvider interface
DemoTokenProvider demoTokenProvider = new DemoTokenProvider(); 

SkyflowConfiguration skyflowConfig = new SkyflowConfiguration(<VAULT_ID>,<VAULT_URL>,demoTokenProvider);

Skyflow skyflowClient = Skyflow.init(skyflowConfig);
```
Example implementation of DemoTokenProvider using credentials file path is as follows
```java
import com.skyflow.entities.TokenProvider;

static class DemoTokenProvider implements TokenProvider {
    
        @Override
        public String getBearerToken() throws Exception {
            ResponseToken res = null;
            try {
                String filePath = "<your_credentials_file_path>";
                res = Token.generateBearerToken(filePath);
            } catch (SkyflowException e) {
                e.printStackTrace();
            }
            return res.getAccessToken();
        }
    }
```

Example implementation of DemoTokenProvider using credentials file content is as follows
```java
import com.skyflow.entities.TokenProvider;

static class DemoTokenProvider implements TokenProvider { 
    @Override
    public String getBearerToken() throws Exception {
        ResponseToken res = null;
        try {
            String filePath = "<your_credentials_file_content>";
            res = Token.generateBearerTokenFromCreds(filePath);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
        return res.getAccessToken();
    }
    return res.getAccessToken();
}
```

All Vault APIs must be invoked using a client instance.

## Insert

To insert data into your vault, use the **insert(JSONObject insertInput, InsertOptions options)** method. The first parameter `insertInput` is a JSON object that must have a `records` key and takes an array of records to insert into the vault as a value. The second parameter, `options` is an `InsertOptions` object that provides further options for your insert call, including **upsert** operations as shown below:
```java
import com.skyflow.entities.InsertOptions;
import com.skyflow.entities.UpsertOption;
// initialize Skyflow

// construct insert input
JSONObject records = new JSONObject();
JSONArray recordsArray = new JSONArray();

JSONObject record = new JSONObject();
record.put("table", "<your_table_name>");

JSONObject fields = new JSONObject();
fields.put("<field_name>", "<field_value>");
record.put("fields", fields);
recordsArray.add(record);
records.put("records", recordsArray);

// Create an upsert option and insert it into the UpsertOption array.
UpsertOption[] upsertOptions = new UpsertOption[1];
upsertOptions[0] = new UpsertOption(
          '<table_name>',    // Table name.
          '<unique_column_name>' // Unique column in the table.
        );
// Indicates whether or not tokens should be returned for the inserted data. Defaults to 'True'
InsertOptions insertOptions = new InsertOptions(
            true,
            upsertOptions
        );
   
```
An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/InsertWithUpsertExample.java) of insert call with upsert support
```java
JSONObject recordsJson = new JSONObject();
JSONArray recordsArrayJson = new JSONArray();

JSONObject recordJson = new JSONObject();
recordJson.put("table", "cards");

JSONObject fieldsJson = new JSONObject();
fields.put("cardNumber", "41111111111");
fields.put("cvv","123");

recordJson.put("fields", fieldsJson);
recordsArrayJson.add(record);
recordsJson.put("records", recordsArrayJson);

// Create an Uupsert option and insert it into the UpsertOptions array.
UpsertOption[] upsertOptions = new UpsertOption[1];
upsertOptions[0] = new UpsertOption("cards", "cardNumber");

// Pass Upsert options in the insert method options.
InsertOptions insertOptions = new InsertOptions(true, upsertOptions);

try {
    JSONObject insertResponse = skyflowClient.insert(records,insertOptions);
    System.out.println(insertResponse);
} catch (SkyflowException exception) {
    System.out.println(exception);
}
```
Sample insert Response
```js
{
    "records": [
        {
            "table": "cards",
            "fields": {
                "skyflow_id": "16419435-aa63-4823-aae7-19c6a2d6a19f",
                "cardNumber": "f3907186-e7e2-466f-91e5-48e12c2bcbc1",
                "cvv": "1989cb56-63da-4482-a2df-1f74cd0dd1a5",
            },
        }
    ]
}
```

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/InsertWithContinueOnErrorExample.java) of Insert call with `continueOnError` support:
```java
JSONObject records = new JSONObject();
JSONArray recordsArray = new JSONArray();

JSONObject invalidRecord = new JSONObject();
invalidRecord.put("table", "cards");
JSONObject invalidRecordFields = new JSONObject();
invalidRecordFields.put("namee", "john doe");
invalidRecordFields.put("card_number", "4111111111111111");
invalidRecordFields.put("cvv", "1125");
invalidRecord.put("fields", invalidRecordFields);

JSONObject validRecord = new JSONObject();
validRecord.put("table", "cards");
JSONObject validRecordFields = new JSONObject();
validRecordFields.put("name", "jane doe");
validRecordFields.put("card_number", "4111111111111111");
validRecordFields.put("cvv", "1125");
validRecord.put("fields", validRecordFields);

recordsArray.add(invalidRecord);
recordsArray.add(validRecord);
records.put("records", recordsArray);

try {
    InsertOptions insertOptions = new InsertOptions(true, true);
    JSONObject insertResponse = skyflowClient.insert(records, insertOptions);
    System.out.println(insertResponse);
} catch (SkyflowException e) {
    System.out.println(e);
    e.printStackTrace();
}
```

Sample Response:
```js
{
    "records": [
        {
            "table": "cards",
            "fields": {
                "skyflow_id": "16419435-aa63-4823-aae7-19c6a2d6a19f",
                "cardNumber": "f3907186-e7e2-466f-91e5-48e12c2bcbc1",
                "cvv": "1989cb56-63da-4482-a2df-1f74cd0dd1a5",
                "name": "245d3a0f-a2d3-443b-8a20-8c17de86e186",
            },
            "request_index": 1,
        }
    ],
    "errors": [
        {
            "error": {
                "code":400,
                "description":"Invalid field present in JSON namee - requestId: 87fb2e32-6287-4e61-8304-9268df12bfe8",
                "request_index": 0,
            }
        }
    ]
}
```

## InsertBulk

To insert data into your vault using Bulk operation, use the **insertBulk(JSONObject insertInput, InsertBulkOptions options)** method. The first parameter `insertInput` is a JSON object that must have a `records` key and takes an array of records to insert into the vault as a value. The second parameter, `options` is an `InsertOptions` object that provides further options for your insert call, including **upsert** operations as shown below:

```java
import com.skyflow.entities.InsertOptions;
import com.skyflow.entities.UpsertOption;
// initialize Skyflow

// construct insert input
JSONObject records = new JSONObject();
JSONArray recordsArray = new JSONArray();

JSONObject record = new JSONObject();
record.put("table", "<your_table_name>");

JSONObject fields = new JSONObject();
fields.put("<field_name>", "<field_value>");
record.put("fields", fields);
recordsArray.add(record);
records.put("records", recordsArray);

// Create an upsert option and insert it into the UpsertOption array.
UpsertOption[] upsertOptions = new UpsertOption[1];
upsertOptions[0] = new UpsertOption(
          '<table_name>',    // Table name.
          '<unique_column_name>' // Unique column in the table.
        );
// Indicates whether or not tokens should be returned for the inserted data. Defaults to 'True'
InsertBulkOptions insertOptions = new InsertBulkOptions(
            true,
            upsertOptions
        );
```
An [example]() of insert call with upsert support:

```java
JSONObject recordsJson = new JSONObject();
JSONArray recordsArrayJson = new JSONArray();

JSONObject recordJson = new JSONObject();
recordJson.put("table", "cards");

JSONObject fieldsJson = new JSONObject();
fields.put("cardNumber", "41111111111");
fields.put("cvv","123");

recordJson.put("fields", fieldsJson);
recordsArrayJson.add(record);
recordsJson.put("records", recordsArrayJson);

// Create an Uupsert option and insert it into the UpsertOptions array.
UpsertOption[] upsertOptions = new UpsertOption[1];
upsertOptions[0] = new UpsertOption("cards", "cardNumber");

// Pass Upsert options in the insert method options.
InsertBulkOptions insertOptions = new InsertBulkOptions(true, upsertOptions);

try {
    JSONObject insertResponse = skyflowClient.insertBulk(records,insertOptions);
    System.out.println(insertResponse);
} catch (SkyflowException exception) {
    System.out.println(exception);
}
```
Sample insert Response
```js
{
    "records": [
        {
            "table": "cards",
            "fields": {
                "skyflow_id": "16419435-aa63-4823-aae7-19c6a2d6a19f",
                "cardNumber": "f3907186-e7e2-466f-91e5-48e12c2bcbc1",
                "cvv": "1989cb56-63da-4482-a2df-1f74cd0dd1a5",
            },
        }
    ]
}
```

An [example]() of Insert using bulk call:
```java
JSONObject records = new JSONObject();
JSONArray recordsArray = new JSONArray();

JSONObject invalidRecord = new JSONObject();
invalidRecord.put("table", "cards");
JSONObject invalidRecordFields = new JSONObject();
invalidRecordFields.put("namee", "john doe");
invalidRecordFields.put("card_number", "4111111111111111");
invalidRecordFields.put("cvv", "1125");
invalidRecord.put("fields", invalidRecordFields);

JSONObject validRecord = new JSONObject();
validRecord.put("table", "cards");
JSONObject validRecordFields = new JSONObject();
validRecordFields.put("name", "jane doe");
validRecordFields.put("card_number", "4111111111111111");
validRecordFields.put("cvv", "1125");
validRecord.put("fields", validRecordFields);

recordsArray.add(invalidRecord);
recordsArray.add(validRecord);
records.put("records", recordsArray);

try {
    InsertBulkOptions insertOptions = new InsertBulkOptions(true);
    JSONObject insertResponse = skyflowClient.insertBulk(records, insertOptions);
    System.out.println(insertResponse);
} catch (SkyflowException e) {
    System.out.println(e);
    e.printStackTrace();
}
```

Sample Response:
```js
{
    "records": [
        {
            "table": "cards",
            "fields": {
                "skyflow_id": "16419435-aa63-4823-aae7-19c6a2d6a19f",
                "cardNumber": "f3907186-e7e2-466f-91e5-48e12c2bcbc1",
                "cvv": "1989cb56-63da-4482-a2df-1f74cd0dd1a5",
                "name": "245d3a0f-a2d3-443b-8a20-8c17de86e186",
            },
            "request_index": 1,
        }
    ],
    "errors": [
        {
            "error": {
                "code":400,
                "description":"Invalid field present in JSON namee - requestId: 87fb2e32-6287-4e61-8304-9268df12bfe8",
                "request_index": 0,
            }
        }
    ]
}
```

## Detokenize

To retrieve record data using tokens, use the **detokenize(JSONObject records)** method. TheJSONObject must have a `records` key that takes an JSON array of record objects to fetch:

```java
JSONObject recordsJson = new JSONObject();

JSONObject recordJson = new JSONObject();
recordJson.put("token", "<token>");
recordJson.put("redaction", <Skyflow.RedactionType>); // Optional. Redaction to apply for retrieved data. E.g. RedactionType.DEFAULT.toString()

JSONArray recordsArrayJson = new JSONArray();
recordsArrayJson.put(recordJson);

recordsJson.put("records", recordsArrayJson);
```

Note: `redaction` defaults to [`RedactionType.PLAIN_TEXT`](#redaction-types).

The following [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/DetokenizeExample.java) code makes a detokenize call to reveal the masked value of a token:

```java
JSONObject recordsJson = new JSONObject();

JSONObject validRecordJson = new JSONObject();
validRecordJson.put("token", "45012507-f72b-4f5c-9bf9-86b133bae719");
validRecordJson.put("redaction", RedactionType.MASKED.toString());

JSONObject invalidRecordJson = new JSONObject();
invalidRecordJson.put("token","invalid-token");

JSONArray recordsArrayJson = new JSONArray();
recordsArrayJson.put(validRecordJson);
recordsArrayJson.put(invalidRecordJson);

recordsJson.put("records", recordsArrayJson);
try {
    JSONObject detokenizeResponse = skyflowClient.detokenize(recordsJson);
    System.out.println(detokenizeResponse);
} catch (SkyflowExeception exception) {
    if (exception.getData() != null)
        System.out.println(exception.getData());
    else
        System.out.println(exception);
}
```
The sample response:
```js
{
   "records": [
      {
        "token": "45012507-f72b-4f5c-9bf9-86b133bae719",
        "value": "j***oe"
      }
    ],
  "errors": [
      {
         "token": "invalid-token",
         "error": {
           "code": 404,
           "description": "Tokens not found for invalid-token"
         }
     }
   ]
}
```

## Get
In order to retrieve data from your vault using Skyflow IDs or by Unique Column Values, use the **get(JSONObject records, GetOptions options)** method. The `records` parameter takes a JSONObject that should contain
1. Either an array of Skyflow IDs to fetch
2. Or a column name and array of column values

The second parameter, options, is a GetOptions object that retrieves tokens of Skyflow IDs.

Note:
- GetOptions parameter applicable only for retrieving tokens using Skyflow ID.
- You can't pass GetOptions along with the redaction type.
- `tokens` defaults to false.


### Use Skyflow IDs

1. Retrieve data using Redaction type:

```java
import com.skyflow.entities.RedactionType;

JSONObject records = new JSONObject();
JSONArray recordsArray = new JSONArray();
JSONObject record = new JSONObject();
JSONArray ids = new JSONArray();
ids.add("<your_skyflowId>");

record.put("ids", ids);
record.put("table", "<your_table_name>");
record.put("redaction", RedactionType);

recordsArray.add(record);
records.put("records", recordsArray);
try {
    JSONObject getResponse = skyflowClient.get(records);
    System.out.println(getResponse);
} catch(SkyflowException exception) {
    if (exception.getData() != null) {
        System.out.println(exception.getData());
        } else {
        System.out.println(exception);
        }
}
```
2. Retrieve tokens using GetOptions:
```java
import com.skyflow.entities.*;

JSONObject records = new JSONObject();
JSONArray recordsArray = new JSONArray();

JSONObject record = new JSONObject();
JSONArray ids = new JSONArray();
ids.add("<your_skyflowId>");

record.put("ids", ids);
record.put("table", "<your_table_name>");
record.put("redaction", RedactionType);
recordsArray.add(record);
records.put("records", recordsArray);

try { 
    GetOptions options = new GetOptions(true);
    JSONObject getResponse = skyflowClient.get(records, options);
    System.out.println(getResponse);
} catch(SkyflowException exception) {
    if (exception.getData() != null) {
        System.out.println(exception.getData());
    } else {
        System.out.println(exception);
    }    
}
```

### Use column name and values

```java
import com.skyflow.entities.RedactionType;

JSONObject records = new JSONObject();
JSONArray recordsArray = new JSONArray();

JSONObject record = new JSONObject();
JSONArray values = new JSONArray();
values.add("<your_column_value>");

record.put("table", "<your_table_name>");
record.put("column_name", "<your_column_name>");
record.put("column_values", "<your_column_values>");
record.put("redaction", RedactionType);
recordsArray.add(record);
records.put("records", recordsArray);
```

### Redaction types
There are four accepted values for RedactionType:
* `PLAIN_TEXT`
* `MASKED`
* `REDACTED`
* `DEFAULT`

### Examples
An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/GetExample.java) call using Skyflow IDs with RedactionType.

```java
import com.skyflow.entities.RedactionType;

JSONObject recordsJson = new JSONObject();
JSONArray recordsArrayJson = new JSONArray();

JSONObject validRecord = new JSONObject();
JSONArray idsJson = new JSONArray();
idsJson.add("f8d8a622-b557-4c6b-a12c-c5ebe0b0bfd9");
idsJson.add("da26de53-95d5-4bdb-99db-8d8c66a35ff9");
validRecord.put("ids", idsJson);
validRecord.put("table", "cards");
validRecord.put("redaction", Redaction.PLAIN_TEXT.toString());

JSONObject invalidRecord = new JSONObject();
JSONArray invalidIdsJson = new JSONArray();
invalidIdsJson.add("Invalid Skyflow ID");

invalidRecord.put("ids", invalidIdsJson);
invalidRecord.put("table", "cards");
invalidRecord.put("redaction", Redaction.PLAIN_TEXT.toString());
recordsArrayJson.add(validRecord);
recordsArrayJson.add(invalidRecord);
recordsJson.put("records", recordsArray);

try {
    JSONObject getResponse = skyflowClient.get(recordsJson);
    System.out.println(getResponse);
} catch(SkyflowException exception) {
    if (exception.getData() != null) {
        System.out.println(exception.getData());
    } else {
        System.out.println(exception);
    }
}
```

Sample response:

```json
{
  "records": [
    {
      "fields": {
        "card_number": "4111111111111111",
        "cvv": "127",
        "expiry_date": "11/35",
        "fullname": "myname",
        "id": "f8d8a622-b557-4c6b-a12c-c5ebe0b0bfd9"
      },
      "table": "cards"
    },
    {
      "fields": {
        "card_number": "4111111111111111",
        "cvv": "317",
        "expiry_date": "10/23",
        "fullname": "sam",
        "id": "da26de53-95d5-4bdb-99db-8d8c66a35ff9"
      },
      "table": "cards"
    }
  ],
  "errors": [
    {
      "error": {
        "code": "404",
        "description": "No Records Found - requestId: fc531b8d-412e-9775-b945-4feacc9b8616"
      },
      "ids": ["Invalid Skyflow ID"]
    }
  ]
}
```

An example call using Skyflow IDs with GetOptions:

```java
import com.skyflow.entities.*;

JSONObject recordsJson = new JSONObject();
JSONArray recordsArrayJson = new JSONArray();

JSONObject validRecord = new JSONObject();
JSONArray idsJson = new JSONArray();
idsJson.add("f8d8a622-b557-4c6b-a12c-c5ebe0b0bfd9");
idsJson.add("da26de53-95d5-4bdb-99db-8d8c66a35ff9");
validRecord.put("ids", idsJson);
validRecord.put("table", "cards");

JSONObject invalidRecord = new JSONObject();
JSONArray invalidIdsJson = new JSONArray();
invalidIdsJson.add("Invalid Skyflow ID");

invalidRecord.put("ids", invalidIdsJson);
invalidRecord.put("table", "cards");
recordsArrayJson.add(validRecord);
recordsArrayJson.add(invalidRecord);
recordsJson.put("records", recordsArray);
GetOptions options = new GetOptions(true);
try { 
    JSONObject getResponse = skyflowClient.get(recordsJson, options);
    System.out.println(getResponse);
} catch(SkyflowException exception) {
    if (exception.getData() != null) {
        System.out.println(exception.getData());
    } else {
        System.out.println(exception);
    }    
}
```
Sample response:
```json
{
  "records": [
    {
      "fields": {
        "card_number": "4555-5176-5936-1930",
        "expiry_date": "23396425-93c9-419b-834b-7750b76a34b0",
        "fullname": "d6bb7fe5-6b77-4842-b898-221c51c3cc20",
        "id": "f8d8a622-b557-4c6b-a12c-c5ebe0b0bfd9"
      },
      "table": "cards"
    },
    {
      "fields": {
        "card_number": "8882-7418-2776-6660",
        "expiry_date": "284fb1f6-3c29-449f-8899-83a7839821bc",
        "fullname": "45a69af3-e22a-4668-9016-08bb2ef2259d",
        "id": "da26de53-95d5-4bdb-99db-8d8c66a35ff9"
      },
      "table": "cards"
    }
  ],
  "errors": [
    {
      "error": {
        "code": "404",
        "description": "No Records Found - requestId: fc531b8d-412e-9775-b945-4feacc9b8616"
      },
      "ids": ["Invalid Skyflow ID"]
    }
  ]
}
```
An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/GetExample.java) call using column names and values.

```java
import com.skyflow.entities.RedactionType;

JSONObject recordsJson = new JSONObject();
JSONArray recordsArrayJson = new JSONArray();

JSONObject validRecord = new JSONObject();
JSONArray valuesJson = new JSONArray();
valuesJson.add("123455432112345");
valuesJson.add("123455432112346");

validRecord.put("table", "account_details");
validRecord.put("column_name", "bank_account_number");
validRecord.put("column_values", valuesJson);
validRecord.put("redaction", Redaction.PLAIN_TEXT.toString());

JSONObject invalidRecord = new JSONObject();
JSONArray invalidValuesJson = new JSONArray();
invalidValuesJson.add("Invalid Skyflow column value");

invalidRecord.put("table", "account_details");
invalidRecord.put("column_name", "bank_account_number");
invalidRecord.put("column_values", valuesJson);
invalidRecord.put("redaction", Redaction.PLAIN_TEXT.toString());

recordsArrayJson.add(validRecord);
recordsArrayJson.add(invalidRecord);
recordsJson.put("records", recordsArray);

try {
    JSONObject getResponse = skyflowClient.get(recordsJson);
    System.out.println(getResponse);
} catch(SkyflowException exception) {
    if (exception.getData() != null) {
        System.out.println(exception.getData());
    } else {
        System.out.println(exception);
    }
}
```

Sample response:

```json
{
  "records": [
    {
      "fields": {
        "bank_account_number": "123455432112345",
        "pin_code": "123123",
        "name": "john doe",
        "id": "492c21a1-107f-4d10-ba2c-3482a411827d"
      },
      "table": "account_details"
    },
    {
      "fields": {
        "bank_account_number": "123455432112346",
        "pin_code": "103113",
        "name": "jane doe",
        "id": "ac6c6221-bcd1-4265-8fc7-ae7a8fb6dfd5"
      },
      "table": "account_details"
    }
  ],
  "errors": [
    {
      "columnName": ["bank_account_number"],
      "error": {
        "code": 404,
        "description": "No Records Found - requestId: fc531b8d-412e-9775-b945-4feacc9b8616"
      }
    }
  ]
}
```

`Note:`
While using detokenize and get methods, there is a possibility that some or all of the tokens might be invalid. In such cases, the data from the response consists of both errors and detokenized records. In the SDK, this will raise a SkyflowException and you can retrieve the data from the Exception object, as shown above.

## GetById

In order to retrieve data from your vault using SkyflowIDs, use the **getById(JSONObject records)** method. The `records` parameter takes a JSONObject that should contain an array of SkyflowIDs to be fetched, as shown below:
```java
import com.skyflow.entities.RedactionType;

JSONObject records = new JSONObject();
JSONArray recordsArray = new JSONArray();

JSONObject record = new JSONObject();
JSONArray ids = new JSONArray();
ids.add("<your_skyflowId>");

record.put("ids", ids);
record.put("table", "<you_table_name>");
record.put("redaction", RedactionType);
recordsArray.add(record);
records.put("records", recordsArray);
```
There are 4 accepted values in RedactionType:
- `PLAIN_TEXT`
- `MASKED`
- `REDACTED`
- `DEFAULT` 

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/GetByIdExample.java) getById call 
```java
import com.skyflow.entities.RedactionType;

JSONObject recordsJson = new JSONObject();
JSONArray recordsArrayJson = new JSONArray();

JSONObject validRecord = new JSONObject();
JSONArray idsJson = new JSONArray();
idsJson.add("f8d8a622-b557-4c6b-a12c-c5ebe0b0bfd9");
idsJson.add("da26de53-95d5-4bdb-99db-8d8c66a35ff9");
validRecord.put("ids",idsJson);
validRecord.put("table","cards");
validRecord.put("redaction",Redaction.PLAIN_TEXT.toString());

JSONObject invalidRecord = new JSONObject();
JSONArray invalidIdsJson = new JSONArray();
invalidIdsJson.add("invalid skyflow ID");

invalidRecord.put("ids",invalidIdsJson);
invalidRecord.put("table","cards");
invalidRecord.put("redaction",Redaction.PLAIN_TEXT.toString());
recordsArrayJson.add(validRecord);
recordsArrayJson.add(invalidRecord);
recordsJson.put("records", recordsArrayJson);

try{
    JSONObject getByIdResponse = skyflowClient.getById(recordsJson);
    System.out.println(getByIdResponse);
}catch(SkyflowException exception){
    if(exception.getData() != null)
        System.out.println(exception.getData());
    else
        System.out.println(exception);
}
```
Sample getById response
```js
{
  "records": [
      {
          "fields": {
              "card_number": "4111111111111111",
              "cvv": "127",
              "expiry_date": "11/35",
              "fullname": "myname",
              "id": "f8d8a622-b557-4c6b-a12c-c5ebe0b0bfd9"
          },
          "table": "cards"
      },
      {
          "fields": {
              "card_number": "4111111111111111",
              "cvv": "317",
              "expiry_date": "10/23",
              "fullname": "sam",
              "id": "da26de53-95d5-4bdb-99db-8d8c66a35ff9"
          },
          "table": "cards"
      }
  ],
  "errors": [
      {
          "error": {
              "code": "404",
              "description": "No Records Found"
          },
          "ids": ["invalid skyflow id"]
      }
  ]
}
```
`Note:` While using detokenize and getByID methods, there is a possibility that some or all of the tokens might be invalid. In such cases, the data from response consists of both errors and detokenized records. In the SDK, this will raise a SkyflowException and you can retrieve the data from this Exception object as shown above.

## Update

In order to update the records in your vault by **skyflow_id**, use the **update(records, options)** method. The first parameter, `records`, is a JSONObject that must have a records key and takes an array of records to update as a value in the vault. The options parameter takes an object of update options and includes an option to return tokens for the updated fields as shown below:
```java
JSONObject records = new JSONObject();
JSONArray recordsArray = new JSONArray();

JSONObject record = new JSONObject();
record.put("table", "<your_table_name>");
record.put("id","<your_skyflow_id>");

JSONObject fields = new JSONObject();
fields.put("<field_name>", "<field_value>");
record.put("fields", fields);
recordsArray.add(record);
records.put("records", recordsArray);
UpdateOptions updateOptions = new UpdateOptions(true);
```

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/UpdateExample.java) of update call:
```java
JSONObject records = new JSONObject();
JSONArray recordsArray = new JSONArray();

JSONObject record = new JSONObject();
record.put("table", "cards");
record.put("id","29ebda8d-5272-4063-af58-15cc674e332b");

JSONObject fields = new JSONObject();
fields.put("card_number", "5105105105105100");
fields.put("cardholder_name", "Thomas");
fields.put("expiration_date", "07/2032");
record.put("fields", fields);
recordsArray.add(record);
records.put("records", recordsArray);
UpdateOptions updateOptions = new UpdateOptions(true);

try {
   JSONObject response = skyflowClient.update(records, updateOptions);
}
catch (SkyflowException e) {
   e.printStackTrace();
}
```
Response:
```java
{
   "records": [
       {
           "id": "29ebda8d-5272-4063-af58-15cc674e332b",
           "fields": {
               "card_number": "93f28226-51b0-4f24-8151-78b5a61f028b",
               "cardholder_name": "0838fd08-9b51-4db2-893c-48542f3b121e",
               "expiration_date": "91d7ee77-262f-4d5d-8286-062b694c81fd",
           },
           "table": "cards"
       }
   ]
}
```

## Delete
To delete data from the vault, use the `delete(records, options?)` method of the Skyflow client. The `records` parameter takes an array of records to delete in the following format. The `options` parameter is optional and takes an object of deletion parameters. Currently, there are no supported deletion parameters.

Call schema:

```java
JSONObject records = new JSONObject();
JSONArray recordsArray = new JSONArray();

JSONObject record = new JSONObject();

record.put("id", "<SKYFLOW_ID_1>");
record.put("table", "<TABLE_NAME>");
recordsArray.add(record);
records.put("records", recordsArray);

skyflowClient.delete(records);
```

An example of delete call:
```java
JSONObject records = new JSONObject();
JSONArray recordsArray = new JSONArray();

JSONObject record = new JSONObject();
record.put("id", "71be4592-b9af-4dec-8669-5b9c926afb4c");
record.put("table", "cards");
recordsArray.add(record);
        
JSONObject record2 = new JSONObject();
record2.put("id", "2adf32e7-9a04-408e-b8bb-5b0a852422e0");
record2.put("table", "cards");
recordsArray.add(record2);

records.put("records", recordsArray);

try {
     JSONObject response = skyflowClient.delete(records);
} catch (SkyflowException e) {
     e.printStackTrace();
     System.out.println("error"+ e.getData());
}
```
Response:
```json
{
  "records": [
    {
     "skyflow_id": "71be4592-b9af-4dec-8669-5b9c926afb4c",
     "deleted": true,
    },
    {
     "skyflow_id": "2adf32e7-9a04-408e-b8bb-5b0a852422e0",
     "deleted": true,
    }
  ]
}
```

## Invoke Connection

Using the InvokeConnection method, you can integrate their server-side application with third party APIs and services without directly handling sensitive data. Prior to invoking the `InvokeConnection` method, you must have created a connection and have a connectionURL already generated. Once you have the connectionURL, you can invoke a connection by using the **invokeConnection(JSONObject config)** method. The JSONObject config parameter must include a `connectionURL` and `methodName`. The other fields are optional. 
```java
JSONObject invokeConfig = new JSONObject();
// connection url received when creating a skyflow connection integration
invokeConfig.put("connectionURL", "<your_connection_url>"); 
invokeConfig.put("methodName", RequestMethod);

JSONObject pathParamsJson = new JSONObject();
pathParamsJson.put("<path_param_key>", "<path_param_value>");
invokeConfig.put("pathParams", pathParamsJson);

JSONObject queryParamsJson = new JSONObject();
queryParamsJson.put("<query_param_key>", "<query_param_value>");
invokeConfig.put("queryParams", queryParamsJson);

JSONObject requestHeadersJson = new JSONObject();
requestHeadersJson.put("<request_header_key>", "<request_header_value>");
invokeConfig.put("requestHeader", requestHeadersJson);

JSONObject requestBodyJson = new JSONObject();
requestBodyJson.put("<request_body_key>", "<request_body_value>");
invokeConfig.put("requestBody", requestBodyJson);
```

`methodName` supports the following methods:
- GET
- POST
- PUT
- PATCH
- DELETE


**pathParams, queryParams, requestHeader, requestBody** are the JSON objects that will be sent through the connection integration url.

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/InvokeConnectionExample.java) of invokeConnection:
```java
JSONObject invokeConfig = new JSONObject();
invokeConfig.put("connectionURL", "<your_connection_url>");
invokeConfig.put("methodName", RequestMethod.POST);

JSONObject requestHeaderJson = new JSONObject();
requestHeaderJson.put("Content-Type","application/json");
requestHeaderJson.put("Authorization","<YOUR_CONNECTION_AUTH>");

invokeConfig.put("requestHeader",requestHeaderJson);

JSONObject requestBodyJson = new JSONObject();
requestBodyJson.put("expirationDate","12/2026");
invokeConfig.put("requestBody",requestBodyJson);

JSONObject pathParamsJson = new JSONObject();
pathParamsJson.put("card_number","1852-344-234-34251");
invokeConfig.put("pathParams",pathParamsJson);

try{
    JSONObject invokeConnectionResponse = skyflow.invokeConnection(invokeConfig);
    System.out.println(invokeResponse)    
}catch(SkyflowException exception){
    System.out.println(exception);
}

```
Sample invokeConnection Response
```js
{
    "receivedTimestamp": "2021-11-05 13:43:12.534",
    "processingTimeinMs": 12,
    "resource": {
        "cvv2": "558"
    }
}
```

### Query

To retrieve data with SQL queries, use the `query(queryInput, options)` method. `queryInput` is an object that takes the `query` parameter as follows:

```java
JSONObject queryInput = new JSONObject();
queryInput.put("query", "<YOUR_SQL_QUERY>");
skyflowClient.query(queryInput);
```
See [Query your data](https://docs.skyflow.com/query-data/) and [Execute Query](https://docs.skyflow.com/record/#QueryService_ExecuteQuery) for guidelines and restrictions on supported SQL statements, operators, and keywords.

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/QueryExample.java) of query call:
```java
JSONObject queryInput = new JSONObject();
queryInput.put("query", "SELECT * FROM cards WHERE skyflow_id='3ea3861-x107-40w8-la98-106sp08ea83f'");

try {
     JSONObject res = skyflowClient.query(queryInput);
} catch (SkyflowException e) {
     System.out.println(e.getData());
     e.printStackTrace();
}
```

Sample Response
```java
{
  "records": [
    {
      "fields": {
        "card_number": "XXXXXXXXXXXX1111",
        "card_pin": "*REDACTED*",
        "cvv": "",
        "expiration_date": "*REDACTED*",
        "expiration_month": "*REDACTED*",
        "expiration_year": "*REDACTED*",
        "name": "a***te",
        "skyflow_id": "3ea3861-x107-40w8-la98-106sp08ea83f",
        "ssn": "XXX-XX-6789",
        "zip_code": null
      },
      "tokens": null
    }
  ]
}
```
## Logging

The skyflow-java SDK provides useful logging using java inbuilt `java.util.logging`. By default the logging level of the SDK is set to `LogLevel.ERROR`. This can be changed by using `setLogLevel(LogLevel)` as shown below:

```java
import com.skyflow.entities.LogLevel;
import com.skyflow.Configuration; 

// sets the skyflow-java SDK log level to INFO
Configuration.setLogLevel(LogLevel.INFO);
```

Current the following 5 log levels are supported:

- `DEBUG`:

   When `LogLevel.DEBUG` is passed, all level of logs will be printed(DEBUG, INFO, WARN, ERROR)
   
- `INFO`: 

   When `LogLevel.INFO` is passed, INFO logs for every event that has occurred during the SDK flow execution will be printed along with WARN and ERROR logs
   
- `WARN`: 

   When `LogLevel.WARN` is passed, WARN and ERROR logs will be printed
   
- `ERROR`:

   When `LogLevel.ERROR` is passed, only ERROR logs will be printed.
   
- `OFF`: 

   `LogLevel.OFF` can be used to turn off all logging from the skyflow-java SDK.
   

`Note`:
  - The ranking of logging levels is as follows :  `DEBUG` < `INFO` < `WARN` < `ERROR`.

## Reporting a Vulnerability

If you discover a potential security issue in this project, please reach out to us at security@skyflow.com. Please do not create public GitHub issues or Pull Requests, as malicious actors could potentially view them.
