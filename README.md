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
    - [Migrate from v1 to v2](#migrate-from-v1-to-v2)
    - [Vault APIs](#vault-apis)
        - [Insert](#insert-data-into-the-vault)
        - [Detokenize](#detokenize)
        - [Get](#get)
            - [Use Skyflow IDs](#get-by-skyflow-ids)
            - [Use column name and values](#get-by-column-name-and-column-values)
            - [Redaction types](#redaction-types)
        - [Update](#update)
        - [Delete](#delete)
        - [Query](#query)
    - [Connections](#connections)
        - [Invoke Connection](#invoke-connection)
    - [Logging](#logging)
    - [Reporting a Vulnerability](#reporting-a-vulnerability)

## Features

- Authentication with a Skyflow Service Account and generation of a bearer token
- Vault API operations to insert, retrieve and tokenize sensitive data
- Invoking connections to call downstream third party APIs without directly handling sensitive data

## Installation

### Requirements

- Java 1.8 and above

### Configuration
---

#### Gradle users

Add this dependency to your project's build file:

```
implementation 'com.skyflow:skyflow-java:2.0.0'
```

#### Maven users

Add this dependency to your project's POM:

```xml

<dependency>
    <groupId>com.skyflow</groupId>
    <artifactId>skyflow-java</artifactId>
    <version>2.0.0</version>
</dependency>
```

---

## Service Account Bearer Token Generation

The [Service Account](https://github.com/skyflowapi/skyflow-java/tree/main/src/main/java/com/skyflow/serviceaccount/util)
java module is used to generate service account tokens from service account credentials file which is downloaded upon
creation of service account. The token generated from this module is valid for 60 minutes and can be used to make API
calls to vault services as well as management API(s) based on the permissions of the service account.

The `BearerToken` utility class allows to generate bearer token with the help of credentials json file. Alternatively,
you can also send the entire credentials as a string.

[Example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/serviceaccount/BearerTokenGenerationExample.java
):

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;
import com.skyflow.serviceaccount.util.Token;

import java.io.File;

public class BearerTokenGenerationExample {
    public static void main(String[] args) {
        String token = null;
        // Generate BearerToken by specifying credentials.json file path
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            if (Token.isExpired(token)) {
                BearerToken bearerToken = BearerToken.builder().setCredentials(new File(filePath)).build();
                token = bearerToken.getBearerToken();
            }
            System.out.println(token);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

        // Generate BearerToken by specifying credentials.json as string
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            if (Token.isExpired(token)) {
                BearerToken bearerToken = BearerToken.builder().setCredentials(fileContents).build();
                token = bearerToken.getBearerToken();
            }
            System.out.println(token);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
    }
}
```

## Service Account Bearer Token with Context Generation

Context-Aware Authorization enables you to embed context values into a Bearer token when you generate it, and reference
those values in your policies for more dynamic access control of data in the vault or validating signed data tokens
during detokenization. It can be used to track end user identity when making API calls using service accounts.

The service account generated with `context_id` identifier enabled can be used to generate bearer tokens with `context`,
which is a `jwt` claim for a skyflow generated bearer token. The token generated from this service account will have a
`context_identifier` claim and is valid for 60 minutes and can be used to make API calls to vault services as well as
management API(s) based on the permissions of the service account.

[Example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/serviceaccount/BearerTokenGenerationWithContextExample.java):

``` java
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;

import java.io.File;

public class BearerTokenGenerationWithContextExample {
    public static void main(String[] args) {
        String bearerToken = null;
        // Generate BearerToken with context by specifying credentials.json file path
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            BearerToken token = BearerToken.builder()
                    .setCredentials(new File(filePath))
                    .setCtx("abc")
                    .build();

            bearerToken = token.getBearerToken();
            System.out.println(bearerToken);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

        // Generate BearerToken with context by specifying credentials.json as string
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            BearerToken token = BearerToken.builder()
                    .setCredentials(fileContents)
                    .setCtx("abc")
                    .build();

            bearerToken = token.getBearerToken();
            System.out.println(bearerToken);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
    }
}
```

## Service Account Scoped Bearer Token Generation

A service account that has multiple roles can generate bearer tokens with access restricted to a specific role by
providing the appropriate `roleID`. Generated bearer tokens are valid for 60 minutes and can only perform operations
with the permissions associated with the specified role.

[Example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/serviceaccount/ScopedTokenGenerationExample.java):

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;

import java.io.File;
import java.util.ArrayList;

public class ScopedTokenGenerationExample {
    public static void main(String[] args) {
        String scopedToken = null;
        // Generate Scoped Token  by specifying credentials.json file path
        try {
            ArrayList<String> roles = new ArrayList<>();
            roles.add("ROLE_ID");
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            BearerToken bearerToken = BearerToken.builder()
                    .setCredentials(new File(filePath))
                    .setRoles(roles)
                    .build();

            scopedToken = bearerToken.getBearerToken();
            System.out.println(scopedToken);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
    }
}
```

Notes:

- You can pass either a service account key credentials file path or a service account key credentials as string to the
  `setCredentials` method of the BearerTokenBuilder class.
- If you pass both a file path and string to the `setCredentials` method, the last method used takes precedence.
- To generate multiple bearer tokens using a thread, see
  this [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/serviceaccount/BearerTokenGenerationUsingThreadsExample.java)

## Signed Data Tokens Generation

Skyflow generates data tokens when sensitive data is inserted into the vault. These data tokens can be digitally signed
with the private key of the service account credentials, which adds an additional layer of protection. Signed tokens can
be detokenized by passing the signed data token and a bearer token generated from service account credentials. The
service account must have appropriate permissions and context to detokenize the signed data tokens.

[Example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/serviceaccount/SignedTokenGenerationExample.java):

``` java
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.SignedDataTokenResponse;
import com.skyflow.serviceaccount.util.SignedDataTokens;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SignedTokenGenerationExample {
    public static void main(String[] args) {
        List<SignedDataTokenResponse> signedTokenValues;
        // Generate Signed data token with context by specifying credentials.json file path
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            String context = "abc";
            ArrayList<String> dataTokens = new ArrayList<>();
            dataTokens.add("YOUR_DATA_TOKEN_1");
            SignedDataTokens signedToken = SignedDataTokens.builder()
                    .setCredentials(new File(filePath))
                    .setCtx(context)
                    .setTimeToLive(30) // in seconds
                    .setDataTokens(dataTokens)
                    .build();
            signedTokenValues = signedToken.getSignedDataTokens();
            System.out.println(signedTokenValues);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

        // Generate Signed data token with context by specifying credentials.json as string
        try {
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            String context = "abc";
            ArrayList<String> dataTokens = new ArrayList<>();
            dataTokens.add("YOUR_DATA_TOKEN_1");
            SignedDataTokens signedToken = SignedDataTokens.builder()
                    .setCredentials(fileContents)
                    .setCtx(context)
                    .setTimeToLive(30) // in seconds
                    .setDataTokens(dataTokens)
                    .build();
            signedTokenValues = signedToken.getSignedDataTokens();
            System.out.println(signedTokenValues);
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
        "dataToken":"5530-4316-0674-5748",
        "signedDataToken":"signed_token_eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJzLCpZjA"
    }
]
```

Notes:

- You can pass either a service account key credentials file path or a service account key credentials as string to the
  `setCredentials` method of the SignedDataTokensBuilder class.
- If you pass both a file path and string to the `setCredentials` method, the last method used takes precedence.
- Time to live value expects time as seconds.
- The default time to live value is 60 seconds.

## Migrate from v1 to v2

Below are the steps to migrate the java sdk from v1 to v2.

### 1. Authentication Options
In V2, we have introduced multiple authentication options. 
You can now provide credentials in the following ways: 

- **API Key (Recommended)**
- **Passing credentials as ENV.  (`SKYFLOW_CREDENTIALS`) (Recommended)**
- **Path to your credentials JSON file**
- **Stringified JSON of your credentials**
- **Bearer token**

These options allow you to choose the authentication method that best suits your use case.

### V1 (Old)
```java
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

### V2 (New): Passing one of the following:
```java
// Option 1: API Key (Recommended)
Credentials skyflowCredentials = new Credentials();
skyflowCredentials.setApiKey("<YOUR_API_KEY>"); // Replace <API_KEY> with your actual API key

// Option 2: Environment Variables (Recommended)
// Set SKYFLOW_CREDENTIALS in your environment

// Option 3: Credentials File
skyflowCredentials.setPath("<YOUR_CREDENTIALS_FILE_PATH>"); // Replace with the path to credentials file

// Option 4: Stringified JSON
skyflowCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>"); // Replace with the credentials string

// Option 5: Bearer Token
skyflowCredentials.setToken("<BEARER_TOKEN>"); // Replace <BEARER_TOKEN> with your actual authentication token.
```

**Notes:**
1. Use only ONE authentication method.
2. Environment variables take precedence over programmatic configuration.
3. API Key or Environment Variables are recommended for production use.
4. Secure storage of credentials is essential.
5. For overriding behavior and priority order of credentials, refer to the README.

---

### 2. Client Initialization
In V2, we have introduced a Builder design pattern for client initialization and added support for multi-vault. This allows you to configure multiple vaults during client initialization. 

In V2, the log level is tied to each individual client instance.

During client initialization, you can pass the following parameters: 
- `vaultId` and clusterId: These values are derived from the vault ID & vault URL. 
- `env`: Specify the environment (e.g., SANDBOX or PROD). 
- `credentials`: The necessary authentication credentials.

### V1 (Old)
```java
// DemoTokenProvider class is an implementation of the TokenProvider interface
DemoTokenProvider demoTokenProvider = new DemoTokenProvider();
SkyflowConfiguration skyflowConfig = new SkyflowConfiguration("<VAULT_ID>","<VAULT_URL>", demoTokenProvider);
Skyflow skyflowClient = Skyflow.init(skyflowConfig);
```

### V2 (New)
```java
Credentials credentials = new Credentials();
credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH_1>"); // Replace with the path to the credentials file

// Configure the first vault (Blitz)
VaultConfig config = new VaultConfig();
config.setVaultId("<YOUR_VAULT_ID_1>"); // Replace with the ID of the first vault
config.setClusterId("<YOUR_CLUSTER_ID_1>"); // Replace with the cluster ID of the first vault
config.setEnv(Env.DEV); // Set the environment (e.g., DEV, STAGE, PROD)
config.setCredentials(credentials); // Associate the credentials with the vault

// Set up credentials for the Skyflow client
Credentials skyflowCredentials = new Credentials();
skyflowCredentials.setPath("<YOUR_CREDENTIALS_FILE_PATH_2>"); // Replace with the path to another credentials file

// Create a Skyflow client and add vault configurations
Skyflow skyflowClient = Skyflow.builder()
       .setLogLevel(LogLevel.DEBUG) // Enable debugging for detailed logs
       .addVaultConfig(config)      // Add the first vault configuration
       .addSkyflowCredentials(skyflowCredentials) // Add general Skyflow credentials
       .build();
```

**Key Changes:**
- `vaultUrl` replaced with `clusterId`.
- Added environment specification (`env`).
- Instance-specific log levels.
- TypeScript support with proper type definitions.

---

### 3. Request & Response Structure
In V2, we have removed the use of JSON objects from a third-party package. Instead, we have transitioned to accepting native ArrayList and HashMap data structures and adopted the Builder pattern for request creation. This request need 
- `table`: The name of the table. 
- `values`: An array of objects containing the data to be inserted. 
The response will be of type InsertResponse class, which contains insertedFields and errors.

### V1 (Old) Request Building
```java
JSONObject recordsJson = new JSONObject();
JSONArray recordsArrayJson = new JSONArray();

JSONObject recordJson = new JSONObject();
recordJson.put("table", "cards");

JSONObject fieldsJson = new JSONObject();
fields.put("cardNumber", "41111111111");
fields.put("cvv", "123");

recordJson.put("fields", fieldsJson);
recordsArrayJson.add(record);
recordsJson.put("records", recordsArrayJson);
try {
    JSONObject insertResponse = skyflowClient.insert(records);
    System.out.println(insertResponse);
} catch (SkyflowException exception) {
    System.out.println(exception);
}
```

### V2 (New) Request Building
```java
ArrayList<HashMap<String, Object>> values = new ArrayList<>();
HashMap<String, Object> value = new HashMap<>();
value.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>"); // Replace with column name and value
value.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>"); // Replace with another column name and value
values.add(values);

ArrayList<HashMap<String, Object>> tokens = new ArrayList<>();
HashMap<String, Object> token = new HashMap<>();
token.put("<COLUMN_NAME_2>", "<TOKEN_VALUE_2>"); // Replace with the token for COLUMN_NAME_2
tokens.add(token);

InsertRequest insertRequest = InsertRequest.builder()
       .table("<TABLE_NAME>") // Replace with the table name
       .continueOnError(true) // Continue inserting even if some records fail
       .tokenMode(TokenMode.ENABLE) // Enable BYOT for token validation
       .values(values)        // Data to insert
       .tokens(tokens)        // Provide tokens for BYOT columns
       .returnTokens(true)    // Return tokens along with the response
       .build();
```

### V1 (Old) Response Structure
```json
{
  "records": [
    {
      "table": "cards",
      "fields": {
        "skyflow_id": "16419435-aa63-4823-aae7-19c6a2d6a19f",
        "cardNumber": "f3907186-e7e2-466f-91e5-48e12c2bcbc1",
        "cvv": "1989cb56-63da-4482-a2df-1f74cd0dd1a5"
      }
    }
  ]
}
```

### V2 (New) Response Structure
```json
{
  "insertedFields": [
    {
      "card_number": "5484-7829-1702-9110",
      "request_index": "0",
      "skyflow_id": "9fac9201-7b8a-4446-93f8-5244e1213bd1",
      "cardholder_name": "b2308e2a-c1f5-469b-97b7-1f193159399b"
    }
  ],
  "errors": []
}
```

---

## 4. Request Options
In V2, with the introduction of the Builder design pattern has made handling optional fields in Java more efficient and straightforward.


### V1 (Old)
```java
InsertOptions insertOptions = new InsertOptions(true);
```

### V2 (New)
```java
InsertRequest upsertRequest = new InsertRequest.builder()
       .table("<TABLE_NAME>") // Replace with the table name
       .continueOnError(false) // Stop inserting if any record fails
       .tokenMode(TokenMode.DISABLE) // Disable BYOT
       .values(values) // Data to insert
       .returnTokens(false) // Do not return tokens
       .upsert("<UPSERT_COLUMN>") // Replace with the column name used for upsert logic
       .build();
```

---

## 5. Enhanced Error Details
The V2 error response includes:

- `http_status`: The HTTP status code.
- `grpc_code`: The gRPC code associated with the error.
- `details` & `message`: A detailed description of the error.
- `request_ID`: A unique request identifier for easier debugging.

### V1 (Old) Error Structure
```json
{
  code: "<http_code>",
  description: "<description>"
}
```

### V2 (New) Error Structure
```json
{
  http_status: "<http_status>",
  grpc_code: "<grpc_code>",
  http_code: "<http_code>",
  message: "<message>",
  request_ID: "<request_ID>",
  details: [ "<details>" ]
}
```

## Vault APIs

The [Vault](https://github.com/skyflowapi/skyflow-java/tree/main/src/main/java/com/skyflow/vault) module is used to
perform operations on the vault such as inserting records, detokenizing tokens and retrieving tokens for a skyflow_id.

To use this module, the skyflow client must first be initialized as follows.

```java
import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

public class InitSkyflowClient {
    public static void main(String[] args) throws SkyflowException {
        // Pass only one of apiKey, token, credentialsString or path in credentials        
        Credentials credentials = new Credentials();
        credentials.setToken("<BEARER_TOKEN>");

        VaultConfig config = new VaultConfig();
        config.setVaultId("<VAULT_ID>");      // Primary vault
        config.setClusterId("<CLUSTER_ID>");  // ID from your vault URL Eg https://{clusterId}.vault.skyflowapis.com
        config.setEnv(Env.PROD);              // Env by default is set to PROD
        config.setCredentials(credentials);   // Individual credentials

        JsonObject credentialsObject = new JsonObject();
        credentialsObject.addProperty("clientID", "<YOUR_CLIENT_ID>");
        credentialsObject.addProperty("clientName", "<YOUR_CLIENT_NAME>");
        credentialsObject.addProperty("TokenURI", "<YOUR_TOKEN_URI>");
        credentialsObject.addProperty("keyID", "<YOUR_KEY_ID>");
        credentialsObject.addProperty("privateKey", "<YOUR_PRIVATE_KEY>");

        // To generate Bearer Token from credentials string.
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setCredentialsString(credentialsObject.toString());

        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.INFO)                // Set log level. By default, it is set to ERROR
                .addVaultConfig(config)                    // Add vault config
                .addSkyflowCredentials(skyflowCredentials) // Skyflow credentials will be used if no individual credentials are passed
                .build();
    }
}
```

Notes:

- If both Skyflow common credentials and individual credentials at the configuration level are provided, the individual
  credentials at the configuration level will take priority.

All Vault APIs must be invoked using a client instance.

## Insert data into the vault

To insert data into your vault, use the `insert` method. The `InsertRequest` class is used to create an insert request,
which contains the values to be inserted in the form of a list of records. Additionally, you can provide options in the
insert request, such as returning tokenized data, upserting records, and continuing on error.

Insert call schema

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;

import java.util.ArrayList;
import java.util.HashMap;

public class InsertSchema {
    public static void main(String[] args) {
        try {
            // Initialize skyflow client   
            ArrayList<HashMap<String, Object>> insertData = new ArrayList<>();

            HashMap<String, Object> insertRecord1 = new HashMap<>();
            insertRecord1.put("<FIELD_NAME_1>", "<VALUE_1>");
            insertRecord1.put("<FIELD_NAME_2>", "<VALUE_2>");

            HashMap<String, Object> insertRecord2 = new HashMap<>();
            insertRecord2.put("<FIELD_NAME_1>", "<VALUE_1>");
            insertRecord2.put("<FIELD_NAME_2>", "<VALUE_2>");

            insertData.add(insertRecord1);
            insertData.add(insertRecord2);

            InsertRequest insertRequest = InsertRequest.builder().table("<TABLE_NAME>").values(insertData).build();
            InsertResponse insertResponse = skyflowClient.vault("<VAULT_ID>").insert();
            System.out.println("Insert Response: " + insertResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Insert
call [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/InsertExample.java)

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;

import java.util.ArrayList;
import java.util.HashMap;

public class InsertExample {
    public static void main(String[] args) {
        try {
            // Initialize skyflow client   
            ArrayList<HashMap<String, Object>> insertData = new ArrayList<>();
            HashMap<String, Object> insertRecord = new HashMap<>();
            insertRecord.put("card_number", "4111111111111111");
            insertRecord.put("cardholder_name", "john doe");
            insertData.add(insertRecord);

            InsertRequest insertRequest = InsertRequest.builder()
                    .table("table1")
                    .values(insertData)
                    .returnTokens(true)
                    .build();
            InsertResponse insertResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").insert();
            System.out.println("Insert Response: " + insertResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Skyflow returns tokens for the record you just inserted.

```js
Insert Response: {
	"insertedFields": [{
		"card_number": "5484-7829-1702-9110",
		"request_index": "0",
		"skyflow_id": "9fac9201-7b8a-4446-93f8-5244e1213bd1",
		"cardholder_name": "b2308e2a-c1f5-469b-97b7-1f193159399b",
	}],
	"errors": []
}
```

Insert call example with `continueOnError` option

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;

import java.util.ArrayList;
import java.util.HashMap;

public class InsertExample {
    public static void main(String[] args) {
        try {
            // Initialize skyflow client   
            ArrayList<HashMap<String, Object>> insertData = new ArrayList<>();

            HashMap<String, Object> insertRecord1 = new HashMap<>();
            insertRecord1.put("card_number", "4111111111111111");
            insertRecord1.put("cardholder_name", "john doe");

            HashMap<String, Object> insertRecord2 = new HashMap<>();
            insertRecord2.put("card_numbe", "4111111111111111");
            insertRecord2.put("cardholder_name", "jane doe");

            insertData.add(insertRecord1);
            insertData.add(insertRecord2);

            InsertRequest insertRequest = InsertRequest.builder()
                    .table("table1")
                    .values(insertData)
                    .returnTokens(true)
                    .continueOnError(true)
                    .build();
            InsertResponse insertResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").insert();
            System.out.println("Insert Response: " + insertResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Sample response:

```js
{
	"insertedFields": [{
		"card_number": "5484-7829-1702-9110",
		"request_index": "0",
		"skyflow_id": "9fac9201-7b8a-4446-93f8-5244e1213bd1",
		"cardholder_name": "b2308e2a-c1f5-469b-97b7-1f193159399b",
	}],
	"errors": [{
		"request_index": "1",
		"error": "Insert failed. Column card_numbe is invalid. Specify a valid column.",
	}]
}
```

Insert call example with `upsert` option

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;

import java.util.ArrayList;
import java.util.HashMap;

public class InsertExample {
    public static void main(String[] args) {
        try {
            // Initialize skyflow client   
            ArrayList<HashMap<String, Object>> upsertData = new ArrayList<>();
            HashMap<String, Object> upsertRecord = new HashMap<>();
            upsertRecord.put("cardholder_name", "jane doe");
            upsertData.add(upsertRecord);

            InsertRequest insertRequest = InsertRequest.builder()
                    .table("table1")
                    .values(upsertData)
                    .returnTokens(true)
                    .upsert("cardholder_name")
                    .build();
            InsertResponse insertResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").insert();
            System.out.println("Insert Response: " + insertResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Skyflow returns tokens, with `upsert` support, for the record you just inserted.

```js
{
	"insertedFields": [{
		"skyflowId": "9fac9201-7b8a-4446-93f8-5244e1213bd1",
		"cardholder_name": "73ce45ce-20fd-490e-9310-c1d4f603ee83" 
	}],
	"errors": []
}
```

## Detokenize

To retrieve tokens from your vault, you can use the `detokenize` method. The `DetokenizeRequest` class requires a list
of detokenization data to be provided as input. Additionally, the redaction type and continue on error are optional
parameters.

```java
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;

import java.util.ArrayList;

public class DetokenizeSchema {
    public static void main(String[] args) {
        try {
            // Initialize skyflow client
            ArrayList<String> tokens = new ArrayList<>();
            tokens.add("<YOUR_TOKEN_VALUE_1>");
            tokens.add("<YOUR_TOKEN_VALUE_2>");
            tokens.add("<YOUR_TOKEN_VALUE_3>");
            DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                    .tokens(tokens)
                    .continueOnError(true)
                    .redactionType(RedactionType.PLAIN_TEXT)
                    .build();
            DetokenizeResponse detokenizeResponse = skyflowClient.vault("<VAULT_ID>").detokenize(detokenizeRequest);
            System.out.println(detokenizeResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Notes:

- `redactionType` defaults to [`RedactionType.PLAIN_TEXT`](#redaction-types).
- `continueOnError` defaults to `true`.

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/DetokenizeExample.java)
of a detokenize call:

```java
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;

import java.util.ArrayList;

public class DetokenizeExample {
    public static void main(String[] args) {
        try {
            // Initialize skyflow client
            ArrayList<String> tokens = new ArrayList<>();
            tokens.add("9738-1683-0486-1480");
            tokens.add("6184-6357-8409-6668");
            tokens.add("4914-9088-2814-3840");
            DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                    .tokens(tokens)
                    .continueOnError(false)
                    .redactionType(RedactionType.PLAIN_TEXT)
                    .build();
            DetokenizeResponse detokenizeResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").detokenize(detokenizeRequest);
            System.out.println(detokenizeResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Sample response:

```js
{
	"detokenizedFields": [{
		"token": "9738-1683-0486-1480",
		"value": "4111111111111115",
		"type": "STRING",
	}, {
		"token": "6184-6357-8409-6668",
		"value": "4111111111111119",
		"type": "STRING",
	}, {
		"token": "4914-9088-2814-3840",
		"value": "4111111111111118",
		"type": "STRING",
	}]
	"errors": []
}

```

An example of a detokenize call with `continueOnError` option:

```java
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;

import java.util.ArrayList;

public class DetokenizeExample {
    public static void main(String[] args) {
        try {
            // Initialize skyflow client
            ArrayList<String> tokens = new ArrayList<>();
            tokens.add("9738-1683-0486-1480");
            tokens.add("6184-6357-8409-6668");
            tokens.add("4914-9088-2814-384");
            DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                    .tokens(tokens)
                    .continueOnError(true)
                    .redactionType(RedactionType.PLAIN_TEXT)
                    .build();
            DetokenizeResponse detokenizeResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").detokenize(detokenizeRequest);
            System.out.println(detokenizeResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Sample response:

```js
{
	"detokenizedFields": [{
		"token": "9738-1683-0486-1480",
		"value": "4111111111111115",
		"type": "STRING",
	}, {
		"token": "6184-6357-8409-6668",
		"value": "4111111111111119",
		"type": "STRING",
	}]
	"errors": [{
		"token": "4914-9088-2814-384",
		"error": "Token Not Found",
	}]
}
```

## Tokenize

To tokenize data, use the `tokenize` method. The `TokenizeRequest` class is utilized to create a tokenize request. In
this request, you specify the `values` parameter, which is a list of `ColumnValue`. Each `ColumnValue` contains two
properties: `value` and `columnGroup`.

Tokenize Schema

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.TokenizeRequest;
import com.skyflow.vault.tokens.TokenizeResponse;

import java.util.ArrayList;

public class TokenizeSchema {
    public static void main(String[] args) {
        try {
            // Initialize Skyflow client
            ArrayList<ColumnValue> columnValues = new ArrayList<>();

            ColumnValue columnValue1 = ColumnValue.builder().value("<VALUE>").columnGroup("<COLUMN_GROUP>").build();
            ColumnValue columnValue2 = ColumnValue.builder().value("<VALUE>").columnGroup("<COLUMN_GROUP>").build();

            columnValues.add(columnValue1);
            columnValues.add(columnValue2);

            TokenizeRequest tokenizeRequest = TokenizeRequest.builder().values(columnValues).build();
            TokenizeResponse tokenizeResponse = skyflowClient.vault("<VAULT_ID>").tokenize(tokenizeRequest);
            System.out.println(tokenizeResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/TokenizeExample.java)
of Tokenize call:

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.TokenizeRequest;
import com.skyflow.vault.tokens.TokenizeResponse;

import java.util.ArrayList;

public class TokenizeSchema {
    public static void main(String[] args) {
        try {
            // Initialize Skyflow client
            ArrayList<ColumnValue> columnValues = new ArrayList<>();
            ColumnValue columnValue = ColumnValue.builder().value("4111111111111111").columnGroup("card_number_cg").build();
            columnValues.add(columnValue);

            TokenizeRequest tokenizeRequest = TokenizeRequest.builder().values(columnValues).build();
            TokenizeResponse tokenizeResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").tokenize(tokenizeRequest);
            System.out.println(tokenizeResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Sample response:

```js
{
	"tokens": [5479-4229-4622-1393]
}
```

## Get

To retrieve data using Skyflow IDs or unique column values, use the `get` method. The `GetRequest` class is used to
create a get request, where you specify parameters such as the table name, redaction type, Skyflow IDs, column names,
column values, and return tokens. If Skyflow IDs are provided, column names and column values cannot be used. Similarly,
if column names or column values are provided, Skyflow IDs cannot be used.

Get Schema

```java
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;

import java.util.ArrayList;

public class GetSchema {
    public static void main(String[] args) {
        try {
            // Initialize Skyflow client
            ArrayList<String> ids = new ArrayList<>();
            ids.add("<SKYFLOW_ID_1>");
            ids.add("<SKYFLOW_ID_2>");
            GetRequest getByIdRequest = GetRequest.builder()
                    .ids(ids)
                    .table("<TABLE_NAME>")
                    .returnTokens(false)
                    .redactionType(RedactionType.PLAIN_TEXT)
                    .build();
            GetResponse getByIdResponse = skyflowClient.vault("<VAULT_ID>").get(getByIdRequest);
            System.out.println(getByIdResponse);

            GetRequest getTokensRequest = GetRequest.builder()
                    .ids(ids)
                    .table("<TABLE_NAME>")
                    .returnTokens(true)
                    .build();
            GetResponse getTokensResponse = skyflowClient.vault("<VAULT_ID>").get(getTokensRequest);
            System.out.println(getTokensResponse);

            ArrayList<String> columnValues = new ArrayList<>();
            columnValues.add("<COLUMN_VALUE_1>");
            columnValues.add("<COLUMN_VALUE_2>");
            GetRequest getByColumnRequest = GetRequest.builder()
                    .table("<TABLE_NAME>")
                    .columnName("<COLUMN_NAME>")
                    .columnValues(columnValues)
                    .redactionType(RedactionType.PLAIN_TEXT)
                    .build();
            GetResponse getByColumnResponse = skyflowClient.vault("<VAULT_ID>").get(getByColumnRequest);
            System.out.println(getByColumnResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

### Get by skyflow IDs

-

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/GetExample.java)
of a get call to retrieve data using Redaction type:

```java
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;

import java.util.ArrayList;

public class GetExample {
    public static void main(String[] args) {
        try {
            // Initialize Skyflow client
            ArrayList<String> ids = new ArrayList<>();
            ids.add("a581d205-1969-4350-acbe-a2a13eb871a6");
            ids.add("5ff887c3-b334-4294-9acc-70e78ae5164a");
            GetRequest getByIdRequest = GetRequest.builder()
                    .ids(ids)
                    .table("table1")
                    .returnTokens(false)
                    .redactionType(RedactionType.PLAIN_TEXT)
                    .build();
            GetResponse getByIdResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").get(getByIdRequest);
            System.out.println(getByIdResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Sample response:

```js
{
	"data": [{
		"card_number": "4555555555555553",
		"email": "john.doe@gmail.com",
		"name": "john doe",
		"skyflow_id": "a581d205-1969-4350-acbe-a2a13eb871a6",
	}, {
		"card_number": "4555555555555559",
		"email": "jane.doe@gmail.com",
		"name": "jane doe",
		"skyflow_id": "5ff887c3-b334-4294-9acc-70e78ae5164a",
	}],
	"errors": []
}
```

-

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/getExample.java)
of get call to retrieve tokens using Skyflow IDs:

```java
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;

import java.util.ArrayList;

public class GetExample {
    public static void main(String[] args) {
        try {
            // Initialize Skyflow client
            ArrayList<String> ids = new ArrayList<>();
            ids.add("a581d205-1969-4350-acbe-a2a13eb871a6");
            ids.add("5ff887c3-b334-4294-9acc-70e78ae5164a");
            GetRequest getTokensRequest = GetRequest.builder()
                    .ids(ids)
                    .table("table1")
                    .returnTokens(true)
                    .build();
            GetResponse getTokensResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").get(getTokensRequest);
            System.out.println(getTokensResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Sample response:

```js
{
	"data": [{
		"card_number": "3998-2139-0328-0697",
		"email": "c9a6c9555060@82c092e7.bd52",
		"name": "82c092e7-74c0-4e60-bd52-c9a6c9555060",
		"skyflow_id": "a581d205-1969-4350-acbe-a2a13eb871a6",
	}, {
		"card_number": "3562-0140-8820-7499",
		"email": "6174366e2bc6@59f82e89.93fc",
		"name": "59f82e89-138e-4f9b-93fc-6174366e2bc6",
		"skyflow_id": "5ff887c3-b334-4294-9acc-70e78ae5164a",
	}],
	"errors": []
}
```

### Get By column name and column values

-

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/GetExample.java)
of get call to retrieve data using column name and column values

```java
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;

import java.util.ArrayList;

public class GetExample {
    public static void main(String[] args) {
        try {
            // Initialize Skyflow client
            ArrayList<String> columnValues = new ArrayList<>();
            columnValues.add("john.doe@gmail.com");
            columnValues.add("jane.doe@gmail.com");
            GetRequest getByColumnRequest = GetRequest.builder()
                    .table("table1")
                    .columnName("email")
                    .columnValues(columnValues)
                    .redactionType(RedactionType.PLAIN_TEXT)
                    .build();
            GetResponse getByColumnResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").get(getByColumnRequest);
            System.out.println(getByColumnResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Sample response:

```js
{
	"data": [{
		"card_number": "4555555555555553",
		"email": "john.doe@gmail.com",
		"name": "john doe",
		"skyflow_id": "a581d205-1969-4350-acbe-a2a13eb871a6",
	}, {
		"card_number": "4555555555555559",
		"email": "jane.doe@gmail.com",
		"name": "jane doe",
		"skyflow_id": "5ff887c3-b334-4294-9acc-70e78ae5164a",
	}],
	"errors": []
}
```

### Redaction types

There are four accepted values for RedactionType:

* `PLAIN_TEXT`
* `MASKED`
* `REDACTED`
* `DEFAULT`

## Update

To update data in your vault, use the `update` method. The `UpdateRequest` class is used to create an update request,
where you specify parameters such as the table name, data (as a map of key value pairs), tokens, returnTokens, and
tokenStrict. If `returnTokens` is set to `true`, Skyflow returns tokens for the updated records. If `returnTokens` is
set to `false`, Skyflow returns IDs for the updated records.

Update Schema:

```java
import com.skyflow.enums.TokenMode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateResponse;

import java.util.HashMap;

public class UpdateSchema {
    public static void main(String[] args) {
        try {
            // Initialize Skyflow client
            HashMap<String, Object> data = new HashMap<>();
            data.put("skyflow_id", "<SKYFLOW_ID>");
            data.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");
            data.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>");

            HashMap<String, Object> tokens = new HashMap<>();
            tokens.put("<COLUMN_NAME_2>", "<TOKEN_VALUE_2>");

            UpdateRequest updateRequest = UpdateRequest.builder()
                    .table("<TABLE_NAME>")
                    .tokenMode(TokenMode.ENABLE)
                    .data(data)
                    .tokens(tokens)
                    .returnTokens(true)
                    .build();
            UpdateResponse updateResponse = skyflowClient.vault("<VAULT_ID>").update(updateRequest);
            System.out.println(updateResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/UpdateExample.java)
of update call:

```java
import com.skyflow.enums.TokenMode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateResponse;

import java.util.HashMap;

public class UpdateExample {
    public static void main(String[] args) {
        try {
            // Initialize Skyflow client
            HashMap<String, Object> data = new HashMap<>();
            data.put("skyflow_id", "5b699e2c-4301-4f9f-bcff-0a8fd3057413");
            data.put("name", "john doe");
            data.put("card_number", "4111111111111115");

            HashMap<String, Object> tokens = new HashMap<>();
            tokens.put("name", "72b8ffe3-c8d3-4b4f-8052-38b2a7405b5a");

            UpdateRequest updateRequest = UpdateRequest.builder()
                    .table("table1")
                    .tokenMode(TokenMode.ENABLE)
                    .data(data)
                    .tokens(tokens)
                    .build();
            UpdateResponse updateResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").update(updateRequest);
            System.out.println(updateResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Sample response:

- `returnTokens` set to `true`

```js
{
  "skyflowId": "5b699e2c-4301-4f9f-bcff-0a8fd3057413",
  "name": "72b8ffe3-c8d3-4b4f-8052-38b2a7405b5a",
  "card_number": "4315-7650-1359-9681"
}
```

- `returnTokens` set to `false`

```js
{
  "skyflowId": "5b699e2c-4301-4f9f-bcff-0a8fd3057413"
}
```

## Delete

To delete records using Skyflow IDs, use the `delete` method. The `DeleteRequest` class accepts a list of Skyflow IDs
that you want to delete, as shown below:

Delete schema:

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;

import java.util.ArrayList;

public class DeleteSchema {
    public static void main(String[] args) {
        try {
            // Initialize Skyflow client
            ArrayList<String> ids = new ArrayList<>();
            ids.add("<SKYFLOW_ID_1>");
            ids.add("<SKYFLOW_ID_2>");
            ids.add("<SKYFLOW_ID_3>");
            DeleteRequest deleteRequest = DeleteRequest.builder().ids(ids).table("<TABLE_NAME>").build();
            DeleteResponse deleteResponse = skyflowClient.vault("<VAULT_ID>").delete(deleteRequest);
            System.out.println(deleteResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/DeleteExample.java)
of delete call:

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;

import java.util.ArrayList;

public class DeleteExample {
    public static void main(String[] args) {
        try {
            // Initialize Skyflow client
            ArrayList<String> ids = new ArrayList<>();
            ids.add("9cbf66df-6357-48f3-b77b-0f1acbb69280");
            ids.add("ea74bef4-f27e-46fe-b6a0-a28e91b4477b");
            ids.add("47700796-6d3b-4b54-9153-3973e281cafb");
            DeleteRequest deleteRequest = DeleteRequest.builder().ids(ids).table("table1").build();
            DeleteResponse deleteResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").delete(deleteRequest);
            System.out.println(deleteResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Sample response:

```json
{
  "deletedIds": [
    "9cbf66df-6357-48f3-b77b-0f1acbb69280",
    "ea74bef4-f27e-46fe-b6a0-a28e91b4477b",
    "47700796-6d3b-4b54-9153-3973e281cafb"
  ]
}
```

### Query

To retrieve data with SQL queries, use the `query` method. The `QueryRequest` class accepts a `query` parameter as
follows:

Query Schema

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;

public class QueryExample {
    public static void main(String[] args) {
        try {
            // initialize Skyflow client
            String query = "<YOUR_SQL_QUERY>";
            QueryRequest queryRequest = QueryRequest.builder().query(query).build();
            QueryResponse queryResponse = skyflowClient.vault("<VAULT_ID>").query(queryRequest);
            System.out.println(queryResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

See [Query your data](https://docs.skyflow.com/query-data/)
and [Execute Query](https://docs.skyflow.com/record/#QueryService_ExecuteQuery) for guidelines and restrictions on
supported SQL statements, operators, and keywords.

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/QueryExample.java)
of query call:

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;

public class QueryExample {
    public static void main(String[] args) {
        try {
            // initialize Skyflow client
            String query = "SELECT * FROM cards WHERE skyflow_id='3ea3861-x107-40w8-la98-106sp08ea83f'";
            QueryRequest queryRequest = QueryRequest.builder().query(query).build();
            QueryResponse queryResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").query(queryRequest);
            System.out.println(queryResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Sample response:

```js
{
	"fields": [{
		"card_number": "XXXXXXXXXXXX1112",
		"name": "S***ar",
		"skyflow_id": "3ea3861-x107-40w8-la98-106sp08ea83f",
		"tokenizedData": null
	}]
}
```

# Connections

The [connections](https://github.com/skyflowapi/skyflow-java/tree/main/src/main/java/com/skyflow/vault/connection)
module is used to invoke INBOUND and/or OUTBOUND connections.

## Invoke Connection

Using Skyflow Connection, end-user applications can integrate checkout/card issuance flow with their apps/systems. To
invoke connection, use the `invoke` method of the Skyflow client.

Invoke Connection Schema:

```java
import com.skyflow.enums.RequestMethod;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import com.skyflow.vault.connection.InvokeConnectionResponse;

import java.util.HashMap;
import java.util.Map;

public class InvokeConnectionSchema {
    public static void main(String[] args) {
        try {
            // Initialize Skyflow client
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");
            requestBody.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>");

            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("<HEADER_NAME_1>", "<HEADER_VALUE_1>");
            requestHeaders.put("<HEADER_NAME_2>", "<HEADER_VALUE_2>");

            Map<String, String> pathParams = new HashMap<>();
            pathParams.put("<YOUR_PATH_PARAM_KEY_1>", "<YOUR_PATH_PARAM_VALUE_1>");
            pathParams.put("<YOUR_PATH_PARAM_KEY_2>", "<YOUR_PATH_PARAM_VALUE_2>");

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("<YOUR_QUERY_PARAM_KEY_1>", "<YOUR_QUERY_PARAM_VALUE_1>");
            queryParams.put("<YOUR_QUERY_PARAM_KEY_2>", "<YOUR_QUERY_PARAM_VALUE_2>");

            InvokeConnectionRequest invokeConnectionRequest = InvokeConnectionRequest.builder()
                    .method(RequestMethod.POST)
                    .requestBody(requestBody)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .build();
            InvokeConnectionResponse invokeConnectionResponse = skyflowClient.connection("<CONNECTION_ID>").invoke(invokeConnectionRequest);
            System.out.println(invokeConnectionResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

`methodName` supports the following methods:

- GET
- POST
- PUT
- PATCH
- DELETE

**pathParams, queryParams, requestHeader, requestBody** are the JSON objects represented as HashMaps, that will be sent
through the connection integration url.

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/connection/InvokeConnectionExample.java)
of invokeConnection:

```java
import com.skyflow.Skyflow;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.RequestMethod;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import com.skyflow.vault.connection.InvokeConnectionResponse;

import java.util.HashMap;
import java.util.Map;

public class InvokeConnectionExample {
    public static void main(String[] args) {
        try {
            Credentials credentials = new Credentials();
            credentials.setPath("/path/to/credentials.json");

            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setConnectionId("");
            connectionConfig.setConnectionUrl("https://connection.url.com");
            connectionConfig.setCredentials(credentials);

            Skyflow skyflowClient = Skyflow.builder()
                    .setLogLevel(LogLevel.DEBUG)
                    .addConnectionConfig(connectionConfig)
                    .build();

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("card_number", "4337-1696-5866-0865");
            requestBody.put("ssn", "524-41-4248");

            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("Content-Type", "application/json");

            InvokeConnectionRequest invokeConnectionRequest = InvokeConnectionRequest.builder()
                    .method(RequestMethod.POST)
                    .requestBody(requestBody)
                    .requestHeaders(requestHeaders)
                    .build();
            InvokeConnectionResponse invokeConnectionResponse = skyflowClient.connection("<CONNECTION_ID>").invoke(invokeConnectionRequest);
            System.out.println(invokeConnectionResponse);
        } catch (SkyflowException e) {
            System.out.println("Error occurred: ");
            System.out.println(e);
        }
    }
}
```

Sample response:

```js
InvokeConnectionResponse{
  response={
    "card_number":"4337-1696-5866-0865",
    "ssn":"524-41-4248"
}
```

## Logging

The skyflow java SDK provides useful logging using python's inbuilt `logging` library. By default, the logging level of
the SDK is set to `LogLevel.ERROR`. This can be changed by using `setLogLevel(logLevel)` method as shown below:

```java
import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

public class ChangeLogLevel {
    public static void main(String[] args) throws SkyflowException {
        // Pass only one of apiKey, token, credentialsString or path in credentials        
        Credentials credentials = new Credentials();
        credentials.setToken("<BEARER_TOKEN>");

        VaultConfig config = new VaultConfig();
        config.setVaultId("<VAULT_ID>");      // Primary vault
        config.setClusterId("<CLUSTER_ID>");  // ID from your vault URL Eg https://{clusterId}.vault.skyflowapis.com
        config.setEnv(Env.PROD);              // Env by default is set to PROD
        config.setCredentials(credentials);   // Individual credentials

        JsonObject credentialsObject = new JsonObject();
        credentialsObject.addProperty("clientID", "<YOUR_CLIENT_ID>");
        credentialsObject.addProperty("clientName", "<YOUR_CLIENT_NAME>");
        credentialsObject.addProperty("TokenURI", "<YOUR_TOKEN_URI>");
        credentialsObject.addProperty("keyID", "<YOUR_KEY_ID>");
        credentialsObject.addProperty("privateKey", "<YOUR_PRIVATE_KEY>");

        // To generate Bearer Token from credentials string.
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setCredentialsString(credentialsObject.toString());

        Skyflow skyflowClient = Skyflow.builder()
                .addVaultConfig(config)                    // Add vault config
                .addSkyflowCredentials(skyflowCredentials) // Skyflow credentials will be used if no individual credentials are passed
                .setLogLevel(LogLevel.INFO)                // Set log level. By default, it is set to ERROR
                .build();
    }
}
```

Currently, the following 5 log levels are supported:

- `DEBUG`:

  When `LogLevel.DEBUG` is passed, all level of logs will be printed(DEBUG, INFO, WARN, ERROR)

- `INFO`:

  When `LogLevel.INFO` is passed, INFO logs for every event that has occurred during the SDK flow execution will be
  printed along with WARN and ERROR logs

- `WARN`:

  When `LogLevel.WARN` is passed, WARN and ERROR logs will be printed

- `ERROR`:

  When `LogLevel.ERROR` is passed, only ERROR logs will be printed.

- `OFF`:

  `LogLevel.OFF` can be used to turn off all logging from the skyflow-java SDK.

`Note`: The ranking of logging levels is as follows :  `DEBUG` < `INFO` < `WARN` < `ERROR`.

## Reporting a Vulnerability

If you discover a potential security issue in this project, please reach out to us at security@skyflow.com. Please do
not create public GitHub issues or Pull Requests, as malicious actors could potentially view them.
