package com.qiniu.client.up.slice.resume;

import java.util.LinkedList;
import java.util.List;

public abstract class BaseResume implements Resumable {

	private int blockCount;
	private List<Block> blocks;
	
	public BaseResume(int blockCount){
		this.blockCount = blockCount;
		blocks = new LinkedList<Block>();
		this.load();
	}
	
	@Override
	public int blockCount() {
		return blockCount;
	}
	
	@Override
	public boolean isDone() {
		return this.blockCount == blocks.size();
	}

	@Override
	public boolean isBlockDone(int idx) {
		return blocks.get(idx) != null;
	}

	@Override
	public String getCtx(int idx) {
		Block block = blocks.get(idx);
		if(block != null){
			return block.getCtx();
		}else{
			return null;
		}
	}

	@Override
	public String getCtxes() {
		StringBuilder sb = new StringBuilder();
		for(Block b : blocks){
			sb.append(",").append(b.getCtx());
		}
		String s = sb.substring(1);
		return s;
	}
	
	@Override
	public void add(Block block){
		blocks.add(block.getIdx(), block);
	}
	
	@Override
	public void remove(Block block){
		blocks.remove(block.getIdx());
	}


}
