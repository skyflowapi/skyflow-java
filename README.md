# Skyflow Java

The Skyflow Java SDK is designed to help with integrating Skyflow into a Java backend.

[![CI](https://img.shields.io/static/v1?label=CI&message=passing&color=green?style=plastic&logo=github)](https://github.com/skyflowapi/skyflow-java/actions)
[![GitHub release](https://img.shields.io/github/v/release/skyflowapi/skyflow-java.svg)](https://mvnrepository.com/artifact/com.skyflow/skyflow-java)
[![License](https://img.shields.io/github/license/skyflowapi/skyflow-java)](https://github.com/skyflowapi/skyflow-java/blob/main/LICENSE)

# Table of Contents

- [Table of Contents](#table-of-contents)
- [Overview](#overview)
- [Install](#install)
  - [Requirements](#requirements)
  - [Configuration](#configuration)
    - [Gradle users](#gradle-users)
    - [Maven users](#maven-users)
- [Migration from v1 to v2](#migration-from-v1-to-v2)
  - [Authentication options](#authentication-options)
  - [Initializing the client](#initializing-the-client)
  - [Request & response structure](#request--response-structure)
  - [Request options](#request-options)
  - [Error structure](#error-structure)
- [Quickstart](#quickstart)
  - [Authenticate](#authenticate)
  - [Initialize the client](#initialize-the-client)
  - [Insert data into the vault](#insert-data-into-the-vault)
- [Vault](#vault)
  - [Insert data into the vault](#insert-data-into-the-vault-1)
  - [Detokenize](#detokenize)
  - [Tokenize](#tokenize)
  - [Get](#get)
    - [Get by skyflow IDS](#get-by-skyflow-ids)
    - [Get tokens](#get-tokens)
    - [Get by column name and column values](#get-by-column-name-and-column-values)
    - [Redaction types](#redaction-types)
  - [Update](#update)
  - [Delete](#delete)
  - [Query](#query)
- [Detect](#detect)
  - [Deidentify Text](#deidentify-text)
  - [Reidentify Text](#reidentify-text)
  - [Deidentify File](#deidentify-file)
  - [Get Run](#get-run)
- [Connections](#connections)
  - [Invoke a connection](#invoke-a-connection)
- [Authenticate with bearer tokens](#authenticate-with-bearer-tokens)
  - [Generate a bearer token](#generate-a-bearer-token)
  - [Generate bearer tokens with context](#generate-bearer-tokens-with-context)
  - [Generate scoped bearer tokens](#generate-scoped-bearer-tokens)
  - [Generate signed data tokens](#generate-signed-data-tokens)
  - [Bearer token expiry edge case](#bearer-token-expiry-edge-case)
- [Logging](#logging)
- [Reporting a Vulnerability](#reporting-a-vulnerability)

# Overview

- Authenticate using a Skyflow service account and generate bearer tokens for secure access.
- Perform Vault API operations such as inserting, retrieving, and tokenizing sensitive data with ease.
- Invoke connections to third-party APIs without directly handling sensitive data, ensuring compliance and data protection.

# Install

## Requirements

- Java 8 and above (tested with Java 8)

## Configuration

---

### Gradle users

Add this dependency to your project's `build.gradle` file:

```
implementation 'com.skyflow:skyflow-java:2.0.0'
```

### Maven users

Add this dependency to your project's `pom.xml` file:

```xml
<dependency>
    <groupId>com.skyflow</groupId>
    <artifactId>skyflow-java</artifactId>
    <version>2.0.0</version>
</dependency>
```

---

# Migrate from v1 to v2

Below are the steps to migrate the java sdk from v1 to v2.

### Authentication options

In V2, we have introduced multiple authentication options. You can now provide credentials in the following ways:

- Passing credentials in ENV. (`SKYFLOW_CREDENTIALS`) _(Recommended)_
- API Key
- Path to your credentials JSON file
- Stringified JSON of your credentials
- Bearer token

These options allow you to choose the authentication method that best suits your use case.

**V1 (Old)**

```java
static class DemoTokenProvider implements TokenProvider {
    @Override
    public String getBearerToken() throws Exception {
        ResponseToken res = null;
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_HERE>";
            res = Token.generateBearerToken(filePath);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
        return res.getAccessToken();
    }
}
```

**V2 (New): Passing one of the following:**

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

Notes:

- Use only ONE authentication method.
- API Key or environment variables are recommended for production use.
- Secure storage of credentials is essential.
- For overriding behavior and priority order of credentials, please refer to [Initialize the client](#initialize-the-client) section in [Quickstart](#quickstart).

---

### Initializing the client

In V2, we have introduced a builder design pattern for client initialization and added support for multi-vault. This allows you to configure multiple vaults during client initialization. In V2, the log level is tied to each individual client instance. During client initialization, you can pass the following parameters:

- `vaultId` and `clusterId`: These values are derived from the vault ID & vault URL.
- `env`: Specify the environment (e.g., SANDBOX or PROD).
- `credentials`: The necessary authentication credentials.

**V1 (Old)**

```java
// DemoTokenProvider class is an implementation of the TokenProvider interface
DemoTokenProvider demoTokenProvider = new DemoTokenProvider();
SkyflowConfiguration skyflowConfig = new SkyflowConfiguration("<VAULT_ID>","<VAULT_URL>", demoTokenProvider);
Skyflow skyflowClient = Skyflow.init(skyflowConfig);
```

**V2 (New)**

```java
Credentials credentials = new Credentials();
credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH_1>"); // Replace with the path to the credentials file

// Configure the first vault (Blitz)
VaultConfig config = new VaultConfig();
config.setVaultId("<YOUR_VAULT>"); // Replace with the ID of the first vault
config.setClusterId("<YOUR_CLUSTER>"); // Replace with the cluster ID of the first vault
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

---

### Request & response structure

In V2, we have removed the use of JSON objects from a third-party package. Instead, we have transitioned to accepting native ArrayList and HashMap data structures and adopted the builder pattern for request creation. This request needs:

- `table`: The name of the table.
- `values`: An array list of objects containing the data to be inserted.

The response will be of type `InsertResponse` class, which contains `insertedFields` and `errors`.

**V1 (Old):** Request building

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

**V2 (New):** Request building

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

**V1 (Old):** Response structure

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

**V2 (New):** Response structure

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

### Request options

In V2, with the introduction of the builder design pattern has made handling optional fields in Java more efficient and straightforward.

**V1 (Old)**

```java
InsertOptions insertOptions = new InsertOptions(true);
```

**V2 (New)**

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

### Error structure

In V2, we have enriched the error details to provide better debugging capabilities.
The error response now includes:

- `httpStatus`: The HTTP status code.
- `grpcCode`: The gRPC code associated with the error.
- `details` & `message`: A detailed description of the error.
- `requestId`: A unique request identifier for easier debugging.

**V1 (Old):** Error structure

```json
{
  "code": "<http_code>",
  "description": "<description>"
}
```

**V2 (New):** Error structure

```js
{
  "httpStatus": "<http_status>",
  "grpcCode": <grpc_code>,
  "httpCode": <http_code>,
  "message": "<message>",
  "requestId": "<request_id>",
  "details": ["<details>"]
}
```

# Quickstart

Get started quickly with the essential steps: authenticate, initialize the client, and perform a basic vault operation. This section provides a minimal setup to help you integrate the SDK efficiently.

### Authenticate

You can use an API key to authenticate and authorize requests to an API. For authenticating via bearer tokens and different supported bearer token types, refer to the [Authenticate with bearer tokens](#authenticate-with-bearer-tokens) section.

```java
// create a new credentials object
Credentials credentials = new Credentials();
credentials.setApiKey("<API_KEY>"); // add your API key in credentials
```

### Initialize the client

To get started, you must first initialize the skyflow client. While initializing the skyflow client, you can specify different types of credentials.

1. **API keys**  
   A unique identifier used to authenticate and authorize requests to an API.

2. **Bearer tokens**  
   A temporary access token used to authenticate API requests, typically included in the Authorization header.

3. **Service account credentials file path**  
   The file path pointing to a JSON file containing credentials for a service account, used for secure API access.

4. **Service account credentials string (JSON formatted)**  
   A JSON-formatted string containing service account credentials, often used as an alternative to a file for programmatic authentication.

Note: Only one type of credential can be used at a time. If multiple credentials are provided, the last one added will take precedence.

```java
import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

/**
 * Example program to initialize the Skyflow client with various configurations.
 * The Skyflow client facilitates secure interactions with the Skyflow vault,
 * such as securely managing sensitive data.
 */
public class InitSkyflowClient {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Define the primary credentials for authentication.
        // Note: Only one type of credential can be used at a time. You can choose between:
        // - API key
        // - Bearer token
        // - A credentials string (JSON-formatted)
        // - A file path to a credentials file.

        // Initialize primary credentials using a Bearer token for authentication.
        Credentials primaryCredentials = new Credentials();
        primaryCredentials.setToken("<BEARER_TOKEN>"); // Replace <BEARER_TOKEN> with your actual authentication token.

        // Step 2: Configure the primary vault details.
        // VaultConfig stores all necessary details to connect to a specific Skyflow vault.
        VaultConfig primaryConfig = new VaultConfig();
        primaryConfig.setVaultId("<PRIMARY_VAULT_ID>"); // Replace with your primary vault's ID.
        primaryConfig.setClusterId("<CLUSTER_ID>");     // Replace with the cluster ID (part of the vault URL, e.g., https://{clusterId}.vault.skyflowapis.com).
        primaryConfig.setEnv(Env.PROD);                 // Set the environment (PROD, SANDBOX, STAGE, DEV).
        primaryConfig.setCredentials(primaryCredentials); // Attach the primary credentials to this vault configuration.

        // Step 3: Create credentials as a JSON object (if a Bearer Token is not provided).
        // Demonstrates an alternate approach to authenticate with Skyflow using a credentials object.
        JsonObject credentialsObject = new JsonObject();
        credentialsObject.addProperty("clientID", "<YOUR_CLIENT_ID>");       // Replace with your Client ID.
        credentialsObject.addProperty("clientName", "<YOUR_CLIENT_NAME>");   // Replace with your Client Name.
        credentialsObject.addProperty("TokenURI", "<YOUR_TOKEN_URI>");       // Replace with the Token URI.
        credentialsObject.addProperty("keyID", "<YOUR_KEY_ID>");             // Replace with your Key ID.
        credentialsObject.addProperty("privateKey", "<YOUR_PRIVATE_KEY>");   // Replace with your Private Key.

        // Step 4: Convert the JSON object to a string and use it as credentials.
        // This approach allows the use of dynamically generated or pre-configured credentials.
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setCredentialsString(credentialsObject.toString()); // Converts JSON object to string for use as credentials.

        // Step 5: Define secondary credentials (API key-based authentication as an example).
        // Demonstrates a different type of authentication mechanism for Skyflow vaults.
        Credentials secondaryCredentials = new Credentials();
        secondaryCredentials.setApiKey("<API_KEY>"); // Replace with your API Key for authentication.

        // Step 6: Configure the secondary vault details.
        // A secondary vault configuration can be used for operations involving multiple vaults.
        VaultConfig secondaryConfig = new VaultConfig();
        secondaryConfig.setVaultId("<SECONDARY_VAULT_ID>"); // Replace with your secondary vault's ID.
        secondaryConfig.setClusterId("<CLUSTER_ID>");       // Replace with the corresponding cluster ID.
        secondaryConfig.setEnv(Env.SANDBOX);                 // Set the environment for this vault.
        secondaryConfig.setCredentials(secondaryCredentials); // Attach the secondary credentials to this configuration.

        // Step 7: Define tertiary credentials using a path to a credentials JSON file.
        // This method demonstrates an alternative authentication method.
        Credentials tertiaryCredentials = new Credentials();
        tertiaryCredentials.setPath("<PATH_TO_YOUR_CREDENTIALS_JSON_FILE>"); // Replace with the path to your credentials file.

        // Step 8: Configure the tertiary vault details.
        VaultConfig tertiaryConfig = new VaultConfig();
        tertiaryConfig.setVaultId("<TERTIARY_VAULT_ID>");   // Replace with the tertiary vault ID.
        tertiaryConfig.setClusterId("<CLUSTER_ID>");        // Replace with the corresponding cluster ID.
        tertiaryConfig.setEnv(Env.STAGE);                    // Set the environment for this vault.
        tertiaryConfig.setCredentials(tertiaryCredentials);  // Attach the tertiary credentials.

        // Step 9: Build and initialize the Skyflow client.
        // Skyflow client is configured with multiple vaults and credentials.
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.INFO)                  // Set log level for debugging or monitoring purposes.
                .addVaultConfig(primaryConfig)               // Add the primary vault configuration.
                .addVaultConfig(secondaryConfig)             // Add the secondary vault configuration.
                .addVaultConfig(tertiaryConfig)              // Add the tertiary vault configuration.
                .addSkyflowCredentials(skyflowCredentials)   // Add JSON-formatted credentials if applicable.
                .build();

        // The Skyflow client is now fully initialized.
        // Use the `skyflowClient` object to perform secure operations such as:
        // - Inserting data
        // - Retrieving data
        // - Deleting data
        // within the configured Skyflow vaults.
    }
}
```

Notes:

- If both Skyflow common credentials and individual credentials at the configuration level are specified, the individual credentials at the configuration level will take precedence.
- If neither Skyflow common credentials nor individual configuration-level credentials are provided, the SDK attempts to retrieve credentials from the `SKYFLOW_CREDENTIALS` environment variable.
- All Vault operations require a client instance.

### Insert data into the vault

To insert data into your vault, use the `insert` method. The `InsertRequest` class creates an insert request, which includes the values to be inserted as a list of records. Below is a simple example to get started. For advanced options, check out [Insert data into the vault](#insert-data-into-the-vault-1) section.

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This example demonstrates how to insert sensitive data (e.g., card information) into a Skyflow vault using the Skyflow client.
 *
 * 1. Initializes the Skyflow client.
 * 2. Prepares a record with sensitive data (e.g., card number and cardholder name).
 * 3. Creates an insert request for inserting the data into the Skyflow vault.
 * 4. Prints the response of the insert operation.
 */
public class InsertExample {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Initialize data to be inserted into the Skyflow vault
            ArrayList<HashMap<String, Object>> insertData = new ArrayList<>();

            // Create a HashMap for a single record with card number and cardholder name as fields
            HashMap<String, Object> insertRecord = new HashMap<>();
            insertRecord.put("card_number", "4111111111111111"); // Replace with actual card number (sensitive data)
            insertRecord.put("cardholder_name", "john doe");     // Replace with actual cardholder name (sensitive data)

            // Add the created record to the list of data to be inserted
            insertData.add(insertRecord);

            // Step 2: Build the InsertRequest object with the table name and data to insert
            InsertRequest insertRequest = InsertRequest.builder()
                    .table("table1")                  // Specify the table in the vault where the data will be inserted
                    .values(insertData)               // Attach the data (records) to be inserted
                    .returnTokens(true)               // Specify if tokens should be returned upon successful insertion
                    .build();                         // Build the insert request object

            // Step 3: Perform the insert operation using the Skyflow client
            InsertResponse insertResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").insert(insertRequest);
            // Replace the vault ID "9f27764a10f7946fe56b3258e117" with your actual Skyflow vault ID

            // Step 4: Print the response from the insert operation
            System.out.println(insertResponse);
        } catch (SkyflowException e) {
            // Step 5: Handle any exceptions that may occur during the insert operation
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the stack trace for debugging purposes
        }
    }
}
```

Skyflow returns tokens for the record that was just inserted.

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

# Vault

The [Vault](https://github.com/skyflowapi/skyflow-java/tree/main/src/main/java/com/skyflow/vault) module performs operations on the vault, including inserting records, detokenizing tokens, and retrieving tokens associated with a `skyflow_id`.

## Insert data into the vault

Apart from using the `insert` method to insert data into your vault covered in [Quickstart](#quickstart), you can also specify options in `InsertRequest`, such as returning tokenized data, upserting records, or continuing the operation in case of errors.

### Construct an insert request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Example program to demonstrate inserting data into a Skyflow vault, along with corresponding InsertRequest schema.
 *
 */
public class InsertSchema {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Prepare the data to be inserted into the Skyflow vault
            ArrayList<HashMap<String, Object>> insertData = new ArrayList<>();

            // Create the first record with field names and their respective values
            HashMap<String, Object> insertRecord1 = new HashMap<>();
            insertRecord1.put("<FIELD_NAME_1>", "<VALUE_1>"); // Replace with actual field name and value
            insertRecord1.put("<FIELD_NAME_2>", "<VALUE_2>"); // Replace with actual field name and value

            // Create the second record with field names and their respective values
            HashMap<String, Object> insertRecord2 = new HashMap<>();
            insertRecord2.put("<FIELD_NAME_1>", "<VALUE_1>"); // Replace with actual field name and value
            insertRecord2.put("<FIELD_NAME_2>", "<VALUE_2>"); // Replace with actual field name and value

            // Add the records to the list of data to be inserted
            insertData.add(insertRecord1);
            insertData.add(insertRecord2);

            // Step 2: Build an InsertRequest object with the table name and the data to insert
            InsertRequest insertRequest = InsertRequest.builder()
                    .table("<TABLE_NAME>") // Replace with the actual table name in your Skyflow vault
                    .values(insertData)   // Attach the data to be inserted
                    .build();

            // Step 3: Use the Skyflow client to perform the insert operation
            InsertResponse insertResponse = skyflowClient.vault("<VAULT_ID>").insert(insertRequest);
            // Replace <VAULT_ID> with your actual vault ID

            // Print the response from the insert operation
            System.out.println("Insert Response: " + insertResponse);
        } catch (SkyflowException e) {
            // Step 4: Handle any exceptions that occur during the insert operation
            System.out.println("Error occurred while inserting data: ");
            e.printStackTrace(); // Print the stack trace for debugging
        }
    }
}
```

### Insert call [example](https://github.com/skyflowapi/skyflow-java/blob/SK-1893-update-readme-for-v2/samples/src/main/java/com/example/vault/InsertExample.java) with `continueOnError` option

The `continueOnError` flag is a boolean that determines whether insert operation should proceed despite encountering partial errors. Set to `true` to allow the process to continue even if some errors occur.

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This example demonstrates how to insert multiple records into a Skyflow vault using the Skyflow client.
 *
 * 1. Initializes the Skyflow client.
 * 2. Prepares multiple records with sensitive data (e.g., card number and cardholder name).
 * 3. Creates an insert request with the records to insert into the Skyflow vault.
 * 4. Specifies options to continue on error and return tokens.
 * 5. Prints the response of the insert operation.
 */
public class InsertExample {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Initialize a list to hold the data records to be inserted into the vault
            ArrayList<HashMap<String, Object>> insertData = new ArrayList<>();

            // Step 2: Create the first record with card number and cardholder name
            HashMap<String, Object> insertRecord1 = new HashMap<>();
            insertRecord1.put("card_number", "4111111111111111"); // Replace with actual card number (sensitive data)
            insertRecord1.put("cardholder_name", "john doe");     // Replace with actual cardholder name (sensitive data)

            // Step 3: Create the second record with card number and cardholder name
            HashMap<String, Object> insertRecord2 = new HashMap<>();
            insertRecord2.put("card_number", "4111111111111111"); // Ensure field name matches ("card_number")
            insertRecord2.put("cardholder_name", "jane doe");     // Replace with actual cardholder name (sensitive data)

            // Step 4: Add the records to the insertData list
            insertData.add(insertRecord1);
            insertData.add(insertRecord2);

            // Step 5: Build the InsertRequest object with the data records to insert
            InsertRequest insertRequest = InsertRequest.builder()
                    .table("table1")                      // Specify the table in the vault where data will be inserted
                    .values(insertData)                   // Attach the data records to be inserted
                    .returnTokens(true)                   // Specify if tokens should be returned upon successful insertion
                    .continueOnError(true)                // Specify to continue inserting records even if an error occurs for some records
                    .build();                             // Build the insert request object

            // Step 6: Perform the insert operation using the Skyflow client
            InsertResponse insertResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").insert(insertRequest);
            // Replace the vault ID "9f27764a10f7946fe56b3258e117" with your actual Skyflow vault ID

            // Step 7: Print the response from the insert operation
            System.out.println(insertResponse);
        } catch (SkyflowException e) {
            // Step 8: Handle any exceptions that may occur during the insert operation
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the stack trace for debugging purposes
        }
    }
}
```

Sample response:

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
  "errors": [
    {
      "request_index": "1",
      "error": "Insert failed. Column card_numbe is invalid. Specify a valid column."
    }
  ]
}
```

### Insert call example with `upsert` option

An upsert operation checks for a record based on a unique column's value. If a match exists, the record is updated; otherwise, a new record is inserted.

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This example demonstrates how to insert or upsert a record into a Skyflow vault using the Skyflow client, with the option to return tokens.
 *
 * 1. Initializes the Skyflow client.
 * 2. Prepares a record to insert or upsert (e.g., cardholder name).
 * 3. Creates an insert request with the data to be inserted or upserted into the Skyflow vault.
 * 4. Specifies the field (cardholder_name) for upsert operations.
 * 5. Prints the response of the insert or upsert operation.
 */
public class UpsertExample {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Initialize a list to hold the data records for the insert/upsert operation
            ArrayList<HashMap<String, Object>> upsertData = new ArrayList<>();

            // Step 2: Create a record with the field 'cardholder_name' to insert or upsert
            HashMap<String, Object> upsertRecord = new HashMap<>();
            upsertRecord.put("cardholder_name", "jane doe"); // Replace with the actual cardholder name

            // Step 3: Add the record to the upsertData list
            upsertData.add(upsertRecord);

            // Step 4: Build the InsertRequest object with the upsertData
            InsertRequest insertRequest = InsertRequest.builder()
                    .table("table1")                      // Specify the table in the vault where data will be inserted/upserted
                    .values(upsertData)                   // Attach the data records to be inserted/upserted
                    .returnTokens(true)                   // Specify if tokens should be returned upon successful operation
                    .upsert("cardholder_name")            // Specify the field to be used for upsert operations (e.g., cardholder_name)
                    .build();                             // Build the insert request object

            // Step 5: Perform the insert/upsert operation using the Skyflow client
            InsertResponse insertResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").insert(insertRequest);
            // Replace the vault ID "9f27764a10f7946fe56b3258e117" with your actual Skyflow vault ID

            // Step 6: Print the response from the insert/upsert operation
            System.out.println(insertResponse);
        } catch (SkyflowException e) {
            // Step 7: Handle any exceptions that may occur during the insert/upsert operation
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the stack trace for debugging purposes
        }
    }
}
```

Skyflow returns tokens, with `upsert` support, for the record you just inserted.

```json
{
  "insertedFields": [
    {
      "skyflowId": "9fac9201-7b8a-4446-93f8-5244e1213bd1",
      "cardholder_name": "73ce45ce-20fd-490e-9310-c1d4f603ee83"
    }
  ],
  "errors": []
}
```

## Detokenize

To retrieve tokens from your vault, use the `detokenize` method. The `DetokenizeRequest` class requires a list of detokenization data as input. Additionally, you can provide optional parameters, such as the redaction type and the option to continue on error.

### Construct a detokenize request

```java
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to detokenize sensitive data from tokens stored in a Skyflow vault, along with corresponding DetokenizeRequest schema.
 *
 */
public class DetokenizeSchema {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Initialize a list of tokens to be detokenized (replace with actual tokens)
            ArrayList<String> tokens = new ArrayList<>();
            tokens.add("<YOUR_TOKEN_VALUE_1>"); // Replace with your actual token value
            tokens.add("<YOUR_TOKEN_VALUE_2>"); // Replace with your actual token value
            tokens.add("<YOUR_TOKEN_VALUE_3>"); // Replace with your actual token value

            // Step 2: Create the DetokenizeRequest object with the tokens and redaction type
            DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                    .tokens(tokens)                        // Provide the list of tokens to be detokenized
                    .continueOnError(true)                  // Continue even if one token cannot be detokenized
                    .redactionType(RedactionType.PLAIN_TEXT) // Specify how the detokenized data should be returned (plain text)
                    .build();                               // Build the detokenization request

            // Step 3: Call the Skyflow vault to detokenize the provided tokens
            DetokenizeResponse detokenizeResponse = skyflowClient.vault("<VAULT_ID>").detokenize(detokenizeRequest);
            // Replace <VAULT_ID> with your actual Skyflow vault ID

            // Step 4: Print the detokenization response, which contains the detokenized data
            System.out.println(detokenizeResponse);
        } catch (SkyflowException e) {
            // Step 5: Handle any errors that occur during the detokenization process
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception for debugging purposes
        }
    }
}
```

Notes:

- `redactionType` defaults to [`RedactionType.PLAIN_TEXT`](#redaction-types).
- `continueOnError` defaults to `true`.

### An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/DetokenizeExample.java) of a detokenize call:

```java
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to detokenize sensitive data from tokens stored in a Skyflow vault.
 *
 * 1. Initializes the Skyflow client.
 * 2. Creates a list of tokens (e.g., credit card tokens) that represent the sensitive data.
 * 3. Builds a detokenization request using the provided tokens and specifies how the redacted data should be returned.
 * 4. Calls the Skyflow vault to detokenize the tokens and retrieves the detokenized data.
 * 5. Prints the detokenization response, which contains the detokenized values or errors.
 */
public class DetokenizeExample {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Initialize a list of tokens to be detokenized (replace with actual token values)
            ArrayList<String> tokens = new ArrayList<>();
            tokens.add("9738-1683-0486-1480"); // Replace with your actual token value
            tokens.add("6184-6357-8409-6668"); // Replace with your actual token value
            tokens.add("4914-9088-2814-3840"); // Replace with your actual token value

            // Step 2: Create the DetokenizeRequest object with the tokens and redaction type
            DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                    .tokens(tokens)                        // Provide the list of tokens to be detokenized
                    .continueOnError(false)                 // Stop the process if any token cannot be detokenized
                    .redactionType(RedactionType.PLAIN_TEXT) // Specify how the detokenized data should be returned (plain text)
                    .build();                               // Build the detokenization request

            // Step 3: Call the Skyflow vault to detokenize the provided tokens
            DetokenizeResponse detokenizeResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").detokenize(detokenizeRequest);
            // Replace "9f27764a10f7946fe56b3258e117" with your actual Skyflow vault ID

            // Step 4: Print the detokenization response, which contains the detokenized data
            System.out.println(detokenizeResponse);
        } catch (SkyflowException e) {
            // Step 5: Handle any errors that occur during the detokenization process
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception for debugging purposes
        }
    }
}
```

Sample response:

```json
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

### An example of a detokenize call with `continueOnError` option:

```java
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to detokenize sensitive data (e.g., credit card numbers) from tokens in a Skyflow vault.
 *
 * 1. Initializes the Skyflow client.
 * 2. Creates a list of tokens (e.g., credit card tokens) to be detokenized.
 * 3. Builds a detokenization request with the tokens and specifies the redaction type for the detokenized data.
 * 4. Calls the Skyflow vault to detokenize the tokens and retrieves the detokenized data.
 * 5. Prints the detokenization response, which includes the detokenized values or errors.
 */
public class DetokenizeExample {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Initialize a list of tokens to be detokenized (replace with actual token values)
            ArrayList<String> tokens = new ArrayList<>();
            tokens.add("9738-1683-0486-1480"); // Example token value 1
            tokens.add("6184-6357-8409-6668"); // Example token value 2
            tokens.add("4914-9088-2814-384");  // Example token value 3

            // Step 2: Create the DetokenizeRequest object with the tokens and redaction type
            DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                    .tokens(tokens)                        // Provide the list of tokens to detokenize
                    .continueOnError(true)                  // Continue even if some tokens cannot be detokenized
                    .redactionType(RedactionType.PLAIN_TEXT) // Specify the format for the detokenized data (plain text)
                    .build();                               // Build the detokenization request

            // Step 3: Call the Skyflow vault to detokenize the provided tokens
            DetokenizeResponse detokenizeResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").detokenize(detokenizeRequest);
            // Replace "9f27764a10f7946fe56b3258e117" with your actual Skyflow vault ID

            // Step 4: Print the detokenization response, which contains the detokenized data or errors
            System.out.println(detokenizeResponse);
        } catch (SkyflowException e) {
            // Step 5: Handle any errors that occur during the detokenization process
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception for debugging purposes
        }
    }
}
```

Sample response:

```json
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

Tokenization replaces sensitive data with unique identifier tokens. This approach protects sensitive information by securely storing the original data while allowing the use of tokens within your application.

To tokenize data, use the `tokenize` method. The `TokenizeRequest` class creates a tokenize request. In this request, you specify the `values` parameter, which is a list of `ColumnValue` objects. Each `ColumnValue` contains two properties: `value` and `columnGroup`.

### Construct a tokenize request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.TokenizeRequest;
import com.skyflow.vault.tokens.TokenizeResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to tokenize sensitive data (e.g., credit card information) using the Skyflow client, along with corresponding TokenizeRequest schema.
 *
 */
public class TokenizeSchema {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Initialize a list of column values to be tokenized (replace with actual sensitive data)
            ArrayList<ColumnValue> columnValues = new ArrayList<>();

            // Step 2: Create column values for each sensitive data field (e.g., card number and cardholder name)
            ColumnValue columnValue1 = ColumnValue.builder().value("<VALUE>").columnGroup("<COLUMN_GROUP>").build(); // Replace <VALUE> and <COLUMN_GROUP> with actual data
            ColumnValue columnValue2 = ColumnValue.builder().value("<VALUE>").columnGroup("<COLUMN_GROUP>").build(); // Replace <VALUE> and <COLUMN_GROUP> with actual data

            // Add the created column values to the list
            columnValues.add(columnValue1);
            columnValues.add(columnValue2);

            // Step 3: Build the TokenizeRequest with the column values
            TokenizeRequest tokenizeRequest = TokenizeRequest.builder().values(columnValues).build();

            // Step 4: Call the Skyflow vault to tokenize the sensitive data
            TokenizeResponse tokenizeResponse = skyflowClient.vault("<VAULT_ID>").tokenize(tokenizeRequest);
            // Replace <VAULT_ID> with your actual Skyflow vault ID

            // Step 5: Print the tokenization response, which contains the generated tokens or errors
            System.out.println(tokenizeResponse);
        } catch (SkyflowException e) {
            // Step 6: Handle any errors that occur during the tokenization process
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception for debugging purposes
        }
    }
}
```

### An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/TokenizeExample.java) of Tokenize call:

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.TokenizeRequest;
import com.skyflow.vault.tokens.TokenizeResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to tokenize sensitive data (e.g., credit card information) using the Skyflow client.
 *
 * 1. Initializes the Skyflow client.
 * 2. Creates a column value for sensitive data (e.g., credit card number).
 * 3. Builds a tokenize request with the column value to be tokenized.
 * 4. Sends the request to the Skyflow vault for tokenization.
 * 5. Prints the tokenization response, which includes the token or errors.
 */
public class TokenizeExample {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Initialize a list of column values to be tokenized (replace with actual sensitive data)
            ArrayList<ColumnValue> columnValues = new ArrayList<>();

            // Step 2: Create a column value for the sensitive data (e.g., card number with its column group)
            ColumnValue columnValue = ColumnValue.builder()
                    .value("4111111111111111") // Replace with the actual sensitive data (e.g., card number)
                    .columnGroup("card_number_cg") // Replace with the actual column group name
                    .build();

            // Add the created column value to the list
            columnValues.add(columnValue);

            // Step 3: Build the TokenizeRequest with the column value
            TokenizeRequest tokenizeRequest = TokenizeRequest.builder().values(columnValues).build();

            // Step 4: Call the Skyflow vault to tokenize the sensitive data
            TokenizeResponse tokenizeResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").tokenize(tokenizeRequest);
            // Replace "9f27764a10f7946fe56b3258e117" with your actual Skyflow vault ID

            // Step 5: Print the tokenization response, which contains the generated token or any errors
            System.out.println(tokenizeResponse);
        } catch (SkyflowException e) {
            // Step 6: Handle any errors that occur during the tokenization process
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception for debugging purposes
        }
    }
}
```

Sample response:

```json
{
	"tokens": [5479-4229-4622-1393]
}
```

## Get

To retrieve data using Skyflow IDs or unique column values, use the `get` method. The `GetRequest` class creates a get request, where you specify parameters such as the table name, redaction type, Skyflow IDs, column names, column values, and whether to return tokens. If you specify Skyflow IDs, you can't use column names and column values, and the inverse is true—if you specify column names and column values, you can't use Skyflow IDs.

### Construct a get request

```java
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to retrieve data from the Skyflow vault using different methods, along with corresponding GetRequest schema.
 *
 */
public class GetSchema {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Initialize a list of Skyflow IDs to retrieve records (replace with actual Skyflow IDs)
            ArrayList<String> ids = new ArrayList<>();
            ids.add("<SKYFLOW_ID_1>"); // Replace with actual Skyflow ID
            ids.add("<SKYFLOW_ID_2>"); // Replace with actual Skyflow ID

            // Step 2: Create a GetRequest to retrieve records by Skyflow ID without returning tokens
            GetRequest getByIdRequest = GetRequest.builder()
                    .ids(ids)
                    .table("<TABLE_NAME>") // Replace with the actual table name
                    .returnTokens(false) // Set to false to avoid returning tokens
                    .redactionType(RedactionType.PLAIN_TEXT) // Redact data as plain text
                    .build();

            // Send the request to the Skyflow vault and retrieve the records
            GetResponse getByIdResponse = skyflowClient.vault("<VAULT_ID>").get(getByIdRequest); // Replace with actual Vault ID
            System.out.println(getByIdResponse);

            // Step 3: Create another GetRequest to retrieve records by Skyflow ID with tokenized values
            GetRequest getTokensRequest = GetRequest.builder()
                    .ids(ids)
                    .table("<TABLE_NAME>") // Replace with the actual table name
                    .returnTokens(true) // Set to true to return tokenized values
                    .build();

            // Send the request to the Skyflow vault and retrieve the tokenized records
            GetResponse getTokensResponse = skyflowClient.vault("<VAULT_ID>").get(getTokensRequest); // Replace with actual Vault ID
            System.out.println(getTokensResponse);

            // Step 4: Create a GetRequest to retrieve records based on specific column values
            ArrayList<String> columnValues = new ArrayList<>();
            columnValues.add("<COLUMN_VALUE_1>"); // Replace with the actual column value
            columnValues.add("<COLUMN_VALUE_2>"); // Replace with the actual column value

            GetRequest getByColumnRequest = GetRequest.builder()
                    .table("<TABLE_NAME>") // Replace with the actual table name
                    .columnName("<COLUMN_NAME>") // Replace with the column name
                    .columnValues(columnValues) // Add the list of column values to filter by
                    .redactionType(RedactionType.PLAIN_TEXT) // Redact data as plain text
                    .build();

            // Send the request to the Skyflow vault and retrieve the records filtered by column values
            GetResponse getByColumnResponse = skyflowClient.vault("<VAULT_ID>").get(getByColumnRequest); // Replace with actual Vault ID
            System.out.println(getByColumnResponse);
        } catch (SkyflowException e) {
            // Step 5: Handle any errors that occur during the retrieval process
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception for debugging purposes
        }
    }
}
```

### Get by skyflow IDs

Retrieve specific records using `skyflow_ids`. Ideal for fetching exact records when IDs are known.

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/GetExample.java) of a get call to retrieve data using Redaction type:

```java
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to retrieve data from the Skyflow vault using a list of Skyflow IDs.
 *
 * 1. Initializes the Skyflow client with a given vault ID.
 * 2. Creates a request to retrieve records based on Skyflow IDs.
 * 3. Specifies that the response should not return tokens.
 * 4. Uses plain text redaction type for the retrieved records.
 * 5. Prints the response to display the retrieved records.
 */
public class GetExample {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Initialize a list of Skyflow IDs (replace with actual Skyflow IDs)
            ArrayList<String> ids = new ArrayList<>();
            ids.add("a581d205-1969-4350-acbe-a2a13eb871a6"); // Replace with actual Skyflow ID
            ids.add("5ff887c3-b334-4294-9acc-70e78ae5164a"); // Replace with actual Skyflow ID

            // Step 2: Create a GetRequest to retrieve records based on Skyflow IDs
            // The request specifies:
            // - `ids`: The list of Skyflow IDs to retrieve
            // - `table`: The table from which the records will be retrieved
            // - `returnTokens`: Set to false, meaning tokens will not be returned in the response
            // - `redactionType`: Set to PLAIN_TEXT, meaning the retrieved records will have data redacted as plain text
            GetRequest getByIdRequest = GetRequest.builder()
                    .ids(ids)
                    .table("table1") // Replace with the actual table name
                    .returnTokens(false) // Set to false to avoid returning tokens
                    .redactionType(RedactionType.PLAIN_TEXT) // Redact data as plain text
                    .build();

            // Step 3: Send the request to the Skyflow vault and retrieve the records
            GetResponse getByIdResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").get(getByIdRequest); // Replace with actual Vault ID
            System.out.println(getByIdResponse); // Print the response to the console

        } catch (SkyflowException e) {
            // Step 4: Handle any errors that occur during the data retrieval process
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception for debugging purposes
        }
    }
}
```

Sample response:

```json
{
  "data": [
    {
      "card_number": "4555555555555553",
      "email": "john.doe@gmail.com",
      "name": "john doe",
      "skyflow_id": "a581d205-1969-4350-acbe-a2a13eb871a6"
    },
    {
      "card_number": "4555555555555559",
      "email": "jane.doe@gmail.com",
      "name": "jane doe",
      "skyflow_id": "5ff887c3-b334-4294-9acc-70e78ae5164a"
    }
  ],
  "errors": []
}
```

### Get tokens

Return tokens for records. Ideal for securely processing sensitive data while maintaining data privacy.

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/getExample.java) of get call to retrieve tokens using Skyflow IDs:

```java
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to retrieve data from the Skyflow vault and return tokens along with the records.
 *
 * 1. Initializes the Skyflow client with a given vault ID.
 * 2. Creates a request to retrieve records based on Skyflow IDs and ensures tokens are returned.
 * 3. Prints the response to display the retrieved records along with the tokens.
 */
public class GetExample {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Initialize a list of Skyflow IDs (replace with actual Skyflow IDs)
            ArrayList<String> ids = new ArrayList<>();
            ids.add("a581d205-1969-4350-acbe-a2a13eb871a6"); // Replace with actual Skyflow ID
            ids.add("5ff887c3-b334-4294-9acc-70e78ae5164a"); // Replace with actual Skyflow ID

            // Step 2: Create a GetRequest to retrieve records based on Skyflow IDs
            // The request specifies:
            // - `ids`: The list of Skyflow IDs to retrieve
            // - `table`: The table from which the records will be retrieved
            // - `returnTokens`: Set to true, meaning tokens will be included in the response
            GetRequest getTokensRequest = GetRequest.builder()
                    .ids(ids)
                    .table("table1") // Replace with the actual table name
                    .returnTokens(true) // Set to true to include tokens in the response
                    .build();

            // Step 3: Send the request to the Skyflow vault and retrieve the records with tokens
            GetResponse getTokensResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").get(getTokensRequest); // Replace with actual Vault ID
            System.out.println(getTokensResponse); // Print the response to the console

        } catch (SkyflowException e) {
            // Step 4: Handle any errors that occur during the data retrieval process
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception for debugging purposes
        }
    }
}
```

Sample response:

```json
{
  "data": [
    {
      "card_number": "3998-2139-0328-0697",
      "email": "c9a6c9555060@82c092e7.bd52",
      "name": "82c092e7-74c0-4e60-bd52-c9a6c9555060",
      "skyflow_id": "a581d205-1969-4350-acbe-a2a13eb871a6"
    },
    {
      "card_number": "3562-0140-8820-7499",
      "email": "6174366e2bc6@59f82e89.93fc",
      "name": "59f82e89-138e-4f9b-93fc-6174366e2bc6",
      "skyflow_id": "5ff887c3-b334-4294-9acc-70e78ae5164a"
    }
  ],
  "errors": []
}
```

### Get By column name and column values

Retrieve records by unique column values. Ideal for querying data without knowing Skyflow IDs, using alternate unique identifiers.

An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/GetExample.java) of get call to retrieve data using column name and column values:

```java
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to retrieve data from the Skyflow vault based on column values.
 *
 * 1. Initializes the Skyflow client with a given vault ID.
 * 2. Creates a request to retrieve records based on specific column values (e.g., email addresses).
 * 3. Prints the response to display the retrieved records after redacting sensitive data based on the specified redaction type.
 */
public class GetExample {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Initialize a list of column values (email addresses in this case)
            ArrayList<String> columnValues = new ArrayList<>();
            columnValues.add("john.doe@gmail.com"); // Example email address
            columnValues.add("jane.doe@gmail.com"); // Example email address

            // Step 2: Create a GetRequest to retrieve records based on column values
            // The request specifies:
            // - `table`: The table from which the records will be retrieved
            // - `columnName`: The column to filter the records by (e.g., "email")
            // - `columnValues`: The list of values to match in the specified column
            // - `redactionType`: Defines how sensitive data should be redacted (set to PLAIN_TEXT here)
            GetRequest getByColumnRequest = GetRequest.builder()
                    .table("table1") // Replace with the actual table name
                    .columnName("email") // The column name to filter by (e.g., "email")
                    .columnValues(columnValues) // The list of column values to match
                    .redactionType(RedactionType.PLAIN_TEXT) // Set the redaction type (e.g., PLAIN_TEXT)
                    .build();

            // Step 3: Send the request to the Skyflow vault and retrieve the records
            GetResponse getByColumnResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").get(getByColumnRequest); // Replace with actual Vault ID
            System.out.println(getByColumnResponse); // Print the response to the console

        } catch (SkyflowException e) {
            // Step 4: Handle any errors that occur during the data retrieval process
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception for debugging purposes
        }
    }
}
```

Sample response:

```json
{
  "data": [
    {
      "card_number": "4555555555555553",
      "email": "john.doe@gmail.com",
      "name": "john doe",
      "skyflow_id": "a581d205-1969-4350-acbe-a2a13eb871a6"
    },
    {
      "card_number": "4555555555555559",
      "email": "jane.doe@gmail.com",
      "name": "jane doe",
      "skyflow_id": "5ff887c3-b334-4294-9acc-70e78ae5164a"
    }
  ],
  "errors": []
}
```

### Redaction types

Redaction types determine how sensitive data is displayed when retrieved from the vault.

#### **Available Redaction Types**

- `DEFAULT`: Applies the vault-configured default redaction setting.
- `REDACTED`: Completely removes sensitive data from view.
- `MASKED`: Partially obscures sensitive information.
- `PLAIN_TEXT`: Displays the full, unmasked data.

#### **Choosing the Right Redaction Type**

- Use `REDACTED` for scenarios requiring maximum data protection to prevent exposure of sensitive information.
- Use `MASKED` to provide partial visibility of sensitive data for less critical use cases.
- Use `PLAIN_TEXT` for internal, authorized access where full data visibility is necessary.

## Update

To update data in your vault, use the `update` method. The `UpdateRequest` class is used to create an update request,
where you specify parameters such as the table name, data (as a map of key value pairs), tokens, returnTokens, and
tokenStrict. If `returnTokens` is set to `true`, Skyflow returns tokens for the updated records. If `returnTokens` is
set to `false`, Skyflow returns IDs for the updated records.

### Construct an update request

```java
import com.skyflow.enums.TokenMode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateResponse;

import java.util.HashMap;

/**
 * This example demonstrates how to update records in the Skyflow vault by providing new data and/or tokenized values, along with corresponding UpdateRequest schema.
 *
 */
public class UpdateSchema {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Prepare the data to update in the vault
            // Use a HashMap to store the data that will be updated in the specified table
            HashMap<String, Object> data = new HashMap<>();
            data.put("skyflow_id", "<SKYFLOW_ID>"); // Skyflow ID for identifying the record to update
            data.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>"); // Example of a column name and its value to update
            data.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>"); // Another example of a column name and its value to update

            // Step 2: Prepare the tokens (if necessary) for certain columns that require tokenization
            // Use a HashMap to specify columns that need tokens in the update request
            HashMap<String, Object> tokens = new HashMap<>();
            tokens.put("<COLUMN_NAME_2>", "<TOKEN_VALUE_2>"); // Example of a column name that should be tokenized

            // Step 3: Create an UpdateRequest to specify the update operation
            // The request includes the table name, token mode, data, tokens, and the returnTokens flag
            UpdateRequest updateRequest = UpdateRequest.builder()
                    .table("<TABLE_NAME>") // Replace with the actual table name to update
                    .tokenMode(TokenMode.ENABLE) // Specifies the tokenization mode (ENABLE means tokenization is applied)
                    .data(data) // The data to update in the record
                    .tokens(tokens) // The tokens associated with specific columns
                    .returnTokens(true) // Specify whether to return tokens in the response
                    .build();

            // Step 4: Send the request to the Skyflow vault and update the record
            UpdateResponse updateResponse = skyflowClient.vault("<VAULT_ID>").update(updateRequest); // Replace with actual Vault ID
            System.out.println(updateResponse); // Print the response to confirm the update result

        } catch (SkyflowException e) {
            // Step 5: Handle any errors that occur during the update operation
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception for debugging purposes
        }
    }
}
```

### An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/UpdateExample.java) of update call

```java
import com.skyflow.enums.TokenMode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateResponse;

import java.util.HashMap;

/**
 * This example demonstrates how to update a record in the Skyflow vault with specified data and tokens.
 *
 * 1. Initializes the Skyflow client with a given vault ID.
 * 2. Constructs an update request with data to modify and tokens to include.
 * 3. Sends the request to update the record in the vault.
 * 4. Prints the response to confirm the success or failure of the update operation.
 */
public class UpdateExample {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Prepare the data to update in the vault
            // A HashMap is used to store the data that will be updated in the specified table
            HashMap<String, Object> data = new HashMap<>();
            data.put("skyflow_id", "5b699e2c-4301-4f9f-bcff-0a8fd3057413"); // Skyflow ID identifies the record to update
            data.put("name", "john doe"); // Updating the "name" column with a new value
            data.put("card_number", "4111111111111115"); // Updating the "card_number" column with a new value

            // Step 2: Prepare the tokens to include in the update request
            // Tokens can be included to update sensitive data with tokenized values
            HashMap<String, Object> tokens = new HashMap<>();
            tokens.put("name", "72b8ffe3-c8d3-4b4f-8052-38b2a7405b5a"); // Tokenized value for the "name" column

            // Step 3: Create an UpdateRequest to define the update operation
            // The request specifies the table name, token mode, data, and tokens for the update
            UpdateRequest updateRequest = UpdateRequest.builder()
                    .table("table1") // Replace with the actual table name to update
                    .tokenMode(TokenMode.ENABLE) // Token mode enabled to allow tokenization of sensitive data
                    .data(data) // The data to update in the record
                    .tokens(tokens) // The tokenized values for sensitive columns
                    .build();

            // Step 4: Send the update request to the Skyflow vault
            UpdateResponse updateResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").update(updateRequest); // Replace with your actual Vault ID
            System.out.println(updateResponse); // Print the response to confirm the update result

        } catch (SkyflowException e) {
            // Step 5: Handle any exceptions that occur during the update operation
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception stack trace for debugging purposes
        }
    }
}
```

Sample response:

- When `returnTokens` is set to `true`

```json
{
  "skyflowId": "5b699e2c-4301-4f9f-bcff-0a8fd3057413",
  "name": "72b8ffe3-c8d3-4b4f-8052-38b2a7405b5a",
  "card_number": "4315-7650-1359-9681"
}
```

- When `returnTokens` is set to `false`

```json
{
  "skyflowId": "5b699e2c-4301-4f9f-bcff-0a8fd3057413"
}
```

## Delete

To delete records using Skyflow IDs, use the `delete` method. The `DeleteRequest` class accepts a list of Skyflow IDs
that you want to delete, as shown below:

### Construct a delete request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to delete records from a Skyflow vault using specified Skyflow IDs, along with corresponding DeleteRequest schema.
 *
 */
public class DeleteSchema {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Prepare a list of Skyflow IDs for the records to delete
            // The list stores the Skyflow IDs of the records that need to be deleted from the vault
            ArrayList<String> ids = new ArrayList<>();
            ids.add("<SKYFLOW_ID_1>"); // Replace with actual Skyflow ID 1
            ids.add("<SKYFLOW_ID_2>"); // Replace with actual Skyflow ID 2
            ids.add("<SKYFLOW_ID_3>"); // Replace with actual Skyflow ID 3

            // Step 2: Create a DeleteRequest to define the delete operation
            // The request specifies the table from which to delete the records and the IDs of the records to delete
            DeleteRequest deleteRequest = DeleteRequest.builder()
                    .ids(ids) // List of Skyflow IDs to delete
                    .table("<TABLE_NAME>") // Replace with the actual table name from which to delete
                    .build();

            // Step 3: Send the delete request to the Skyflow vault
            DeleteResponse deleteResponse = skyflowClient.vault("<VAULT_ID>").delete(deleteRequest); // Replace with your actual Vault ID
            System.out.println(deleteResponse); // Print the response to confirm the delete result

        } catch (SkyflowException e) {
            // Step 4: Handle any exceptions that occur during the delete operation
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception stack trace for debugging purposes
        }
    }
}
```

### An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/DeleteExample.java) of delete call:

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;

import java.util.ArrayList;

/**
 * This example demonstrates how to delete records from a Skyflow vault using specified Skyflow IDs.
 *
 * 1. Initializes the Skyflow client with a given Vault ID.
 * 2. Constructs a delete request by specifying the IDs of the records to delete.
 * 3. Sends the delete request to the Skyflow vault to delete the specified records.
 * 4. Prints the response to confirm the success or failure of the delete operation.
 */
public class DeleteExample {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Prepare a list of Skyflow IDs for the records to delete
            // The list stores the Skyflow IDs of the records that need to be deleted from the vault
            ArrayList<String> ids = new ArrayList<>();
            ids.add("9cbf66df-6357-48f3-b77b-0f1acbb69280"); // Replace with actual Skyflow ID 1
            ids.add("ea74bef4-f27e-46fe-b6a0-a28e91b4477b"); // Replace with actual Skyflow ID 2
            ids.add("47700796-6d3b-4b54-9153-3973e281cafb"); // Replace with actual Skyflow ID 3

            // Step 2: Create a DeleteRequest to define the delete operation
            // The request specifies the table from which to delete the records and the IDs of the records to delete
            DeleteRequest deleteRequest = DeleteRequest.builder()
                    .ids(ids) // List of Skyflow IDs to delete
                    .table("table1") // Replace with the actual table name from which to delete
                    .build();

            // Step 3: Send the delete request to the Skyflow vault
            DeleteResponse deleteResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").delete(deleteRequest); // Replace with your actual Vault ID
            System.out.println(deleteResponse); // Print the response to confirm the delete result

        } catch (SkyflowException e) {
            // Step 4: Handle any exceptions that occur during the delete operation
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception stack trace for debugging purposes
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

## Query

To retrieve data with SQL queries, use the `query` method. The `QueryRequest` class accepts a `query` parameter, as shown below.

### Construct a query request

Refer to [Query your data](https://docs.skyflow.com/query-data/) and [Execute Query](https://docs.skyflow.com/record/#QueryService_ExecuteQuery) for guidelines and restrictions on supported SQL statements, operators, and keywords.

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;

/**
 * This example demonstrates how to execute a custom SQL query on a Skyflow vault, along with QueryRequest schema.
 *
 */
public class QuerySchema {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Define the SQL query to execute on the Skyflow vault
            // Replace "<YOUR_SQL_QUERY>" with the actual SQL query you want to run
            String query = "<YOUR_SQL_QUERY>"; // Example: "SELECT * FROM table1 WHERE column1 = 'value'"

            // Step 2: Create a QueryRequest with the specified SQL query
            QueryRequest queryRequest = QueryRequest.builder()
                    .query(query) // SQL query to execute
                    .build();

            // Step 3: Execute the query request on the specified Skyflow vault
            QueryResponse queryResponse = skyflowClient.vault("<VAULT_ID>").query(queryRequest); // Replace <VAULT_ID> with your actual Vault ID
            System.out.println(queryResponse); // Print the response containing the query results

        } catch (SkyflowException e) {
            // Step 4: Handle any exceptions that occur during the query execution
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception stack trace for debugging
        }
    }
}
```

### An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/vault/QueryExample.java) of query call

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;

/**
 * This example demonstrates how to execute a SQL query on a Skyflow vault to retrieve data.
 *
 * 1. Initializes the Skyflow client with the Vault ID.
 * 2. Constructs a query request with a specified SQL query.
 * 3. Executes the query against the Skyflow vault.
 * 4. Prints the response from the query execution.
 */
public class QueryExample {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Define the SQL query
            // Example query: Retrieve all records from the "cards" table with a specific skyflow_id
            String query = "SELECT * FROM cards WHERE skyflow_id='3ea3861-x107-40w8-la98-106sp08ea83f'";

            // Step 2: Create a QueryRequest with the SQL query
            QueryRequest queryRequest = QueryRequest.builder()
                    .query(query) // SQL query to execute
                    .build();

            // Step 3: Execute the query request on the specified Skyflow vault
            QueryResponse queryResponse = skyflowClient.vault("9f27764a10f7946fe56b3258e117").query(queryRequest); // Vault ID: 9f27764a10f7946fe56b3258e117
            System.out.println(queryResponse); // Print the query response (contains query results)

        } catch (SkyflowException e) {
            // Step 4: Handle any exceptions that occur during the query execution
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception stack trace for debugging
        }
    }
}
```

Sample response:

```json
{
  "fields": [
    {
      "card_number": "XXXXXXXXXXXX1112",
      "name": "S***ar",
      "skyflow_id": "3ea3861-x107-40w8-la98-106sp08ea83f",
      "tokenizedData": null
    }
  ]
}
```

# Detect
Skyflow Detect enables you to deidentify and reidentify sensitive data in text and files, supporting advanced privacy-preserving workflows. The Detect API supports the following operations:

## Deidentify Text
To deidentify text, use the `deidentifyText` method. The `DeidentifyTextRequest` class creates a deidentify text request, which includes the text to be deidentified. Additionally, you can provide optional parameters using the `DeidentifyTextOptions` class.

### Construct an deidentify text request

```java
import com.skyflow.enums.DetectEntities;
import com.skyflow.vault.detect.DateTransformation;
import com.skyflow.vault.detect.DeidentifyTextRequest;
import com.skyflow.vault.detect.TokenFormat;
import com.skyflow.vault.detect.Transformations;
import com.skyflow.vault.detect.DeidentifyTextResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * This example demonstrate to build deidentify text request.
 */
public class DeidentifyTextSchema {

    public static void main(String[] args) {

        // Step 1: Initialise the Skyflow client by configuring the credentials & vault config.

        // Step 2: Configure the options for deidentify text
        
        // Replace with the entity you want to detect
        List<DetectEntities> detectEntitiesList = new ArrayList<>();
        detectEntitiesList.add(DetectEntities.SSN);

        // Replace with the entity you want to detect with vault token
        List<DetectEntities> vaultTokenList = new ArrayList<>();
        vaultTokenList.add(DetectEntities.CREDIT_CARD);

        // Replace with the entity you want to detect with entity only
        List<DetectEntities> entityOnlyList = new ArrayList<>();
        entityOnlyList.add(DetectEntities.SSN);

        // Replace with the entity you want to detect with entity unique counter
        List<DetectEntities> entityUniqueCounterList = new ArrayList<>();
        entityUniqueCounterList.add(DetectEntities.SSN);

        // Replace with the regex patterns you want to allow during deidentification
        List<String> allowRegexList = new ArrayList<>();
        allowRegexList.add("<YOUR_ALLOW_REGEX_LIST>");

        // Replace with the regex patterns you want to restrict during deidentification
        List<String> restrictRegexList = new ArrayList<>();
        restrictRegexList.add("YOUR_RESTRICT_REGEX_LIST");

        // Configure Token Format
        TokenFormat tokenFormat = TokenFormat.builder()
                .vaultToken(vaultTokenList)
                .entityOnly(entityOnlyList)
                .entityUniqueCounter(entityUniqueCounterList)
                .build();

        // Configure Transformation
        List<DetectEntities> detectEntitiesTransformationList = new ArrayList<>();
        detectEntitiesTransformationList.add(DetectEntities.DOB); // Replace with the entity you want to transform

        DateTransformation dateTransformation = new DateTransformation(20, 5, detectEntitiesTransformationList);
        Transformations transformations = new Transformations(dateTransformation);

        // Step 3: Create a deidentify text request for the vault
        DeidentifyTextRequest deidentifyTextRequest = DeidentifyTextRequest.builder()
                .text("<SENSITIVE_TEXT>") // Replace with the text you want to deidentify
                .entities(detectEntitiesList)
                .allowRegexList(allowRegexList)
                .restrictRegexList(restrictRegexList)
                .tokenFormat(tokenFormat)
                .transformations(transformations)
                .build();

        // Step 4: Use the Skyflow client to perform the deidentifyText operation
        // Replace <VAULT_ID> with your actual vault ID
        DeidentifyTextResponse deidentifyTextResponse = skyflowClient.detect("<VAULT_ID>").deidentifyText(deidentifyTextRequest);

        // Step 5: Print the response
        System.out.println("Deidentify text Response: " + deidentifyTextResponse);
    }
}

```

## An [example](https://github.com/skyflowapi/skyflow-java/blob/beta-release/25.6.2/samples/src/main/java/com/example/detect/DeidentifyTextExample.java) of deidentify text:
```java
import java.util.ArrayList;
import java.util.List;

import com.skyflow.enums.DetectEntities;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.detect.DateTransformation;
import com.skyflow.vault.detect.DeidentifyTextRequest;
import com.skyflow.vault.detect.DeidentifyTextResponse;
import com.skyflow.vault.detect.TokenFormat;
import com.skyflow.vault.detect.Transformations;

/**
 * Skyflow Deidentify Text Example
 * <p>
 * This example demonstrates how to use the Skyflow SDK to deidentify text data
 * across multiple vaults. It includes:
 * 1. Setting up credentials and vault configurations.
 * 2. Creating a Skyflow client with multiple vaults.
 * 3. Performing deidentify of text with various options.
 * 4. Handling responses and errors.
 */

public class DeidentifyTextExample {
    public static void main(String[] args) throws SkyflowException {

        // Step 1: Initialise the Skyflow client by configuring the credentials & vault config.

        // Step 2: Configuring the different options for deidentify

        // Replace with the entity you want to detect
        List<DetectEntities> detectEntitiesList = new ArrayList<>();
        detectEntitiesList.add(DetectEntities.SSN);
        detectEntitiesList.add(DetectEntities.CREDIT_CARD);

        // Replace with the entity you want to detect with vault token
        List<DetectEntities> vaultTokenList = new ArrayList<>();
        vaultTokenList.add(DetectEntities.SSN);
        vaultTokenList.add(DetectEntities.CREDIT_CARD);

        // Configure Token Format
        TokenFormat tokenFormat = TokenFormat.builder()
                .vaultToken(vaultTokenList)
                .build();

        // Configure Transformation for deidentified entities
        List<DetectEntities> detectEntitiesTransformationList = new ArrayList<>();
        detectEntitiesTransformationList.add(DetectEntities.DOB); // Replace with the entity you want to transform

        DateTransformation dateTransformation = new DateTransformation(20, 5, detectEntitiesTransformationList);
        Transformations transformations = new Transformations(dateTransformation);

        // Step 3: invoking Deidentify text on the vault
        try {
            // Create a deidentify text request for the vault
            DeidentifyTextRequest deidentifyTextRequest = DeidentifyTextRequest.builder()
                    .text("My SSN is 123-45-6789 and my card is 4111 1111 1111 1111.") // Replace with your deidentify text
                    .entities(detectEntitiesList)
                    .tokenFormat(tokenFormat)
                    .transformations(transformations)
                    .build();
            // Replace `9f27764a10f7946fe56b3258e117` with the acutal vault id
            DeidentifyTextResponse deidentifyTextResponse = skyflowClient.detect("9f27764a10f7946fe56b3258e117").deidentifyText(deidentifyTextRequest);

            System.out.println("Deidentify text Response: " + deidentifyTextResponse);
        } catch (SkyflowException e) {
            System.err.println("Error occurred during deidentify: ");
            e.printStackTrace(); // Print the exception for debugging purposes
        }
    }
}
```

Sample Response:
```json
{
  "processedText": "My SSN is [SSN_IWdexZe] and my card is [CREDIT_CARD_rUzMjdQ].",
  "entities": [
    {
      "token": "SSN_IWdexZe",
      "value": "123-45-6789",
      "textIndex": {
        "start": 10,
        "end": 21
      },
      "processedIndex": {
        "start": 10,
        "end": 23
      },
      "entity": "SSN",
      "scores": {
        "SSN": 0.9384
      }
    },
    {
      "token": "CREDIT_CARD_rUzMjdQ",
      "value": "4111 1111 1111 1111",
      "textIndex": {
        "start": 37,
        "end": 56
      },
      "processedIndex": {
        "start": 39,
        "end": 60
      },
      "entity": "CREDIT_CARD",
      "scores": {
        "CREDIT_CARD": 0.9051
      }
    }
  ],
  "wordCount": 9,
  "charCount": 57
}
```

## Reidentify Text
To reidentify text, use the `reidentifyText` method. The `ReidentifyTextRequest` class creates a reidentify text request, which includes the redacted or deidentified text to be reidentified. Additionally, you can provide optional parameters using the ReidentifyTextOptions class to control how specific entities are returned (as redacted, masked, or plain text).

### Construct an reidentify text request

```java
import com.skyflow.enums.DetectEntities;
import com.skyflow.vault.detect.ReidentifyTextRequest;
import com.skyflow.vault.detect.ReidentifyTextResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * This example demonstrates how to build a reidentify text request.
 */
public class ReidentifyTextSchema {
    public static void main(String[] args) {
        // Step 1: Initialise the Skyflow client by configuring the credentials & vault config.

        // Step 2: Configuring the different options for reidentify
        List<DetectEntities> maskedEntity = new ArrayList<>();
        maskedEntity.add(DetectEntities.CREDIT_CARD); // Replace with the entity you want to mask

        List<DetectEntities> plainTextEntity = new ArrayList<>();
        plainTextEntity.add(DetectEntities.SSN); // Replace with the entity you want to keep in plain text

        // List<DetectEntities> redactedEntity = new ArrayList<>();
        // redactedEntity.add(DetectEntities.SSN); // Replace with the entity you want to redact


        // Step 3: Create a reidentify text request with the configured entities
        ReidentifyTextRequest reidentifyTextRequest = ReidentifyTextRequest.builder()
                .text("My SSN is [SSN_IWdexZe] and my card is [CREDIT_CARD_rUzMjdQ].") // Replace with your deidentify text
                .maskedEntities(maskedEntity)
//                .redactedEntities(redactedEntity)
                .plainTextEntities(plainTextEntity)
                .build();

        // Step 4: Invoke reidentify text on the vault
        ReidentifyTextResponse reidentifyTextResponse = skyflowClient.detect("<VAULT_ID>").reidentifyText(reidentifyTextRequest);
        System.out.println("Reidentify text Response: " + reidentifyTextResponse);
    }
}
```

## An [example](https://github.com/skyflowapi/skyflow-java/blob/beta-release/25.6.2/samples/src/main/java/com/example/detect/ReidentifyTextExample.java) of Reidentify text

```java
import com.skyflow.enums.DetectEntities;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.detect.ReidentifyTextRequest;
import com.skyflow.vault.detect.ReidentifyTextResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Skyflow Reidentify Text Example
 * <p>
 * This example demonstrates how to use the Skyflow SDK to reidentify text data
 * across multiple vaults. It includes:
 * 1. Setting up credentials and vault configurations.
 * 2. Creating a Skyflow client with multiple vaults.
 * 3. Performing reidentify of text with various options.
 * 4. Handling responses and errors.
 */

public class ReidentifyTextExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Initialise the Skyflow client by configuring the credentials & vault config.

        // Step 2: Configuring the different options for reidentify
        List<DetectEntities> maskedEntity = new ArrayList<>();
        maskedEntity.add(DetectEntities.CREDIT_CARD); // Replace with the entity you want to mask

        List<DetectEntities> plainTextEntity = new ArrayList<>();
        plainTextEntity.add(DetectEntities.SSN); // Replace with the entity you want to keep in plain text

        try {
            // Step 3: Create a reidentify text request with the configured options
            ReidentifyTextRequest reidentifyTextRequest = ReidentifyTextRequest.builder()
                    .text("My SSN is [SSN_IWdexZe] and my card is [CREDIT_CARD_rUzMjdQ].") // Replace with your deidentify text
                    .maskedEntities(maskedEntity)
                    .plainTextEntities(plainTextEntity)
                    .build();

            // Step 4: Invoke Reidentify text on the vault
            // Replace `9f27764a10f7946fe56b3258e117` with the acutal vault id
            ReidentifyTextResponse reidentifyTextResponse = skyflowClient.detect("9f27764a10f7946fe56b3258e117").reidentifyText(reidentifyTextRequest);

            // Handle the response from the reidentify text request
            System.out.println("Reidentify text Response: " + reidentifyTextResponse);
        } catch (SkyflowException e) {
            System.err.println("Error occurred during reidentify : ");
            e.printStackTrace();
        }
    }
}
```

Sample Response: 

```json
{
  "processedText":"My SSN is 123-45-6789 and my card is XXXXX1111."
}
```

## Deidentify file
To deidentify files, use the `deidentifyFile` method. The `DeidentifyFileRequest` class creates a deidentify file request, which includes the file to be deidentified (such as images, PDFs, audio, documents, spreadsheets, or presentations). Additionally, you can provide optional parameters using the DeidentifyFileOptions class to control how entities are detected and deidentified, as well as how the output is generated for different file types.

### Construct an deidentify file request

```java
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.MaskingMethod;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.detect.DeidentifyFileRequest;
import com.skyflow.vault.detect.DeidentifyFileResponse;

import java.io.File;

/**
 * This example demonstrates how to build a deidentify file request.
 */

public class DeidentifyFileSchema {

    public static void main(String[] args) {
        // Step 1: Initialise the Skyflow client by configuring the credentials & vault config.

        // Step 2: Create a deidentify file request with all options

        // Create file object
        File file = new File("<FILE_PATH>"); // Replace with the path to the file you want to deidentify

        // Create file input using the file object
        FileInput fileInput = FileInput.builder()
                .file(file)
                .build();

        // Output configuration
        String outputDirectory = "<OUTPUT_DIRECTORY>"; // Replace with the desired output directory to save the deidentified file

        // Entities to detect
        // List<DetectEntities> detectEntities = new ArrayList<>();
        // detectEntities.add(DetectEntities.IP_ADDRESS); // Replace with the entities you want to detect

        // Image-specific options
        // Boolean outputProcessedImage = true; // Include processed image in output
        // Boolean outputOcrText = true; // Include OCR text in output
        MaskingMethod maskingMethod = MaskingMethod.BLACKBOX; // Masking method for images

        // PDF-specific options
        // Integer pixelDensity = 15; //  Pixel density for PDF processing
        // Integer maxResolution = 2000; // Max resolution for PDF

        // Audio-specific options
        // Boolean outputProcessedAudio = true; // Include processed audio
        // DetectOutputTranscriptions outputTanscription = DetectOutputTranscriptions.PLAINTEXT_TRANSCRIPTION;  // Transcription type

        // Audio bleep configuration
        // AudioBleep audioBleep = AudioBleep.builder()
        //         .frequency(5D) // Pitch in Hz
        //         .startPadding(7D) // Padding at start (seconds)
        //         .stopPadding(8D) // Padding at end (seconds)
        //         .build();

        Integer waitTime = 20; // Max wait time for response (max 64 seconds)

        DeidentifyFileRequest deidentifyFileRequest = DeidentifyFileRequest.builder()
                .file(fileInput)
                .waitTime(waitTime)
                .entities(detectEntities)
                .outputDirectory(outputDirectory)
                .maskingMethod(maskingMethod)
                // .outputProcessedImage(outputProcessedImage)
                // .outputOcrText(outputOcrText)
                // .pixelDensity(pixelDensity)
                // .maxResolution(maxResolution)
                // .outputProcessedAudio(outputProcessedAudio)
                // .outputTranscription(outputTanscription)
                // .bleep(audioBleep)
                .build();


        DeidentifyFileResponse deidentifyFileResponse = skyflowClient.detect("<VAULT_ID>").deidentifyFile(deidentifyFileRequest);
        System.out.println("Deidentify file response: " + deidentifyFileResponse.toString());
    }
} 
```

## An [example](https://github.com/skyflowapi/skyflow-java/blob/beta-release/25.6.2/samples/src/main/java/com/example/detect/DeidentifyFileExample.java) of Deidentify file

```java
import java.io.File;

import com.skyflow.enums.MaskingMethod;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.detect.DeidentifyFileRequest;
import com.skyflow.vault.detect.DeidentifyFileResponse;

/**
 * Skyflow Deidentify File Example
 * <p>
 * This example demonstrates how to use the Skyflow SDK to deidentify file
 * It has all available options for deidentifying files.
 * Supported file types: images (jpg, png, etc.), pdf, audio (mp3, wav), documents, spreadsheets, presentations, structured text.
 * It includes:
 * 1. Configure credentials
 * 2. Set up vault configuration
 * 3. Create a deidentify file request with all options
 * 4. Call deidentifyFile to deidentify file.
 * 5. Handle response and errors
 */
public class DeidentifyFileExample {

    public static void main(String[] args) throws SkyflowException {
        // Step 1: Initialise the Skyflow client by configuring the credentials & vault config.
        try {
            // Step 2: Create a deidentify file request with all options


            // Create file object
            File file = new File("sensitive-folder/personal-info.txt"); // Replace with the path to the file you want to deidentify

            // Create file input using the file object
            FileInput fileInput = FileInput.builder()
                    .file(file)
                    .build();

            // Output configuration
            String outputDirectory = "deidenfied-file/"; // Replace with the desired output directory to save the deidentified file

            // Entities to detect
            // List<DetectEntities> detectEntities = new ArrayList<>();
            // detectEntities.add(DetectEntities.IP_ADDRESS); // Replace with the entities you want to detect

            // Image-specific options
            // Boolean outputProcessedImage = true; // Include processed image in output
            // Boolean outputOcrText = true; // Include OCR text in output
            MaskingMethod maskingMethod = MaskingMethod.BLACKBOX; // Masking method for images         

            Integer waitTime = 20; // Max wait time for response (max 64 seconds)

            DeidentifyFileRequest deidentifyFileRequest = DeidentifyFileRequest.builder()
                    .file(fileInput)
                    .waitTime(waitTime)
                    .outputDirectory(outputDirectory)
                    .maskingMethod(maskingMethod)
                    .build();

            // Step 3: Invoking deidentifyFile 
            // Replace `9f27764a10f7946fe56b3258e117` with the acutal vault id
            DeidentifyFileResponse deidentifyFileResponse = skyflowClient.detect("9f27764a10f7946fe56b3258e117").deidentifyFile(deidentifyFileRequest);
            System.out.println("Deidentify file response: " + deidentifyFileResponse.toString());
        } catch (SkyflowException e) {
            System.err.println("Error occurred during deidentify file: ");
            e.printStackTrace();
        }
    }
}

```

Sample response: 

```json
{
  "file": {
    "name": "deidentified.txt",
    "size": 33,
    "type": "",
    "lastModified": 1751355183039
  },
  "fileBase64": "bXkgY2FyZCBudW1iZXIgaXMgW0NSRURJVF",
  "type": "redacted_file",
  "extension": "txt",
  "wordCount": 11,
  "charCount": 61,
  "sizeInKb": 0,
  "entities": [
    {
      "file": "bmFtZTogW05BTUVfMV0gCm==",
      "type": "entities",
      "extension": "json"
    }
  ],
  "runId": "undefined",
  "status": "success"
}

```

**Supported file types:**  
- Documents: `doc`, `docx`, `pdf`
- PDFs: `pdf`
- Images: `bmp`, `jpeg`, `jpg`, `png`, `tif`, `tiff`
- Structured text: `json`, `xml`
- Spreadsheets: `csv`, `xls`, `xlsx`
- Presentations: `ppt`, `pptx`
- Audio: `mp3`, `wav`

**Note:** 
- Transformations cannot be applied to Documents, Images, or PDFs file formats.

- The `waitTime` option must be ≤ 64 seconds; otherwise, an error is thrown.

- If the API takes more than 64 seconds to process the file, it will return only the run ID in the response.

Sample response (when the API takes more than 64 seconds):
```json
{
  "file": null,
  "fileBase64": null,
  "type": null,
  "extension": null,
  "wordCount": null,
  "charCount": null,
  "sizeInKb": null,
  "durationInSeconds": null,
  "pageCount": null,
  "slideCount": null,
  "entities": null,
  "runId": "1273a8c6-c498-4293-a9d6-389864cd3a44",
  "status": "IN_PROGRESS",
  "errors": null
}
```

## Get run:
To retrieve the results of a previously started file `deidentification operation`, use the `getDetectRun` method.
The `GetDetectRunRequest` class is initialized with the `runId` returned from a prior deidentifyFile call.
This method allows you to fetch the final results of the file processing operation once they are available.

### Construct an get run request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.detect.DeidentifyFileResponse;
import com.skyflow.vault.detect.GetDetectRunRequest;

/**
 * Skyflow Get Detect Run Example
 */

public class GetDetectRunSchema {

    public static void main(String[] args) {
        try {
            // Step 1: Initialise the Skyflow client by configuring the credentials & vault config.

            // Step 2: Create a get detect run request
            GetDetectRunRequest getDetectRunRequest = GetDetectRunRequest.builder()
                    .runId("<RUN_ID_FROM_DEIDENTIFY_FILE>") // Replace with the runId from deidentifyFile call
                    .build();

            // Step 3: Call getDetectRun to poll for file processing results
            // Replace <VAULT_ID> with your actual vault ID
            DeidentifyFileResponse deidentifyFileResponse = skyflowClient.detect("<VAULT_ID>").getDetectRun(getDetectRunRequest);
            System.out.println("Get Detect Run Response: " + deidentifyFileResponse);
        } catch (SkyflowException e) {
            System.err.println("Error occurred during get detect run: ");
            e.printStackTrace();
        }
    }
}

```

## An [example](https://github.com/skyflowapi/skyflow-java/blob/beta-release/25.6.2/samples/src/main/java/com/example/detect/GetDetectRunExample.java) of get run
```java
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.detect.DeidentifyFileResponse;
import com.skyflow.vault.detect.GetDetectRunRequest;

/**
 * Skyflow Get Detect Run Example
 * <p>
 * This example demonstrates how to:
 * 1. Configure credentials
 * 2. Set up vault configuration
 * 3. Create a get detect run request
 * 4. Call getDetectRun to poll for file processing results
 * 5. Handle response and errors
 */
public class GetDetectRunExample {
    public static void main(String[] args) throws SkyflowException {
    // Step 1: Initialise the Skyflow client by configuring the credentials & vault config.
        try {

            // Step 2: Create a get detect run request
            GetDetectRunRequest getDetectRunRequest = GetDetectRunRequest.builder()
                    .runId("e0038196-4a20-422b-bad7-e0477117f9bb") // Replace with the runId from deidentifyFile call
                    .build();

            // Step 3: Call getDetectRun to poll for file processing results
            // Replace `9f27764a10f7946fe56b3258e117` with the acutal vault id
            DeidentifyFileResponse deidentifyFileResponse = skyflowClient.detect("9f27764a10f7946fe56b3258e117").getDetectRun(getDetectRunRequest);
            System.out.println("Get Detect Run Response: " + deidentifyFileResponse);
        } catch (SkyflowException e) {
            System.err.println("Error occurred during get detect run: ");
            e.printStackTrace();
        }
    }
}
```

Sample Response:

```json
{
  "file": "bmFtZTogW05BTET0JfMV0K",
  "type": "redacted_file",
  "extension": "txt",
  "wordCount": 11,
  "charCount": 61,
  "sizeInKb": 0.0,
  "entities": [
    {
      "file": "gW05BTUVfMV0gCmNhcmQ0K",
      "type": "entities",
      "extension": "json"
    }
  ],
  "runId": "e0038196-4a20-422b-bad7-e0477117f9bb",
  "status": "success"
}

```

# Connections

Skyflow Connections is a gateway service that uses tokenization to securely send and receive data between your systems and first- or third-party services. The [connections](https://github.com/skyflowapi/skyflow-java/tree/main/src/main/java/com/skyflow/vault/connection) module invokes both inbound and/or outbound connections.

- **Inbound connections**: Act as intermediaries between your client and server, tokenizing sensitive data before it reaches your backend, ensuring downstream services handle only tokenized data.
- **Outbound connections**: Enable secure extraction of data from the vault and transfer it to third-party services via your backend server, such as processing checkout or card issuance flows.

## Invoke a connection

To invoke a connection, use the `invoke` method of the Skyflow client.

### Construct an invoke connection request

```java
import com.skyflow.enums.RequestMethod;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import com.skyflow.vault.connection.InvokeConnectionResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * This example demonstrates how to invoke an external connection using the Skyflow SDK, along with corresponding InvokeConnectionRequest schema.
 *
 */
public class InvokeConnectionSchema {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Define the request body parameters
            // These are the values you want to send in the request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");
            requestBody.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>");

            // Step 2: Define the request headers
            // Add any required headers that need to be sent with the request
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("<HEADER_NAME_1>", "<HEADER_VALUE_1>");
            requestHeaders.put("<HEADER_NAME_2>", "<HEADER_VALUE_2>");

            // Step 3: Define the path parameters
            // Path parameters are part of the URL and typically used in RESTful APIs
            Map<String, String> pathParams = new HashMap<>();
            pathParams.put("<YOUR_PATH_PARAM_KEY_1>", "<YOUR_PATH_PARAM_VALUE_1>");
            pathParams.put("<YOUR_PATH_PARAM_KEY_2>", "<YOUR_PATH_PARAM_VALUE_2>");

            // Step 4: Define the query parameters
            // Query parameters are included in the URL after a '?' and are used to filter or modify the response
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("<YOUR_QUERY_PARAM_KEY_1>", "<YOUR_QUERY_PARAM_VALUE_1>");
            queryParams.put("<YOUR_QUERY_PARAM_KEY_2>", "<YOUR_QUERY_PARAM_VALUE_2>");

            // Step 5: Build the InvokeConnectionRequest using the provided parameters
            InvokeConnectionRequest invokeConnectionRequest = InvokeConnectionRequest.builder()
                    .method(RequestMethod.POST) // The HTTP method to use for the request (POST in this case)
                    .requestBody(requestBody)   // The body of the request
                    .requestHeaders(requestHeaders) // The headers to include in the request
                    .pathParams(pathParams)    // The path parameters for the URL
                    .queryParams(queryParams) // The query parameters to append to the URL
                    .build();

            // Step 6: Invoke the connection using the request
            // Replace "<CONNECTION_ID>" with the actual connection ID you are using
            InvokeConnectionResponse invokeConnectionResponse = skyflowClient.connection("<CONNECTION_ID>").invoke(invokeConnectionRequest);

            // Step 7: Print the response from the invoked connection
            // This response contains the result of the request sent to the external system
            System.out.println(invokeConnectionResponse);

        } catch (SkyflowException e) {
            // Step 8: Handle any exceptions that occur during the connection invocation
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the exception stack trace for debugging
        }
    }
}
```

`method` supports the following methods:

- GET
- POST
- PUT
- PATCH
- DELETE

**pathParams, queryParams, requestHeader, requestBody** are the JSON objects represented as HashMaps, that will be sent through the connection integration url.

### An [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/connection/InvokeConnectionExample.java) of invokeConnection

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

/**
 * This example demonstrates how to invoke an external connection using the Skyflow SDK.
 * It configures a connection, sets up the request, and sends a POST request to the external service.
 *
 * 1. Initialize Skyflow client with connection details.
 * 2. Define the request body, headers, and method.
 * 3. Execute the connection request.
 * 4. Print the response from the invoked connection.
 */
public class InvokeConnectionExample {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Set up credentials and connection configuration
            // Load credentials from a JSON file (you need to provide the correct path)
            Credentials credentials = new Credentials();
            credentials.setPath("/path/to/credentials.json");

            // Define the connection configuration (URL and credentials)
            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setConnectionId("<CONNECTION_ID>");  // Replace with actual connection ID
            connectionConfig.setConnectionUrl("https://connection.url.com"); // Replace with actual connection URL
            connectionConfig.setCredentials(credentials); // Set credentials for the connection

            // Initialize the Skyflow client with the connection configuration
            Skyflow skyflowClient = Skyflow.builder()
                    .setLogLevel(LogLevel.DEBUG)  // Set log level to DEBUG for detailed logs
                    .addConnectionConfig(connectionConfig)  // Add connection configuration to client
                    .build();  // Build the Skyflow client instance

            // Step 2: Define the request body and headers
            // Map for request body parameters
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("card_number", "4337-1696-5866-0865");  // Example card number
            requestBody.put("ssn", "524-41-4248");  // Example SSN

            // Map for request headers
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("Content-Type", "application/json");  // Set content type for the request

            // Step 3: Build the InvokeConnectionRequest with required parameters
            // Set HTTP method to POST, include the request body and headers
            InvokeConnectionRequest invokeConnectionRequest = InvokeConnectionRequest.builder()
                    .method(RequestMethod.POST)  // HTTP POST method
                    .requestBody(requestBody)  // Add request body parameters
                    .requestHeaders(requestHeaders)  // Add headers
                    .build();  // Build the request

            // Step 4: Invoke the connection and capture the response
            // Replace "<CONNECTION_ID>" with the actual connection ID
            InvokeConnectionResponse invokeConnectionResponse = skyflowClient.connection("<CONNECTION_ID>").invoke(invokeConnectionRequest);

            // Step 5: Print the response from the connection invocation
            System.out.println(invokeConnectionResponse);  // Print the response to the console

        } catch (SkyflowException e) {
            // Step 6: Handle any exceptions that occur during the connection invocation
            System.out.println("Error occurred: ");
            e.printStackTrace();  // Print the exception stack trace for debugging
        }
    }
}
```

Sample response:

```json
{
  "data": {
    "card_number": "4337-1696-5866-0865",
    "ssn": "524-41-4248"
  },
  "metadata": {
    "requestId": "4a3453b5-7aa4-4373-98d7-cf102b1f6f97"
  }
}
```

# Authenticate with bearer tokens

This section covers methods for generating and managing tokens to authenticate API calls:

- **Generate a bearer token**:  
  Enable the creation of bearer tokens using service account credentials. These tokens, valid for 60 minutes, provide secure access to Vault services and management APIs based on the service account's permissions. Use this for general API calls when you only need basic authentication without additional context or role-based restrictions.
- **Generate a bearer token with context**:  
  Support embedding context values into bearer tokens, enabling dynamic access control and the ability to track end-user identity. These tokens include context claims and allow flexible authorization for Vault services. Use this when policies depend on specific contextual attributes or when tracking end-user identity is required.
- **Generate a scoped bearer token**:  
  Facilitate the creation of bearer tokens with role-specific access, ensuring permissions are limited to the operations allowed by the designated role. This is particularly useful for service accounts with multiple roles. Use this to enforce fine-grained role-based access control, ensuring tokens only grant permissions for a specific role.
- **Generate signed data tokens**:  
  Add an extra layer of security by digitally signing data tokens with the service account's private key. These signed tokens can be securely detokenized, provided the necessary bearer token and permissions are available. Use this to add cryptographic protection to sensitive data, enabling secure detokenization with verified integrity and authenticity.

## Generate a bearer token

The [Service Account](https://github.com/skyflowapi/skyflow-java/tree/v2/src/main/java/com/skyflow/serviceaccount/util) Java module generates service account tokens using a service account credentials file, which is provided when a service account is created. The tokens generated by this module are valid for 60 minutes and can be used to make API calls to the [Data](https://docs.skyflow.com/record/) and [Management](https://docs.skyflow.com/management/) APIs, depending on the permissions assigned to the service account.

The `BearerToken` utility class generates bearer tokens using a credentials JSON file. Alternatively, you can pass the credentials as a string.

[Example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/serviceaccount/BearerTokenGenerationExample.java):

```java
/**
 * Example program to generate a Bearer Token using Skyflow's BearerToken utility.
 * The token can be generated in two ways:
 * 1. Using the file path to a credentials.json file.
 * 2. Using the JSON content of the credentials file as a string.
 */
public class BearerTokenGenerationExample {
    public static void main(String[] args) {
        // Variable to store the generated token
        String token = null;

        // Example 1: Generate Bearer Token using a credentials.json file
        try {
            // Specify the full file path to the credentials.json file
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";

            // Check if the token is either not initialized or has expired
            if (Token.isExpired(token)) {
                // Create a BearerToken object using the credentials file
                BearerToken bearerToken = BearerToken.builder()
                        .setCredentials(new File(filePath)) // Set credentials from the file path
                        .build();

                // Generate a new Bearer Token
                token = bearerToken.getBearerToken();
            }

            // Print the generated Bearer Token to the console
            System.out.println("Generated Bearer Token (from file): " + token);
        } catch (SkyflowException e) {
            // Handle any exceptions encountered during the token generation process
            e.printStackTrace();
        }

        // Example 2: Generate Bearer Token using the credentials JSON as a string
        try {
            // Provide the credentials JSON content as a string
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";

            // Check if the token is either not initialized or has expired
            if (Token.isExpired(token)) {
                // Create a BearerToken object using the credentials string
                BearerToken bearerToken = BearerToken.builder()
                        .setCredentials(fileContents) // Set credentials from the string
                        .build();

                // Generate a new Bearer Token
                token = bearerToken.getBearerToken();
            }

            // Print the generated Bearer Token to the console
            System.out.println("Generated Bearer Token (from string): " + token);
        } catch (SkyflowException e) {
            // Handle any exceptions encountered during the token generation process
            e.printStackTrace();
        }
    }
}
```

## Generate bearer tokens with context

**Context-aware authorization** embeds context values into a bearer token during its generation and so you can reference those values in your policies. This enables more flexible access controls, such as helping you track end-user identity when making API calls using service accounts, and facilitates using signed data tokens during detokenization. .

A service account with the `context_id` identifier generates bearer tokens containing context information, represented as a JWT claim in a Skyflow-generated bearer token. Tokens generated from such service accounts include a `context_identifier` claim, are valid for 60 minutes, and can be used to make API calls to the Data and Management APIs, depending on the service account's permissions.

[Example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/serviceaccount/BearerTokenGenerationWithContextExample.java):

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;

import java.io.File;

/**
 * Example program to generate a Bearer Token using Skyflow's BearerToken utility.
 * The token is generated using two approaches:
 * 1. By providing the credentials.json file path.
 * 2. By providing the contents of credentials.json as a string.
 */
public class BearerTokenGenerationWithContextExample {
    public static void main(String[] args) {
        // Variable to store the generated Bearer Token
        String bearerToken = null;

        // Approach 1: Generate Bearer Token by specifying the path to the credentials.json file
        try {
            // Replace <YOUR_CREDENTIALS_FILE_PATH> with the full path to your credentials.json file
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";

            // Create a BearerToken object using the file path
            BearerToken token = BearerToken.builder()
                    .setCredentials(new File(filePath)) // Set credentials using a File object
                    .setCtx("abc") // Set context string (example: "abc")
                    .build(); // Build the BearerToken object

            // Retrieve the Bearer Token as a string
            bearerToken = token.getBearerToken();

            // Print the generated Bearer Token to the console
            System.out.println(bearerToken);
        } catch (SkyflowException e) {
            // Handle exceptions specific to Skyflow operations
            e.printStackTrace();
        }

        // Approach 2: Generate Bearer Token by specifying the contents of credentials.json as a string
        try {
            // Replace <YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING> with the actual contents of your credentials.json file
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";

            // Create a BearerToken object using the file contents as a string
            BearerToken token = BearerToken.builder()
                    .setCredentials(fileContents) // Set credentials using a string representation of the file
                    .setCtx("abc") // Set context string (example: "abc")
                    .build(); // Build the BearerToken object

            // Retrieve the Bearer Token as a string
            bearerToken = token.getBearerToken();

            // Print the generated Bearer Token to the console
            System.out.println(bearerToken);
        } catch (SkyflowException e) {
            // Handle exceptions specific to Skyflow operations
            e.printStackTrace();
        }
    }
}
```

## Generate scoped bearer tokens

A service account with multiple roles can generate bearer tokens with access limited to a specific role by specifying the appropriate `roleID`. This can be used to limit access to specific roles for services with multiple responsibilities, such as segregating access for billing and analytics. The generated bearer tokens are valid for 60 minutes and can only execute operations permitted by the permissions associated with the designated role.

[Example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/serviceaccount/ScopedTokenGenerationExample.java):

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;

import java.io.File;
import java.util.ArrayList;

/**
 * Example program to generate a Scoped Token using Skyflow's BearerToken utility.
 * The token is generated by providing the file path to the credentials.json file
 * and specifying roles associated with the token.
 */
public class ScopedTokenGenerationExample {
    public static void main(String[] args) {
        // Variable to store the generated scoped token
        String scopedToken = null;

        // Example: Generate Scoped Token by specifying the credentials.json file path
        try {
            // Create a list of roles that the generated token will be scoped to
            ArrayList<String> roles = new ArrayList<>();
            roles.add("ROLE_ID"); // Add a specific role to the list (e.g., "ROLE_ID")

            // Specify the full file path to the service account's credentials.json file
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";

            // Create a BearerToken object using the credentials file and associated roles
            BearerToken bearerToken = BearerToken.builder()
                    .setCredentials(new File(filePath)) // Set credentials using the credentials.json file
                    .setRoles(roles) // Set the roles that the token should be scoped to
                    .build(); // Build the BearerToken object

            // Retrieve the generated scoped token
            scopedToken = bearerToken.getBearerToken();

            // Print the generated scoped token to the console
            System.out.println(scopedToken);
        } catch (SkyflowException e) {
            // Handle exceptions that may occur during token generation
            e.printStackTrace();
        }
    }
}
```

Notes:

- You can pass either the file path of a service account key credentials file or the service account key credentials as a string to the `setCredentials` method of the `BearerTokenBuilder` class.
- If both a file path and a string are provided, the last method used takes precedence.
- To generate multiple bearer tokens concurrently using threads, refer to the following [example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/serviceaccount/BearerTokenGenerationUsingThreadsExample.java).

## Generate Signed Data Tokens

Skyflow generates data tokens when sensitive data is inserted into the vault. These data tokens can be digitally signed
with the private key of the service account credentials, which adds an additional layer of protection. Signed tokens can
be detokenized by passing the signed data token and a bearer token generated from service account credentials. The
service account must have appropriate permissions and context to detokenize the signed data tokens.

[Example](https://github.com/skyflowapi/skyflow-java/blob/main/samples/src/main/java/com/example/serviceaccount/SignedTokenGenerationExample.java):

```java
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

```json
[
  {
    "dataToken": "5530-4316-0674-5748",
    "signedDataToken": "signed_token_eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJzLCpZjA"
  }
]
```

Notes:

- You can provide either the file path to a service account key credentials file or the service account key credentials as a string to the `setCredentials` method of the `SignedDataTokensBuilder` class.
- If both a file path and a string are passed to the `setCredentials` method, the most recently specified input takes precedence.
- The `time-to-live` (TTL) value should be specified in seconds.
- By default, the TTL value is set to 60 seconds.

## Bearer token expiry edge case
When you use bearer tokens for authentication and API requests in SDKs, there's the potential for a token to expire after the token is verified as valid but before the actual API call is made, causing the request to fail unexpectedly due to the token's expiration. An error from this edge case would look something like this:

```txt
message: Authentication failed. Bearer token is expired. Use a valid bearer token. See https://docs.skyflow.com/api-authentication/
```

If you encounter this kind of error, retry the request. During the retry, the SDK detects that the previous bearer token has expired and generates a new one for the current and subsequent requests.

#### [Example](https://github.com/skyflowapi/skyflow-java/blob/v2/samples/src/main/java/com/example/serviceaccount/BearerTokenExpiryExample.java):

```java
package com.example.serviceaccount;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.ArrayList;

/**
 * This example demonstrates how to configure and use the Skyflow SDK
 * to detokenize sensitive data stored in a Skyflow vault.
 * It includes setting up credentials, configuring the vault, and
 * making a detokenization request. The code also implements a retry
 * mechanism to handle unauthorized access errors (HTTP 401).
 */
public class DetokenizeExample {
    public static void main(String[] args) {
        try {
            // Setting up credentials for accessing the Skyflow vault
            Credentials vaultCredentials = new Credentials();
            vaultCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>");

            // Configuring the Skyflow vault with necessary details
            VaultConfig vaultConfig = new VaultConfig();
            vaultConfig.setVaultId("<YOUR_VAULT_ID>"); // Vault ID
            vaultConfig.setClusterId("<YOUR_CLUSTER_ID>"); // Cluster ID
            vaultConfig.setEnv(Env.PROD); // Environment (e.g., DEV, PROD)
            vaultConfig.setCredentials(vaultCredentials); // Setting credentials

            // Creating a Skyflow client instance with the configured vault
            Skyflow skyflowClient = Skyflow.builder()
                    .setLogLevel(LogLevel.ERROR) // Setting log level to ERROR
                    .addVaultConfig(vaultConfig) // Adding vault configuration
                    .build();

            // Attempting to detokenize data using the Skyflow client
            try {
                detokenizeData(skyflowClient);
            } catch (SkyflowException e) {
                // Retry detokenization if the error is due to unauthorized access (HTTP 401)
                if (e.getHttpCode() == 401) {
                    detokenizeData(skyflowClient);
                } else {
                    // Rethrow the exception for other error codes
                    throw e;
                }
            }
        } catch (SkyflowException e) {
            // Handling any exceptions that occur during the process
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Method to detokenize data using the Skyflow client.
     * It sends a detokenization request with a list of tokens and prints the response.
     *
     * @param skyflowClient The Skyflow client instance used for detokenization.
     * @throws SkyflowException If an error occurs during the detokenization process.
     */
    public static void detokenizeData(Skyflow skyflowClient) throws SkyflowException {
        // Creating a list of tokens to be detokenized
        ArrayList<String> tokenList = new ArrayList<>();
        tokenList.add("<YOUR_TOKEN_VALUE_1>"); // First token
        tokenList.add("<YOUR_TOKEN_VALUE_2>"); // Second token

        // Building a detokenization request with the token list and configuration
        DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                .tokens(tokenList) // Adding tokens to the request
                .continueOnError(false) // Stop on error
                .redactionType(RedactionType.PLAIN_TEXT) // Redaction type (e.g., PLAIN_TEXT)
                .build();

        // Sending the detokenization request and receiving the response
        DetokenizeResponse detokenizeResponse = skyflowClient.vault().detokenize(detokenizeRequest);

        // Printing the detokenized response
        System.out.println(detokenizeResponse);
    }
}
```

# Logging

The SDK provides logging with Java's built-in logging library. By default, the SDK's logging level is set to `LogLevel.ERROR`. This can be changed using the `setLogLevel(logLevel)` method, as shown below:

Currently, the following five log levels are supported:

- `DEBUG`**:**  
  When `LogLevel.DEBUG` is passed, logs at all levels will be printed (DEBUG, INFO, WARN, ERROR).
- `INFO`**:**  
  When `LogLevel.INFO` is passed, INFO logs for every event that occurs during SDK flow execution will be printed, along with WARN and ERROR logs.
- `WARN`**:**  
  When `LogLevel.WARN` is passed, only WARN and ERROR logs will be printed.
- `ERROR`**:**  
  When `LogLevel.ERROR` is passed, only ERROR logs will be printed.
- `OFF`**:**  
  `LogLevel.OFF` can be used to turn off all logging from the Skyflow Java SDK.

**Note:** The ranking of logging levels is as follows: `DEBUG` \< `INFO` \< `WARN` \< `ERROR` \< `OFF`.

```java
import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

/**
 * This example demonstrates how to configure the Skyflow client with custom log levels
 * and authentication credentials (either token, credentials string, or other methods).
 * It also shows how to configure a vault connection using specific parameters.
 *
 * 1. Set up credentials with a Bearer token or credentials string.
 * 2. Define the Vault configuration.
 * 3. Build the Skyflow client with the chosen configuration and set log level.
 * 4. Example of changing the log level from ERROR (default) to INFO.
 */
public class ChangeLogLevel {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up credentials - either pass token or use credentials string
        // In this case, we are using a Bearer token for authentication
        Credentials credentials = new Credentials();
        credentials.setToken("<BEARER_TOKEN>"); // Replace with actual Bearer token

        // Step 2: Define the Vault configuration
        // Configure the vault with necessary details like vault ID, cluster ID, and environment
        VaultConfig config = new VaultConfig();
        config.setVaultId("<VAULT_ID>");      // Replace with actual Vault ID (primary vault)
        config.setClusterId("<CLUSTER_ID>");  // Replace with actual Cluster ID (from vault URL)
        config.setEnv(Env.PROD);              // Set the environment (default is PROD)
        config.setCredentials(credentials);   // Set credentials for the vault (either token or credentials)

        // Step 3: Define additional Skyflow credentials (optional, if needed for credentials string)
        // Create a JSON object to hold your Skyflow credentials
        JsonObject credentialsObject = new JsonObject();
        credentialsObject.addProperty("clientID", "<YOUR_CLIENT_ID>");  // Replace with your client ID
        credentialsObject.addProperty("clientName", "<YOUR_CLIENT_NAME>"); // Replace with your client name
        credentialsObject.addProperty("TokenURI", "<YOUR_TOKEN_URI>"); // Replace with your token URI
        credentialsObject.addProperty("keyID", "<YOUR_KEY_ID>");  // Replace with your key ID
        credentialsObject.addProperty("privateKey", "<YOUR_PRIVATE_KEY>"); // Replace with your private key

        // Convert the credentials object to a string format to be used for generating a Bearer Token
        Credentials skyflowCredentials = new Credentials();
        skyflowCredentials.setCredentialsString(credentialsObject.toString()); // Set credentials string

        // Step 4: Build the Skyflow client with the chosen configuration and log level
        Skyflow skyflowClient = Skyflow.builder()
                .addVaultConfig(config)                    // Add the Vault configuration
                .addSkyflowCredentials(skyflowCredentials) // Use Skyflow credentials if no token is passed
                .setLogLevel(LogLevel.INFO)                // Set log level to INFO (default is ERROR)
                .build();  // Build the Skyflow client

        // Now, the Skyflow client is ready to use with the specified log level and credentials
        System.out.println("Skyflow client has been successfully configured with log level: INFO.");
    }
}
```

# Reporting a Vulnerability

If you discover a potential security issue in this project, please reach out to us at **security@skyflow.com**. Please do not create public GitHub issues or Pull Requests, as malicious actors could potentially view them.
