package com.qiniu.cl.rs;

import org.json.JSONObject;

public class CallRet {
	protected int statusCode;
	protected String response;
	protected Exception exception;
	protected String hash;
	protected String key;

	public CallRet(int statusCode, String response) {
		this.statusCode = statusCode;
		this.response = response;
		try {
			unmarshal();
		} catch (Exception e) {
			this.exception = e;
		}
	}

	public CallRet(int statusCode, Exception e) {
		this.statusCode = statusCode;
		this.exception = e;
	}

	public boolean ok() {
		return this.statusCode / 100 == 2 && this.exception == null;
	}

	protected void unmarshal() throws Exception {
		JSONObject jsonObject = new JSONObject(this.response);
		unmarshal(jsonObject);
	}

	protected void unmarshal(JSONObject jsonObject) throws Exception {
		hash = jsonObject.optString("hash", null);
		key = jsonObject.optString("key", null);
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getResponse() {
		return response;
	}

	public Exception getException() {
		return exception;
	}

	public String getHash() {
		return hash;
	}

	public String getKey() {
		return key;
	}

	public String toString() {
		return "{\"statusCode\":\"" + statusCode + "\",\"response\":\""
				+ response + "\",\"exception\":\"" + exception + "\"}";
	}

}
