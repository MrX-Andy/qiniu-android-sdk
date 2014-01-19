package com.qiniu.cl.rs;

import org.json.JSONObject;

public class ChunkCallRet extends CallRet {
	protected String ctx;
	protected String checksum;
	protected long offset;
	protected String host;
	protected String crc32;

	public ChunkCallRet(int statusCode, String response) {
		super(statusCode, response);
	}
	
	public ChunkCallRet(int statusCode, Exception e) {
		super(statusCode, e);
	}
	
	@Override
	protected void unmarshal(JSONObject jsonObject) throws Exception{
		super.unmarshal(jsonObject);
		ctx = jsonObject.optString("ctx", null);
		checksum = jsonObject.optString("checksum", null);
		offset = jsonObject.optLong("offset", 0);
		host = jsonObject.optString("host", null);
		crc32 = jsonObject.optString("crc32", null);
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

	public String getCrc32() {
		return crc32;
	}


    public boolean canRetry() {
        //TODO
        return false;
    }
}
