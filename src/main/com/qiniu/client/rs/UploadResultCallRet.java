package com.qiniu.client.rs;

import org.json.JSONException;
import org.json.JSONObject;

public class UploadResultCallRet extends CallRet {
	protected String hash;
	protected String key;
	
	public UploadResultCallRet(CallRet ret) {
		super(ret);
	}
	
	public UploadResultCallRet(int statusCode, Exception e) {
		super(statusCode, e);
	}

	public UploadResultCallRet(int statusCode, String response) {
		super(statusCode, response);
	}

	@Override
	protected void unmarshal() throws JSONException{
		JSONObject jsonObject = new JSONObject(this.response);
		hash = jsonObject.optString("hash", null);
		key = jsonObject.optString("key", null);
	}

	public String getHash() {
		return hash;
	}

	public String getKey() {
		return key;
	}
}
