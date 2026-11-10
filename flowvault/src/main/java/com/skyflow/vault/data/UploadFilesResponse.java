package com.skyflow.vault.data;

import java.util.List;

/** One record per requested record, in request order. */
public class UploadFilesResponse extends BaseUploadFilesResponse<UploadFilesResponseRecord> {
    public UploadFilesResponse(List<UploadFilesResponseRecord> records) {
        super(records);
    }
}
