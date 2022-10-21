# Java SDK samples
Test the SDK by adding `VAULT-ID`, `VAULT-URL`, and `SERVICE-ACCOUNT` details in the required places for each sample.

## Prerequisites
-  A Skyflow account. If you don't have one, register for one on the [Try Skyflow](https://skyflow.com/try-skyflow) page.
- Java 1.8 or higher.

### Create the vault
1. In a browser, sign in to Skyflow Studio.
2. Create a vault by clicking **Create Vault** > **Start With a Template** > **Quickstart vault**.
3. Once the vault is ready, click the gear icon and select **Edit Vault Details**.
4. Note your **Vault URL** and **Vault ID** values, then click **Cancel**. You'll need these later.

### Create a service account
1. In the side navigation click, **IAM** > **Service Accounts** > **New Service Account**.
2. For **Name**, enter "SDK Sample". For **Roles**, choose **Vault Editor**.
3. Click **Create**. Your browser downloads a **credentials.json** file. Keep this file secure, as You'll need it for each of the samples.

## The samples
### Detokenize
Detokenize a data token from the vault. Make sure the specified token is for data that exists in the vault. If you need a valid token, use [InsertEample.java](src/main/java/com/example/InsertExample.java) to insert the data, then use this data's token for detokenization.
#### Configure
1. Replace **<your_vaultID>** with **VAULT ID**
2. Replace **<your_vaultURL>** with **VAULT URL**
3. Replace **<your_token>** with **Data Token**.
4. Replace **<YOUR_CREDENTIALS_FILE_PATH>** with relative  path of **SERVICE ACCOUNT CREDENTIAL FILE**.
#### Run the sample
        
        javac DetokenizeExample.java
        java DetokenizeExample
### GetById
Get data using skyflow id. 
#### Configure
1. Replace **<your_vaultID>** with **VAULT ID**
2. Replace **<your_vaultURL>** with **VAULT URL**.
3. Replace **<your_skyflowId>** with **Skyflow id**.
4. Replace **<YOUR_CREDENTIALS_FILE_PATH>** with relative  path of **SERVICE ACCOUNT CREDENTIAL FILE**.
5. Replace **<your_table_name>** with **credit_cards**.
#### Run the sample
        
        javac GetByIdExample.java
        java GetByIdExample
### Insert
Insert data in the vault.
#### Configure
1. Replace **<your_vaultID>** with **VAULT ID**.
2. Replace **<your_vaultURL>** with **VAULT URL**.
3. Replace **<YOUR_CREDENTIALS_FILE_PATH>** with relative  path of **SERVICE ACCOUNT CREDENTIAL FILE**.
4. Replace **<your_table_name>** with **credit_cards**.
5. Replace **<your_field_name>** with **column name**.
6. Replace **<your_field_value>** with **valid value corresponding to column name**.
#### Run the sample
    
        javac InsertExample.java
        java InsertExample
### InvokeConnection
Skyflow Connections is a gateway service that uses Skyflow's underlying tokenization capabilities to securely connect to first-party and third-party services. This way, your infrastructure is never directly exposed to sensitive data, and you offload security and compliance requirements to Skyflow.
#### Configure
1. Replace **<your_vaultID>** with **VAULT ID**.
2. Replace **<your_vaultURL>** with **VAULT URL**.
3. Replace **<YOUR_CREDENTIALS_FILE_PATH>** with relative  path of **SERVICE ACCOUNT CREDENTIAL FILE**.
4. Replace **<your_connection_url>** with **Connection url**.
5. Replace **<path_param_key>** with **Path param key**.
6. Replace **<path_param_value>** with **Path param value**.
7. Replace **<query_param_key>** with **Query param key**.
8. Replace **<query_param_value>** with **Query param value**.
9. Replace **<request_header_key>** with **Request header key**.
10. Replace **<request_header_vaule>** with **Request header value**.
11. Replace **<request_body_key>** with **Request body key**.
12. Replace **<request_body_value>** with **Request body value**.
#### Run the sample
            
        javac InvokeConnectionExample.java
        java InvokeConnectionExample

### TokenGeneration
Generates bearer tokens using file path and content of the SA credentials file
#### Configure
1. Replace **<YOUR_CREDENTIALS_FILE_PATH>** with relative  path of **SERVICE ACCOUNT CREDENTIAL FILE PATH**.
2. Replace **<<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>>** with relative  path of **SERVICE ACCOUNT CREDENTIAL FILE CONTENT AS STRING**.
#### Run the sample
        
        javac TokenGenerationExample.java
        java TokenGenerationExample
