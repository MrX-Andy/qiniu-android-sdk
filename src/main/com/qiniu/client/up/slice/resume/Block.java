package com.qiniu.client.up.slice.resume;

import java.util.Date;

public class Block {
	private int idx;
	private String ctx;
	private long time;
	private String key;
	
	public Block(int idx, String ctx){
		this(idx, ctx, new Date().getTime());
	}
	
	public Block(int idx, String ctx, long time){
		this.idx = idx;
		this.ctx = ctx;
		this.time = time;
	}
	
	public int getIdx() {
		return idx;
	}

	public String getCtx() {
		return ctx;
	}

	public long getTime() {
		return time;
	}

	public String getKey() {
		return key;
	}
}
