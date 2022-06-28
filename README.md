# Skyflow-java
This Java SDK is designed to help developers easily implement Skyflow into their java backend. 

[![CI](https://img.shields.io/static/v1?label=CI&message=passing&color=green?style=plastic&logo=github)](https://github.com/skyflowapi/skyflow-java/actions)
[![GitHub release](https://img.shields.io/github/v/release/skyflowapi/skyflow-java.svg)](https://mvnrepository.com/artifact/com.skyflow/skyflow-java)
[![License](https://img.shields.io/github/license/skyflowapi/skyflow-java)](https://github.com/skyflowapi/skyflow-java/blob/master/LICENSE)

# Table of Contents

* [Features](#features)
* [Installation](#installation)
    * [Requirements](#requirements)
    * [Configuration](#configuration)
* [Service Account Bearer Token Generation](#service-account-token-generation)
* [Vault APIs](#vault-apis)
    * [Insert](#insert)
    * [Detokenize](#detokenize)
    * [GetById](#get-by-id)
    * [InvokeConnection](#invoke-connection)
*   [Logging](#logging)

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
implementation 'com.skyflow:skyflow-java:1.5.0'
```

#### Maven users
Add this dependency to your project's POM:

```xml
    <dependency>
        <groupId>com.skyflow</groupId>
        <artifactId>skyflow-java</artifactId>
        <version>1.5.0</version>
    </dependency>
```
---

## Service Account Bearer Token Generation
The [Service Account](https://github.com/skyflowapi/skyflow-java/tree/master/src/main/java/com/skyflow/serviceaccount) java module is used to generate service account tokens from service account credentials file which is downloaded upon creation of service account. The token generated from this module is valid for 60 minutes and can be used to make API calls to vault services as well as management API(s) based on the permissions of the service account.

The `generateBearerToken(filepath)` function takes the credentials file path for token generation, alternatively, you can also send the entire credentials as string, by using `generateBearerTokenFromCreds(credentials)` 

[Example](https://github.com/skyflowapi/skyflow-java/blob/master/samples/src/main/java/com/example/TokenGenerationExample.java
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
## Vault APIs
The [Vault](https://github.com/skyflowapi/skyflow-java/tree/master/src/main/java/com/skyflow/vault) module is used to perform operations on the vault such as inserting records, detokenizing tokens, retrieving tokens for a skyflow_id and to invoke a connection.

To use this module, the skyflow client must first be initialized as follows.
```java
import com.skyflow.vault.Skyflow;
import com.skyflow.entities.SkyflowConfiguration;

// DemoTokenProvider class is an implementation of the TokenProvider interface
DemoTokenProvider demoTokenProvider = new DemoTokenProvider(); 

SkyflowConfiguration skyflowConfig = new SkyflowConfiguration(<VAULT_ID>,<VAULT_URL>,demoTokenProvider);

Skyflow skyflowClient = Skyflow.init(skyflowConfig);
```
Example implementation of DemoTokenProvider is as follows
```java
import com.skyflow.entities.TokenProvider;

static class DemoTokenProvider implements TokenProvider {
    
        @Override
        public String getBearerToken() throws Exception {
            ResponseToken res = null;
            try {
                String filePath = "<your_credentials_file_path>";
                res = Token.GenerateBearerToken(filePath);
            } catch (SkyflowException e) {
                e.printStackTrace();
            }
            return res.getAccessToken();
        }
    }
```

All Vault APIs must be invoked using a client instance.

## Insert

To insert data into your vault, use the **insert(JSONObject insertInput, InsertOptions options)** method. The first parameter `insertInput` is a JSONObject that must have a `records` key and takes an array of records to be inserted into the vault as a value. The second parameter `options` is a InsertOptions object that provides further options for your insert call, as shown below.
```java
import com.skyflow.entities.InsertOptions;

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

// Indicates whether or not tokens should be returned for the inserted data. Defaults to 'True'
InsertOptions insertOptions = new InsertOptions(true);
   
```
An [example](https://github.com/skyflowapi/skyflow-java/blob/master/samples/src/main/java/com/example/InsertExample.java) of insert call
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

InsertOptions insertOptions = new InsertOptions(true);

try{
        JSONObject insertResponse = skyflowClient.insert(records,insertOptions);
        System.out.println(insertResponse);
    } catch(SkyflowException exception){
        System.out.println(exception);
    }
```
Sample insert Response
```JS
{
    "records": [
        {
            "table": "cards",
            "fields": {
                "cardNumber": "f3907186-e7e2-466f-91e5-48e12c2bcbc1",
                "cvv": "1989cb56-63da-4482-a2df-1f74cd0dd1a5",
            },
        }
    ]
}
```

## Detokenize

In order to retrieve data from your vault using tokens that you have previously generated for that data, you can use the **detokenize(JSONObject records)** method. The first parameter JSONObject must have a `records` key that takes an array of tokens to be fetched from the vault, as shown below.
```java
JSONObject recordsJson = new JSONObject();

JSONObject tokenJson = new JSONObject();
tokenJson.put("token","<token>");

JSONArray recordsArrayJson = new JSONArray();
recordsArrayJson.put(tokenJSon);

recordsJson.put("records",recordsArrayJson);
```
An [example](https://github.com/skyflowapi/skyflow-java/blob/master/samples/src/main/java/com/example/DetokenizeExample.java) of detokenize call
```java
JSONObject recordsJson = new JSONObject();

JSONObject validTokenJson = new JSONObject();
validTokenJson.put("token","45012507-f72b-4f5c-9bf9-86b133bae719");

JSONObject invalidTokenJson = new JSONObject();
invalidTokenJson.put("token","invalid-token");

JSONArray recordsArrayJson = new JSONArray();
recordsArrayJson.put(tokenJson);
recordsArrayJson.put(invalidTokenJson);

recordsJson.put("records",recordsArrayJson);
    try{
        JSONObject detokenizeResponse = skyflowClient.detokenize(recordsJson);
        System.out.println(detokenizeResponse);
    }catch(SkyflowExeception exception){
        if(exception.getData() != null)
            System.out.println(exception.getData());
        else
            System.out.println(exception);
    }
```
Sample detokenize Response
```js
{
   "records": [
      {
        "token": "45012507-f72b-4f5c-9bf9-86b133bae719",
        "value": "1990-01-01"
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

An [example](https://github.com/skyflowapi/skyflow-java/blob/master/samples/src/main/java/com/example/GetByIdExample.java) getById call 
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
recordsJson.put("records", recordsArray);

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

An [example](https://github.com/skyflowapi/skyflow-java/blob/master/samples/src/main/java/com/example/InvokeConnectionExample.java) of invokeConnection:
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
