package com.skyflow.vault.tokens;

import com.skyflow.generated.rest.models.V1DetokenizeRecordResponse;
import com.skyflow.generated.rest.models.V1DetokenizeResponse;

import java.util.ArrayList;
import java.util.List;

public class DetokenizeResponse {
    private final V1DetokenizeResponse generatedResponse;
    private final ArrayList<DetokenizeRecordResponse> records;

    public DetokenizeResponse(V1DetokenizeResponse response) {
        this.generatedResponse = response;
        this.records = new ArrayList<>();
        this.createResponse();
    }

    public ArrayList<DetokenizeRecordResponse> getRecords() {
        return records;
    }

    private void createResponse() {
        List<V1DetokenizeRecordResponse> records = this.generatedResponse.getRecords();
        for (V1DetokenizeRecordResponse record : records) {
            DetokenizeRecordResponse recordResponse = new DetokenizeRecordResponse();
            recordResponse.setValue(record.getValue());
            recordResponse.setType(record.getValueType().getValue());
            this.records.add(recordResponse);
        }
    }

    @Override
    public String toString() {
        return "DetokenizeResponse {" +
                "\n\t" + toIndentedString(records) +
                "\n}";
    }

    private String toIndentedString(Object o) {
        return o.toString().replace("\n", "\n\t");
    }

    public class DetokenizeRecordResponse {
        private String value;
        private String type;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "DetokenizeRecordResponse {" +
                    "\n\t" + "value: '" + value + "'," +
                    "\n\t" + "type: '" + type + "'," +
                    "\n}";
        }
    }
}
