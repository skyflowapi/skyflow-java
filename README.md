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
import com.skyflow.enums.UpsertType;
import com.skyflow.vault.data.InsertRecord;
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
            HashMap<String, Object> recordData1 = new HashMap<>();
            rerecordData1cord1.put("<YOUR_COLUMN_NAME_1>", "<YOUR_VALUE_1>");
            recordData1.put("<YOUR_COLUMN_NAME_2>", "<YOUR_VALUE_1>");
            
            // Specify the columns to be used for upsert operation
            List<String> upsertColumns = new ArrayList<>();
            upsertColumns.add("<YOUR_COLUMN_NAME_1>");

            // Create the first record with field names and their respective values
            InsertRecord insertRecord1 = InsertRecord
                    .builder()
                    .data(recordData1)
                    .table("<YOUR_TABLE_NAME>")
                    .upsert(upsertColumns)
                    .upsertType(UpsertType.UPDATE)
                    .build();

            // Step 2: Prepare second record for insertion
            HashMap<String, Object> recordData2 = new HashMap<>();
            recordData2.put("<YOUR_COLUMN_NAME_1>", "<YOUR_VALUE_1>");
            recordData2.put("<YOUR_COLUMN_NAME_2>", "<YOUR_VALUE_1>");

            InsertRecord insertRecord2 = InsertRecord
                    .builder()
                    .data(recordData2)
                    .table("<YOUR_TABLE_NAME>")
                    .build();

            // Step 3: Combine records into a Insert record list
            ArrayList<InsertRecord> insertRecords = new ArrayList<>();
            insertRecords.add(insertRecord1);
            insertRecords.add(insertRecord2);

            // Step 4: Build the insert request with table name and values
            InsertRequest request = InsertRequest
                    .builder()
                    .records(insertRecords)
                    .table("<YOUR_TABLE_NAME>")
                    .upsert(upsertColumns)
                    .upsertType(UpsertType.UPDATE)
                    .build();
            
            // Step 5: Use the Skyflow client to perform the sync bulk insert operation
            InsertResponse insertResponse = skyflowClient.vault().bulkInsert(insertRequest);

            // Print the response from the insert operation
            System.out.println("Insert Response: " + insertResponse);
        } catch (SkyflowException e) {
            // Step 6: Handle any exceptions that occur during the insert operation
            System.out.println("Error occurred while inserting data: ");
            e.printStackTrace(); // Print the stack trace for debugging
        }
    }
}
```

**Note**:
- The tableName can be specified either at the request level `InsertRequest` or at the record level `InsertRecord`, but not both.
- If tableName is not specified at the request level `InsertRequest`, then it must be specified in all record objects.
- If tableName is specified at the request level `InsertRequest`, then upsert must also be specified at the request level.
- If tableName is specified at the record level `InsertRecord`, then upsert must also be specified at the record level `InsertRecord`.

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
            ArrayList<InsertRecord> insertRecords = new ArrayList<>();

            // Create the first record with field names and their respective values
            HashMap<String, Object> insertData1 = new HashMap<>();
            insertData1.put("name", "John doe"); // Replace with actual field name and value
            insertData1.put("email", "john.doe@example.com"); // Replace with actual field name and value
            InsertRecord insertRecord1 = InsertRecord
                    .builder()
                    .data(insertData1)
                    .build();

            // Create the second record with field names and their respective values
            HashMap<String, Object> insertData2 = new HashMap<>();
            insertData2.put("name", "Jane doe"); // Replace with actual field name and value
            insertData2.put("email", "jane.doe@example.com"); // Replace with actual field name and value
            InsertRecord insertRecord2 = InsertRecord
                    .builder()
                    .data(insertData2)
                    .build();

            // Add the records to the list of data to be inserted
            insertRecords.add(insertRecord1);
            insertRecords.add(insertRecord2);

            // Specify the columns to be used for upsert operation
            List<String> upsertColumns = new ArrayList<>();
            upsertColumns.add("name"); // // Replace with actual unique field name 

            // Step 2: Build an InsertRequest object with the table name and the data to insert
            InsertRequest insertRequest = InsertRequest.builder()
                    .table("table1") // Replace with the actual table name in your Skyflow vault
                    .records(insertRecords)   // Attach the data to be inserted
                    .upsert(upsertColumns) // upsert 
                    .upsertType(UpsertType.UPDATE) // upsert type
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
      "data": { "email": "john.doe@example.com", "name": "john doe" },
      "table": "table1",
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
      "data": { "email": "jane.doe@example.com", "name": "Jane doe" },
      "table": "table1",
    },
  ],
  "errors": []
}
```

### An [example](https://github.com/skyflowapi/skyflow-java/blob/v3/samples/src/main/java/com/example/vault/BulkInsertSync.java) of a sync bulkInsert call for inserting data into multiple tables

The `bulkInsert` operation will insert records in multiple tables the data synchronously into the vault.

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
            ArrayList<InsertRecord> insertRecords = new ArrayList<>();

            // Create the first record with field names and their respective values
            HashMap<String, Object> insertData1 = new HashMap<>();
            insertData1.put("name", "John doe"); // Replace with actual field name and value
            insertData1.put("email", "john.doe@example.com"); // Replace with actual field name and value
            
            // Specify the columns to be used for upsert operation
            List<String> upsertColumns = new ArrayList<>();
            upsertColumns.add("name"); // Replace with actual unique field name 

            InsertRecord insertRecord1 = InsertRecord
                    .builder()
                    .table("table1") // replace the table name 
                    .data(insertData1)
                    .upsert(upsertColumns) // upsert 
                    .upsertType(UpsertType.UPDATE) // upsert type
                    .build();

            // Create the second record with field names and their respective values
            HashMap<String, Object> insertData2 = new HashMap<>();
            insertData2.put("name", "Jane doe"); // Replace with actual field name and value
            insertData2.put("email", "jane.doe@example.com"); // Replace with actual field name and value
            InsertRecord insertRecord2 = InsertRecord
                    .builder()
                    .table("table2") // replace the table name 
                    .data(insertData2)
                    .build();

            // Add the records to the list of data to be inserted
            insertRecords.add(insertRecord1);
            insertRecords.add(insertRecord2);

            // Step 2: Build an InsertRequest object with the table name and the data to insert
            InsertRequest insertRequest = InsertRequest.builder()
                    .records(insertRecords)   // Attach the data to be inserted
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
      "data": { "email": "john.doe@example.com", "name": "john doe" },
      "table": "table1",
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
      "data": { "email": "jane.doe@example.com", "name": "Jane doe" },
      "table":"table2"
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
            ArrayList<InsertRecord> insertRecords = new ArrayList<>();

            // Create the first record with field names and their respective values
            HashMap<String, Object> insertData1 = new HashMap<>();
            insertData1.put("name", "John doe"); // Replace with actual field name and value
            insertData1.put("email", "john.doe@example.com"); // Replace with actual field name and value
            InsertRecord insertRecord1 = InsertRecord
                    .builder()
                    .data(insertData1)
                    .build();

            // Create the second record with field names and their respective values
            HashMap<String, Object> insertData2 = new HashMap<>();
            insertData2.put("name", "Jane doe"); // Replace with actual field name and value
            insertData2.put("email", "jane.doe@example.com"); // Replace with actual field name and value
            InsertRecord insertRecord2 = InsertRecord
                    .builder()
                    .data(insertData2)
                    .build();

            // Add the records to the list of data to be inserted
            insertRecords.add(insertRecord1);
            insertRecords.add(insertRecord2);

            // Specify the columns to be used for upsert operation
            List<String> upsertColumns = new ArrayList<>();
            upsertColumns.add("name"); // // Replace with actual unique field name 

            // Step 2: Build an InsertRequest object with the table name and the data to insert
            InsertRequest insertRequest = InsertRequest.builder()
                    .table("table1") // Replace with the actual table name in your Skyflow vault
                    .records(insertRecords)   // Attach the data to be inserted
                    .upsert(upsertColumns) // upsert 
                    .upsertType(UpsertType.UPDATE) // upsert type
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
      "data": { "email": "john.doe@example.com", "name": "john doe" },
      "table": "table1",
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
      "data": { "email": "jane.doe@example.com", "name": "Jane doe" },
      "table": "table1",
    },
  ],
  "errors": []
}
```

### An [example](https://github.com/skyflowapi/skyflow-java/blob/v3/samples/src/main/java/com/example/vault/BulkInsertAsync.java) of an async bulkInsert call for inserting data into multiple tables

The `bulkInsertAsync` operation operation will insert records in multiple tables the data asynchronously into the vault.

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
            ArrayList<InsertRecord> insertRecords = new ArrayList<>();

            // Create the first record with field names and their respective values
            HashMap<String, Object> insertData1 = new HashMap<>();
            insertData1.put("name", "John doe"); // Replace with actual field name and value
            insertData1.put("email", "john.doe@example.com"); // Replace with actual field name and value
            
            // Specify the columns to be used for upsert operation
            List<String> upsertColumns = new ArrayList<>();
            upsertColumns.add("name"); // Replace with actual unique field name 

            InsertRecord insertRecord1 = InsertRecord
                    .builder()
                    .table("table1") // replace the table name 
                    .data(insertData1)
                    .upsert(upsertColumns) // upsert 
                    .upsertType(UpsertType.UPDATE) // upsert type
                    .build();

            // Create the second record with field names and their respective values
            HashMap<String, Object> insertData2 = new HashMap<>();
            insertData2.put("name", "Jane doe"); // Replace with actual field name and value
            insertData2.put("email", "jane.doe@example.com"); // Replace with actual field name and value
            InsertRecord insertRecord2 = InsertRecord
                    .builder()
                    .table("table2") // replace the table name 
                    .data(insertData2)
                    .build();

            // Add the records to the list of data to be inserted
            insertRecords.add(insertRecord1);
            insertRecords.add(insertRecord2);

            // Step 2: Build an InsertRequest object with the table name and the data to insert
            InsertRequest insertRequest = InsertRequest.builder()
                    .records(insertRecords)   // Attach the data to be inserted
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
      "data": { "email": "john.doe@example.com", "name": "john doe" },
      "table": "table1",
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
      "data": { "email": "jane.doe@example.com", "name": "Jane doe" },
      "table": "table2",
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

The [Service Account](https://github.com/skyflowapi/skyflow-java/tree/v3/common/src/main/java/com/skyflow/serviceaccount/util) Java module generates service account tokens using a service account credentials file, which is provided when a service account is created. The tokens generated by this module are valid for 60 minutes and can be used to make API calls to the [Data](https://docs.skyflow.com/api/data/) and [Management](https://docs.skyflow.com/management/) APIs, depending on the permissions assigned to the service account.

The `BearerToken` utility class generates bearer tokens using a credentials JSON file. Alternatively, you can pass the credentials as a string.

Example:

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

Example:

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

Example:

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

## Generate Signed Data Tokens

Skyflow generates data tokens when sensitive data is inserted into the vault. These data tokens can be digitally signed
with the private key of the service account credentials, which adds an additional layer of protection. Signed tokens can
be detokenized by passing the signed data token and a bearer token generated from service account credentials. The
service account must have appropriate permissions and context to detokenize the signed data tokens.

Example:

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

#### Example:

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