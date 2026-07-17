package com.skyflow.contracttest;

import com.skyflow.Skyflow;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.DeidentifyFileStatus;
import com.skyflow.enums.DetectEntities;
import com.skyflow.enums.DetectOutputTranscriptions;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.MaskingMethod;
import com.skyflow.enums.RedactionType;
import com.skyflow.enums.RequestMethod;
import com.skyflow.enums.TokenMode;
import com.skyflow.enums.TokenType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;
import com.skyflow.serviceaccount.util.SignedDataTokenResponse;
import com.skyflow.serviceaccount.util.SignedDataTokens;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import com.skyflow.vault.connection.InvokeConnectionResponse;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;
import com.skyflow.vault.data.FileUploadRequest;
import com.skyflow.vault.data.FileUploadResponse;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateResponse;
import com.skyflow.vault.detect.AudioBleep;
import com.skyflow.vault.detect.DateTransformation;
import com.skyflow.vault.detect.DeidentifyFileRequest;
import com.skyflow.vault.detect.DeidentifyFileResponse;
import com.skyflow.vault.detect.DeidentifyTextRequest;
import com.skyflow.vault.detect.DeidentifyTextResponse;
import com.skyflow.vault.detect.FileInput;
import com.skyflow.vault.detect.GetDetectRunRequest;
import com.skyflow.vault.detect.ReidentifyTextRequest;
import com.skyflow.vault.detect.ReidentifyTextResponse;
import com.skyflow.vault.detect.TokenFormat;
import com.skyflow.vault.detect.Transformations;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.DetokenizeData;
import com.skyflow.vault.tokens.DetokenizeRecordResponse;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.DetokenizeResponse;
import com.skyflow.vault.tokens.TokenizeRequest;
import com.skyflow.vault.tokens.TokenizeResponse;
import org.junit.Assert;
import org.junit.Test;

/**
 * Contract test: the imports above are the classes real customers import today, taken
 * directly from samples/src/main/java and README.md. This file's only real assertion is
 * that it keeps compiling - if any of these classes are renamed, moved, or removed, this
 * fails with a plain "cannot find symbol" naming the exact broken import, instead of
 * requiring anyone to read a japicmp diff report.
 */
public class SampleImportContractTest {
    @Test
    public void publicApiImportsStillCompile() {
        Assert.assertNotNull(Skyflow.class);
    }
}
