package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * <b>Deprecation notice:</b> the {@code skyflow_id} key in each {@link #getData()} record map is
 * deprecated and will be removed in an upcoming release. Use {@code skyflowId} instead.
 * Both keys are present simultaneously in v2 for backward compatibility.
 */
public class GetResponse extends BaseGetResponse {
    public GetResponse(ArrayList<HashMap<String, Object>> data, ArrayList<HashMap<String, Object>> errors) {
        super(data, errors);
    }
}
