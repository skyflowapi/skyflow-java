package com.skyflow.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Constants {
    public static final String SECURE_PROTOCOL = "https://";
    public static final String DEV_DOMAIN = ".vault.skyflowapis.dev";
    public static final String STAGE_DOMAIN = ".vault.skyflowapis.tech";
    public static final String SANDBOX_DOMAIN = ".vault.skyflowapis-preview.com";
    public static final String PROD_DOMAIN = ".vault.skyflowapis.com";
    public static final String HTTPS_PROTOCOL = "https";
    public static final String PKCS8_PRIVATE_HEADER = "-----BEGIN PRIVATE KEY-----";
    public static final String PKCS8_PRIVATE_FOOTER = "-----END PRIVATE KEY-----";
    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    public static final String SIGNED_DATA_TOKEN_PREFIX = "signed_token_";
    public static final String ORDER_ASCENDING = "ASCENDING";
    public static final String API_KEY_REGEX = "^sky-[a-zA-Z0-9]{5}-[a-fA-F0-9]{32}$";
    public static final String ENV_CREDENTIALS_KEY_NAME = "SKYFLOW_CREDENTIALS";
    public static final String SDK_NAME = "Skyflow Java SDK";
    public static final String DEFAULT_SDK_VERSION = "v2";
    public static final String SDK_VERSION;
    public static final String SDK_PREFIX;
    public static final String SDK_METRIC_NAME_VERSION = "sdk_name_version";
    public static final String SDK_METRIC_NAME_VERSION_PREFIX = "skyflow-java@";
    public static final String SDK_METRIC_CLIENT_DEVICE_MODEL = "sdk_client_device_model";
    public static final String SDK_METRIC_CLIENT_OS_DETAILS = "sdk_client_os_details";
    public static final String SDK_METRIC_RUNTIME_DETAILS = "sdk_runtime_details";
    public static final String SDK_METRIC_RUNTIME_DETAILS_PREFIX = "Java@";
    public static final String SDK_AUTH_HEADER_KEY = "x-skyflow-authorization";
    public static final String SDK_METRICS_HEADER_KEY = "sky-metadata";
    public static final String REQUEST_ID_HEADER_KEY = "x-request-id";
    public static final String ERROR_FROM_CLIENT_HEADER_KEY = "error-from-client";
    public static final String PROCESSED_FILE_NAME_PREFIX = "processed-";
    public static final String DEIDENTIFIED_FILE_PREFIX = "deidentified";
    public static final class HttpHeader {
        public static final String CONTENT_TYPE = "content-type";
        public static final String CONTENT_TYPE_JSON = "application/json";
        public static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
        public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
        public static final String ACCEPT = "Accept";
        public static final String ACCEPT_ALL = "*/*";
        public static final String CONTENT_DISPOSITION = "Content-Disposition";
        public static final String BOUNDARY_SEPARATOR = "; boundary=";
        public static final String FORM_DATA_HEADER = ": form-data; name=\"";
        
        private HttpHeader() {
            // Utility class constructor
        }
    }
    
    public static final class EncodingType {
        public static final String BASE64 = "base64";
        public static final String UTF_8 = "utf-8";
        public static final String BINARY = "binary";
        public static final String UTF_8_CHARSET = "UTF-8";
        
        private EncodingType() {
            // Utility class constructor
        }
    }
    
    public static final class FileFormatType {
        public static final String TXT = "txt";
        public static final String MP3 = "mp3";
        public static final String WAV = "wav";
        public static final String PDF = "pdf";
        public static final String JPG = "jpg";
        public static final String JPEG = "jpeg";
        public static final String PNG = "png";
        public static final String BMP = "bmp";
        public static final String TIF = "tif";
        public static final String TIFF = "tiff";
        public static final String PPT = "ppt";
        public static final String PPTX = "pptx";
        public static final String CSV = "csv";
        public static final String XLS = "xls";
        public static final String XLSX = "xlsx";
        public static final String DOC = "doc";
        public static final String DOCX = "docx";
        public static final String JSON = "json";
        public static final String XML = "xml";
        
        private FileFormatType() {
            // Utility class constructor
        }
    }
    
    public static final class DetectStatus {
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILED = "FAILED";
        public static final String IN_PROGRESS = "IN_PROGRESS";
        
        private DetectStatus() {
            // Utility class constructor
        }
    }
    
    public static final class FileProcessing {
        public static final String ENTITIES = "entities";
        
        private FileProcessing() {
            // Utility class constructor
        }
    }
    
    public static final class FieldNames {
        public static final String SKYFLOW_ID_CAMEL = "skyflowId";
        public static final String TOKENIZED_DATA = "tokenizedData";
        
        private FieldNames() {
            // Utility class constructor
        }
    }
    
    public static final class FormData {
        public static final String BOUNDARY_SEPARATOR = "--";
        
        private FormData() {
            // Utility class constructor
        }
    }

    public static final class JsonFieldNames {
        public static final String ERROR = "error";
        public static final String MESSAGE = "message";
        public static final String GRPC_CODE = "grpc_code";
        public static final String HTTP_STATUS = "http_status";
        public static final String DETAILS = "details";
        public static final String BODY = "Body";
        public static final String TOKENS = "tokens";
        public static final String RECORDS = "records";
        public static final String FIELDS = "fields";
        public static final String VALUE = "value";
        public static final String ERRORS = "errors";
        public static final String TYPE = "type";
        public static final String UPDATED_FIELD = "updatedField";
        public static final String REQUEST_INDEX = "requestIndex";
        public static final String HTTP_CODE = "httpCode";
        public static final String REQUEST_ID = "requestId";
        public static final String EXP = "exp";
        public static final String SKYFLOW_ID = "skyflow_id";
        
        private JsonFieldNames() {
            // Utility class constructor
        }
    }

    public static final class CredentialFields {
        public static final String PRIVATE_KEY = "privateKey";
        public static final String CLIENT_ID = "clientID";
        public static final String KEY_ID = "keyID";
        public static final String TOKEN_URI = "tokenURI";
        
        private CredentialFields() {
            // Utility class constructor
        }
    }

    public static final class JwtClaims {
        public static final String ISS = "iss";
        public static final String KEY = "key";
        public static final String AUD = "aud";
        public static final String SUB = "sub";
        public static final String CTX = "ctx";
        public static final String IAT = "iat";
        public static final String TOK = "tok";
        public static final String SDK = "sdk";
        
        private JwtClaims() {
            // Utility class constructor
        }
    }

    public static final class ApiToken {
        public static final String TOKEN = "token";
        public static final String ROLE_PREFIX = " role:";
        
        private ApiToken() {
            // Utility class constructor
        }
    }

    public static final class CredentialTypeValues {
        public static final String FILE = "FILE";
        public static final String STRING = "STRING";
        
        private CredentialTypeValues() {
            // Utility class constructor
        }
    }

    public static final class CryptoAlgorithm {
        public static final String RSA = "RSA";
        
        private CryptoAlgorithm() {
            // Utility class constructor
        }
    }

    public static final class HttpUtility {
        public static final String FORM_ENCODE_SEPARATOR = "=";
        public static final String FORM_ENCODE_DELIMITER = "&";
        public static final String REQUEST_ID_DELIMITER = ",";
        public static final String REQUEST_ID_PREFIX = " - requestId: ";
        public static final String ERROR_DESCRIPTION = "replace with description";
        
        private HttpUtility() {
            // Utility class constructor
        }
    }

    public static final class SystemProperty {
        public static final String OS_NAME = "os.name";
        public static final String OS_VERSION = "os.version";
        public static final String JAVA_VERSION = "java.version";
        public static final String JAVA_TEMP_DIR = "java.io.tmpdir";
        
        private SystemProperty() {
            // Utility class constructor
        }
    }

    public static final class UrlFormat {
        public static final String PROTOCOL_HOST_FORMAT = "%s://%s";
        
        private UrlFormat() {
            // Utility class constructor
        }
    }

    public static final class FileExtension {
        public static final String JSON = ".json";
        
        private FileExtension() {
            // Utility class constructor
        }
    }

    static {
        String sdkVersion;
        // Use a static initializer block to read the properties file
        Properties properties = new Properties();
        try (InputStream input = Constants.class.getClassLoader().getResourceAsStream("sdk.properties")) {
            if (input == null) {
                sdkVersion = DEFAULT_SDK_VERSION;
            } else {
                properties.load(input);
                sdkVersion = properties.getProperty("sdk.version", DEFAULT_SDK_VERSION);
            }
        } catch (IOException ex) {
            sdkVersion = DEFAULT_SDK_VERSION;
        }
        SDK_VERSION = sdkVersion;
        SDK_PREFIX = SDK_NAME + " " + SDK_VERSION;
    }
}
