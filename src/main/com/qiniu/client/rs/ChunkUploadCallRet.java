package com.qiniu.client.rs;

import org.json.JSONException;
import org.json.JSONObject;

public class ChunkUploadCallRet extends CallRet {
	protected String ctx;
	protected String checksum;
	protected int offset;
	protected String host;
	protected long crc32;
	
	private int blockIdx;
	
	public ChunkUploadCallRet(CallRet ret) {
		super(ret);
	}

	public ChunkUploadCallRet(int statusCode, String response) {
		super(statusCode, response);
	}
	
	public ChunkUploadCallRet(int statusCode, Exception e) {
		super(statusCode, e);
	}

	@Override
	protected void unmarshal() throws JSONException{
		JSONObject jsonObject = new JSONObject(this.response);
		ctx = jsonObject.optString("ctx", null);
		checksum = jsonObject.optString("checksum", null);
		offset = jsonObject.optInt("offset", 0);
		host = jsonObject.optString("host", null);
		crc32 = jsonObject.optLong("crc32", 0);
	}
	
	public String getCtx() {
		return ctx;
	}

	public String getChecksum() {
		return checksum;
	}

	public long getOffset() {
		return offset;
	}

	public String getHost() {
		return host;
	}

	public long getCrc32() {
		return crc32;
	}

	public int getBlockIdx() {
		return blockIdx;
	}

	public void setBlockIdx(int blockIdx) {
		this.blockIdx = blockIdx;
	}

}
