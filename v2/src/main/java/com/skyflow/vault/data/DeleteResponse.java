package com.skyflow.vault.data;

import java.util.ArrayList;

public class DeleteResponse {
    private ArrayList<String> deletedIds;

    public DeleteResponse(ArrayList<String> deletedIds) {
        this.deletedIds = deletedIds;
    }

    public ArrayList<String> getDeletedIds() {
        return deletedIds;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\n\t\"deletedIds\": ").append(formatIds());
        return sb.append("\n}").toString();
    }

    private String formatIds() {
        StringBuilder sb = new StringBuilder("[");
        for (String id : deletedIds) {
            sb.append("\n\t\"").append(id).append("\"");
        }
        return toIndentedString(sb.append("]"));
    }

    private String toIndentedString(Object o) {
        return o.toString().replace("\n", "\n\t");
    }
}
