package com.skyflow.vault.tokens;

import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;

import java.util.ArrayList;

public class DetokenizeRequest {
    private final DetokenizeRequestBuilder builder;

    private DetokenizeRequest(DetokenizeRequestBuilder builder) {
        this.builder = builder;
    }

    public static DetokenizeRequestBuilder builder() {
        return new DetokenizeRequestBuilder();
    }

    public ArrayList<DetokenizeData> getDetokenizeData() {
        return this.builder.detokenizeData;
    }

    public Boolean getContinueOnError() {
        return this.builder.continueOnError;
    }

    /**
     * @deprecated Use {@link #getDownloadUrl()} instead.
     */
    @Deprecated(since = "2.1", forRemoval = true)
    public Boolean getDownloadURL() {
        LogUtil.printWarningLog(InfoLogs.DEPRECATED_DOWNLOAD_URL.getLog());
        return getDownloadUrl();
    }

    public Boolean getDownloadUrl() {
        return this.builder.downloadUrl;
    }

    public static final class DetokenizeRequestBuilder {
        private ArrayList<DetokenizeData> detokenizeData;
        private Boolean continueOnError;
        private Boolean downloadUrl;

        private DetokenizeRequestBuilder() {
            this.continueOnError = false;
            this.downloadUrl = false;
        }

        public DetokenizeRequestBuilder detokenizeData(ArrayList<DetokenizeData> detokenizeData) {
            this.detokenizeData = detokenizeData;
            return this;
        }

        public DetokenizeRequestBuilder continueOnError(Boolean continueOnError) {
            this.continueOnError = continueOnError != null && continueOnError;
            return this;
        }

        /**
         * @deprecated Use {@link #downloadUrl(Boolean)} instead.
         */
        @Deprecated(since = "2.1", forRemoval = true)
        public DetokenizeRequestBuilder downloadURL(Boolean downloadURL) {
            LogUtil.printWarningLog(InfoLogs.DEPRECATED_DOWNLOAD_URL.getLog());
            return downloadUrl(downloadURL);
        }

        public DetokenizeRequestBuilder downloadUrl(Boolean downloadUrl) {
            this.downloadUrl = downloadUrl;
            return this;
        }

        public DetokenizeRequest build() {
            return new DetokenizeRequest(this);
        }
    }
}
