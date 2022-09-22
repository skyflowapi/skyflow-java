# JAVA-SDK sample templates
Use this folder to test the functionalities of JAVA-SDK just by adding `VAULT-ID` `VAULT-URL` and `SERVICE-ACCOUNT` details at the required place.

## Prerequisites
- A Skylow account. If you don't have one, you can register for one on the [Try Skyflow](https://skyflow.com/try-skyflow) page.
- Java 1.8 and above.

## Configure
- Before you can run the sample app, create a vault


### Create the vault
1. In a browser, navigate to Skyflow Studio and log in.
2. Create a vault by clicking **Create Vault** > **Start With a Template** > **Quickstart vault**.
3. Once the vault is created, click the gear icon and select **Edit Vault** Details.
4. Note your Vault URL and Vault ID values, then click Cancel. You'll need these later.

### Create a service account
1. In the side navigation click, **IAM** > **Service Accounts** > **New Service Account**.
2. For Name, enter **Test-Java-Sdk-Sample**. For Roles, choose according to the action.
3. Click **Create**. Your browser downloads a **credentials.json** file. Keep this file secure, as you'll need it in the next steps.

### Different types of functionalities of Java-Sdk
- [**Detokenize**](src/main/java/com/example/DetokenizeExample.java)
    - Detokenize the data token from the vault. 
    - Make sure the token is of the data which exists in the Vault. If not so please make use of [InsertEample.java](src/main/java/com/example/InsertExample.java) to insert the data in the data and use this token for detokenization.
    - Configure
        - Replace **<your_vaultID>** with **VAULT ID**
        - Replace **<your_vaultURL>** with **VAULT URL**.
        - Replace **<your_token>** with **Data Token**.
        - Replace **<YOUR_CREDENTIALS_FILE_PATH>** with relative  path of **SERVICE ACCOUNT CREDENTIAL FILE**.
    - Execution
            
            javac DetokenizeExample.java
            java DetokenizeExample
- [**GetById**](src/main/java/com/example/GetByIdExample.java)
    - Get data using skyflow id. 
    - Configure
        - Replace **<your_vaultID>** with **VAULT ID**
        - Replace **<your_vaultURL>** with **VAULT URL**.
        - Replace **<your_skyflowId>** with **Skyflow id**.
        - Replace **<YOUR_CREDENTIALS_FILE_PATH>** with relative  path of **SERVICE ACCOUNT CREDENTIAL FILE**.
        - Replace **<your_table_name>** with **credit_cards**.
    - Execution
            
            javac GetByIdExample.java
            java GetByIdExample
- [**Insert**](src/main/java/com/example/InsertExample.java)
    - Insert data in the vault.
    - Configure
        - Replace **<your_vaultID>** with **VAULT ID**.
        - Replace **<your_vaultURL>** with **VAULT URL**.
        - Replace **<YOUR_CREDENTIALS_FILE_PATH>** with relative  path of **SERVICE ACCOUNT CREDENTIAL FILE**.
        - Replace **<your_table_name>** with **credit_cards**.
        - Replace **<your_field_name>** with **column name**.
        - Replace **<your_field_value>** with **valid value corresponding to column name**.
        - Execution
        
                javac InsertExample.java
                java InsertExample
- [**InvokeConnection**](src/main/java/com/example/InvokeConnectionExample.java)
    - Invoke connection
    - Configure
        - Replace **<your_vaultID>** with **VAULT ID**.
        - Replace **<your_vaultURL>** with **VAULT URL**.
        - Replace **<YOUR_CREDENTIALS_FILE_PATH>** with relative  path of **SERVICE ACCOUNT CREDENTIAL FILE**.
        - Replace **<your_connection_url>** with **Connection url**.
        - Replace **<path_param_key>** with **Path param key**.
        - Replace **<path_param_value>** with **Path param value**.
        - Replace **<query_param_key>** with **Query param key**.
        - Replace **<query_param_value>** with **Query param value**.
        - Replace **<request_header_key>** with **Request header key**.
        - Replace **<request_header_vaule>** with **Request header value**.
        - Replace **<request_body_key>** with **Request body key**.
        - Replace **<request_body_value>** with **Request body value**.
        - Execution
            
                javac InvokeConnectionExample.java
                java InvokeConnectionExample

- [**TokenGeneration**](src/main/java/com/example/TokenGenerationExample.java)
    - generates bearer tokens using file path and content of the SA credentials file
    - Replace **<YOUR_CREDENTIALS_FILE_PATH>** with relative  path of **SERVICE ACCOUNT CREDENTIAL FILE PATH**.
    - Replace **<<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>>** with relative  path of **SERVICE ACCOUNT CREDENTIAL FILE CONTENT AS STRING**.
    - Execution
            
                javac TokenGenerationExample.java
                java TokenGenerationExample
