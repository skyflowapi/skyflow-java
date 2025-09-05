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
- [Quickstart](#quickstart)
  - [Authenticate](#authenticate)
  - [Initialize the client](#initialize-the-client)
- [Vault](#vault)
  - [Bulk insert data into the vault](#bulk-insert-data-into-the-vault)
  - [Bulk Detokenize](#bulk-detokenize)
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
- Perform Vault API operations such as inserting, and detokenizing sensitive data with ease.

# Install

## Requirements

- Java 8 and above (tested with Java 8)

## Configuration

---

### Gradle users

Add this dependency to your project's `build.gradle` file:

```
implementation 'com.skyflow:skyflow-java:3.0.0-beta.3'
```

### Maven users

Add this dependency to your project's `pom.xml` file:

```xml
<dependency>
    <groupId>com.skyflow</groupId>
    <artifactId>skyflow-java</artifactId>
    <version>3.0.0-beta.3</version>
</dependency>
```

---

# Quickstart

Get started quickly with the essential steps: authenticate and initialize the client. This section provides a minimal setup to help you integrate the SDK efficiently.

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
 * Example program to initialize the Skyflow client with a vault configuration.
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
        Credentials credentials = new Credentials();
        credentials.setToken("<BEARER_TOKEN>"); // Replace <BEARER_TOKEN> with your actual authentication token.

        // Step 2: Configure the primary vault details.
        // VaultConfig stores all necessary details to connect to a specific Skyflow vault.
        VaultConfig config = new VaultConfig();
        primaryConfig.setVaultId("<VAULT_ID>"); // Replace with your vault's ID.
        primaryConfig.setClusterId("<CLUSTER_ID>");     // Replace with the cluster ID (part of the vault URL, e.g., https://{clusterId}.vault.skyflowapis.com).
        primaryConfig.setEnv(Env.PROD);                 // Set the environment (PROD, SANDBOX, STAGE, DEV).
        primaryConfig.setCredentials(credentials); // Attach the credentials to this vault configuration.

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

        // Step 5: Build and initialize the Skyflow client.
        // Skyflow client is configured with multiple vaults and credentials.
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.INFO)                  // Set log level for debugging or monitoring purposes.
                .addVaultConfig(config)               // Add the vault configuration.
                .addSkyflowCredentials(skyflowCredentials)   // Add JSON-formatted credentials if applicable.
                .build();

        // The Skyflow client is now fully initialized.
        // Use the `skyflowClient` object to perform secure operations such as:
        // - Inserting data
        // - Detokenizing data
        // within the configured Skyflow vaults.
    }
}
```

Notes:

- If both Skyflow common credentials and individual credentials at the configuration level are specified, the individual credentials at the configuration level will take precedence.
- If neither Skyflow common credentials nor individual configuration-level credentials are provided, the SDK attempts to retrieve credentials from the `SKYFLOW_CREDENTIALS` environment variable.
- All Vault operations require a client instance.

# Vault

The [Vault](https://github.com/skyflowapi/skyflow-java/tree/main/src/main/java/com/skyflow/vault) module performs operations on the vault, including inserting records and detokenizing tokens.

## Bulk insert data into the vault

To insert data into your vault, use the `bulkinsert` or `bulkInsertAsync` methods.  The `InsertRequest` class creates an insert request, which includes the values to be inserted as a list of records.

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

            // Step 3: Use the Skyflow client to perform the sync bulk insert operation
            InsertResponse insertResponse = skyflowClient.vault().bulkInsert(insertRequest);

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

### An [example](https://github.com/skyflowapi/skyflow-java/blob/v3/samples/src/main/java/com/example/vault/BulkInsertSync.java) of a sync bulkInsert call

The `bulkInsert` operation operation will insert the data synchronously into the vault.

```java
/**
 * Example program to demonstrate how to perform a synchronous bulk insert operation using the Skyflow Java SDK.
 * The process involves:
 * 1. Setting up credentials and vault configuration
 * 2. Creating multiple records to be inserted
 * 3. Building and executing a bulk insert request
 * 4. Handling the insert response or any potential errors
 */
public class BulkInsertSync {
    public static void main(String[] args) {
        try {
		        // Initialize Skyflow client
            
            // Step 1: Prepare the data to be inserted into the Skyflow vault
            ArrayList<HashMap<String, Object>> insertData = new ArrayList<>();

            // Create the first record with field names and their respective values
            HashMap<String, Object> insertRecord1 = new HashMap<>();
            insertRecord1.put("name", "John doe"); // Replace with actual field name and value
            insertRecord1.put("email", "john.doe@example.com"); // Replace with actual field name and value

            // Create the second record with field names and their respective values
            HashMap<String, Object> insertRecord2 = new HashMap<>();
            insertRecord2.put("name", "Jane doe"); // Replace with actual field name and value
            insertRecord2.put("email", "jane.doe@example.com"); // Replace with actual field name and value

            // Add the records to the list of data to be inserted
            insertData.add(insertRecord1);
            insertData.add(insertRecord2);

            // Step 2: Build an InsertRequest object with the table name and the data to insert
            InsertRequest insertRequest = InsertRequest.builder()
                    .table("table1") // Replace with the actual table name in your Skyflow vault
                    .values(insertData)   // Attach the data to be inserted
                    .build();

            // Step 3: Use the Skyflow client to perform the sync bulk insert operation
            InsertResponse insertResponse = skyflowClient.vault().bulkInsert(insertRequest);

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

Skyflow returns tokens and data for the records that were just inserted.

```json
{
  "summary": { 
    "totalRecords": 2, 
    "totalInserted": 2,
    "totalFailed": 0 
  },
  "sucess": [
    {
      "index": 0,
      "skyflow_id": "9fac9201-7b8a-4446-93f8-5244e1213bd1",
      "tokens": {
        "name": [{ "token": "token_name", "tokenGroupName": "deterministic_string" }],
        "email": [
          {
            "token": "augn0@xebggri.lmp",
            "tokenGroupName": "nondeterministic_string"
          }
        ]
      },
      "data": { "email": "john.doe@example.com", "name": "john doe" }
    },
    {
      "index": 1,
      "skyflow_id": "9fac9201-7b8a-4446-93f8-5244e1213bd3",
      "tokens": {
        "name": [{ "token": "token_name", "tokenGroupName": "deterministic_string" }],
        "email": [
          {
            "token": "buhn0@xebggrj.lmt",
            "tokenGroupName": "nondeterministic_string"
          }
        ]
      },
      "data": { "email": "jane.doe@example.com", "name": "Jane doe" }
    },
  ],
  "errors": []
}
```

### An [example](https://github.com/skyflowapi/skyflow-java/blob/v3/samples/src/main/java/com/example/vault/BulkInsertAsync.java) of an async bulkInsert call

The `bulkInsertAsync` operation operation will insert the data asynchronously into the vault.

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Example program to demonstrate how to perform an asynchronous bulk insert operation using the Skyflow Java SDK.
 * The process involves:
 * 1. Setting up credentials and vault configuration
 * 2. Creating multiple records to be inserted
 * 3. Building and executing an async bulk insert request
 * 4. Handling the insert response or errors using CompletableFuture
 */
public class BulkInsertAsync {
    public static void main(String[] args) {
        try {
		// Initialize Skyflow client
            // Step 1: Prepare the data to be inserted into the Skyflow vault
            ArrayList<HashMap<String, Object>> insertData = new ArrayList<>();

            // Create the first record with field names and their respective values
            HashMap<String, Object> insertRecord1 = new HashMap<>();
            insertRecord1.put("name", "John doe"); // Replace with actual field name and value
            insertRecord1.put("email", "john.doe@example.com"); // Replace with actual field name and value

            // Create the second record with field names and their respective values
            HashMap<String, Object> insertRecord2 = new HashMap<>();
            insertRecord2.put("name", "Jane doe"); // Replace with actual field name and value
            insertRecord2.put("email", "jane.doe@example.com"); // Replace with actual field name and value

            // Add the records to the list of data to be inserted
            insertData.add(insertRecord1);
            insertData.add(insertRecord2);

            // Step 2: Build an InsertRequest object with the table name and the data to insert
            InsertRequest insertRequest = InsertRequest.builder()
                    .table("table1") // Replace with the actual table name in your Skyflow vault
                    .values(insertData)   // Attach the data to be inserted
                    .build();

            // Step 3: Perform the async bulk insert operation using the Skyflow client
            CompletableFuture<InsertResponse> future = skyflowClient.vault().bulkInsertAsync(insertRequest);
            // Add success and error callbacks
            future.thenAccept(response -> {
                System.out.println("Async bulk insert resolved with response:\t" + response);
            }).exceptionally(throwable -> {
                System.err.println("Async bulk insert rejected with error:\t" + throwable.getMessage());
                throw new CompletionException(throwable);
            });
        } catch (SkyflowException e) {
            // Step 7: Handle any exceptions that may occur during the insert/upsert operation
            System.out.println("Error occurred: ");
            e.printStackTrace(); // Print the stack trace for debugging purposes
        }
    }
}
```

Skyflow returns tokens and data for the records you just inserted.

```json
{
  "summary": { 
    "totalRecords": 2, 
    "totalInserted": 2,
    "totalFailed": 0 
  },
  "sucess": [
    {
      "index": 0,
      "skyflow_id": "9fac9201-7b8a-4446-93f8-5244e1213bd1",
      "tokens": {
        "name": [{ "token": "token_name", "tokenGroupName": "deterministic_string" }],
        "email": [
          {
            "token": "augn0@xebggri.lmp",
            "tokenGroupName": "nondeterministic_string"
          }
        ]
      },
      "data": { "email": "john.doe@example.com", "name": "john doe" }
    },
    {
      "index": 1,
      "skyflow_id": "9fac9201-7b8a-4446-93f8-5244e1213bd3",
      "tokens": {
        "name": [{ "token": "token_name", "tokenGroupName": "deterministic_string" }],
        "email": [
          {
            "token": "buhn0@xebggrj.lmt",
            "tokenGroupName": "nondeterministic_string"
          }
        ]
      },
      "data": { "email": "jane.doe@example.com", "name": "Jane doe" }
    },
  ],
  "errors": []
}
```

## Bulk detokenize

To retrieve tokens from your vault, use the `bulkDetokenize` or `bulkDetokenizeAsync` methods. You can specify how the data should be redacted based on token groups. The `DetokenizeRequest` has two main components:

- `tokens`: List of token strings to detokenize
- `tokenGroupRedactions`: List of redaction rules for specific token groups

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

            TokenGroupRedactions tokenGroupRedaction = TokenGroupRedactions.builder()
                    .tokenGroupName("<YOR_TOKEN_GROUP_NAME>")
                    .redaction("<YOUR_REDACTION>")
                    .build();
            List<TokenGroupRedactions> tokenGroupRedactions = new ArrayList<>();
            tokenGroupRedactions.add(tokenGroupRedaction);

            // Step 2: Create the DetokenizeRequest object with the tokens and token group redactions 
            DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                    .tokens(tokens)                             // Provide the list of tokens to be detokenized
                    .tokenGroupredactions(tokenGroupRedactions) // Provide a list of token grpup redactions
                    .build();                                   // Build the detokenization request

            // Step 3: Call the Skyflow vault to detokenize the provided tokens
            DetokenizeResponse detokenizeResponse = skyflowClient.vault().bulkDetokenize(detokenizeRequest);

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

### An [example](https://github.com/skyflowapi/skyflow-java/blob/v3/samples/src/main/java/com/example/vault/BulkDetokenizeSync.java) of a sync bulkDetokenize call:

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.TokenGroupRedactions;

import java.util.ArrayList;
import java.util.List;

/**
 * This sample demonstrates how to perform a synchronous bulk detokenize operation using the Skyflow Java SDK.
 * The process involves:
 * 1. Setting up credentials and vault configuration
 * 2. Creating a list of tokens to detokenize
 * 3. Configuring token group redactions
 * 4. Building and executing a bulk detokenize request
 * 5. Handling the detokenize response or any potential errors
 */
public class BulkDetokenizeSync {
    public static void main(String[] args) {
        try {
		        // Initialize Skyflow client
            // Step 1: Initialize a list of tokens to be detokenized (replace with actual token values)
            ArrayList<String> tokens = new ArrayList<>();
            tokens.add("b8eea77a-47e1-4d67-a560-fd395cabc82f"); // Replace with your actual token value
            tokens.add("6ffb412b-a79d");                        // Replace with your actual token value

            TokenGroupRedactions tokenGroupRedaction = TokenGroupRedactions.builder()
                    .tokenGroupName("deterministic_regex")
                    .redaction("MASKED")
                    .build();
            List<TokenGroupRedactions> tokenGroupRedactions = new ArrayList<>();
            tokenGroupRedactions.add(tokenGroupRedaction);

            // Step 2: Create the DetokenizeRequest object with the tokens and token group redactions
            DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                    .tokens(tokens)                             // Provide the list of tokens to be detokenized
                    .tokenGroupredactions(tokenGroupRedactions) // Provide a list of token grpup redactions
                    .build();                                   // Build the detokenization request

            // Step 3: Call the Skyflow vault to bulk detokenize the provided tokens synchronously
            DetokenizeResponse detokenizeResponse = skyflowClient.vault().bulkDetokenizeAsync(detokenizeRequest);

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
  "summary": {
    "total_tokens": 2,
    "total_detokenized": 2,
    "total_failed": 0,               
  },
  "success": [
    {
      "index": 0,
      "token": "b8eea77a-47e1-4d67-a560-fd395cabc82f",
      "value": "xxxx@skyflow.com",
      "tokenGroupName": "nondeterministic_regex",
      "metadata": {
          "skyflowID": "5ddc71a6-3bdb-47e4-9723-259452946349",
           "tableName": "table1"
      }
    },
  ],
  "errors": [
    {
      "index": 1,
      "code": 404,
      "error": "Detokenize failed. Token 6ffb412b-a79d is invalid. Specify a valid token.",       
    }
  ]
}
```

### An [example]() of an async bulkDetokenize call


```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.TokenGroupRedactions;

import java.util.ArrayList;
import java.util.List;

/**
 * This sample demonstrates how to perform an asynchronous bulk detokenize operation using the Skyflow Java SDK.
 * The process involves:
 * 1. Setting up credentials and vault configuration
 * 2. Creating a list of tokens to detokenize
 * 3. Configuring token group redactions
 * 4. Building and executing a bulk detokenize request
 * 5. Handling the detokenize response or errors using CompletableFuture
 */
public class BulkDetokenizeAsync {
    public static void main(String[] args) {
        try {
		        // Initialize Skyflow client
            // Step 1: Initialize a list of tokens to be detokenized (replace with actual token values)
            ArrayList<String> tokens = new ArrayList<>();
            tokens.add("b8eea77a-47e1-4d67-a560-fd395cabc82f"); // Replace with your actual token value
            tokens.add("6ffb412b-a79d");                        // Replace with your actual token value
            
            TokenGroupRedactions tokenGroupRedaction = TokenGroupRedactions.builder()
                    .tokenGroupName("deterministic_string")
                    .redaction("MASKED")
                    .build();
            List<TokenGroupRedactions> tokenGroupRedactions = new ArrayList<>();
            tokenGroupRedactions.add(tokenGroupRedaction);

            // Step 2: Create the DetokenizeRequest object with the tokens and redaction type
            DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                    .tokens(tokens)                             // Provide the list of tokens to be detokenized
                    .tokenGroupredactions(tokenGroupRedactions) // Provide a list of token grpup redactions
                    .build();                                   // Build the detokenization request

            // Step 3: Call the Skyflow vault to bulk detokenize the provided tokens asynchronously
            DetokenizeResponse detokenizeResponse = skyflowClient.vault().bulkDetokenizeAsync(detokenizeRequest);

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
  "summary": {
    "total_tokens": 2,
    "total_detokenized": 2,
    "total_failed": 0,               
  },
  "success": [
    {
      "index": 0,
      "token": "b8eea77a-47e1-4d67-a560-fd395cabc82f",
      "value": "xxxx@skyflow.com",
      "tokenGroupName": "nondeterministic_regex",
      "metadata": {
          "skyflowID": "5ddc71a6-3bdb-47e4-9723-259452946349",
           "tableName": "table1"
      }
    },
  ],
  "errors": [
    {
      "index": 1,
      "code": 404,
      "error": "Detokenize failed. Token 6ffb412b-a79d is invalid. Specify a valid token.",       
    }
  ]
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