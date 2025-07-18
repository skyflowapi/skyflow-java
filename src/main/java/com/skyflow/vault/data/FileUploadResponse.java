package com.skyflow.vault.data;

import com.google.gson.*;

public class FileUploadResponse {
	private final String skyflowId;

	public FileUploadResponse(String skyflowId) {
		this.skyflowId = skyflowId;
	}

	public String getSkyflowId() {
		return skyflowId;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().serializeNulls().create();
		JsonObject responseObject = gson.toJsonTree(this).getAsJsonObject();
		responseObject.add("errors", null);
		return responseObject.toString();
	}
}
