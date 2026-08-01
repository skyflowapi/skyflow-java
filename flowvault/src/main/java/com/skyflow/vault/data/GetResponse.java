package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.HashMap;

public class GetResponse extends BaseGetResponse {
    public GetResponse(ArrayList<HashMap<String, Object>> data, ArrayList<HashMap<String, Object>> errors) {
        super(data, errors);
    }
}
