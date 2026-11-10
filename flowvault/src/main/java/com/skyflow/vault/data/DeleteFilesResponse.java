package com.skyflow.vault.data;

import java.util.List;

/** One record per resolved record; a record addressed by uniqueValues can resolve to several. */
public class DeleteFilesResponse extends BaseDeleteFilesResponse<DeleteFilesResponseRecord> {
    public DeleteFilesResponse(List<DeleteFilesResponseRecord> records) {
        super(records);
    }
}
