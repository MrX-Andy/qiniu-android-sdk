package com.qiniu.client.up.slice.resume;

public abstract class BaseResume implements Resumable {

	protected int blockCount;
	private Block[] blocks;
	
	public BaseResume(int blockCount){
		this.blockCount = blockCount;
		initBlocks();
		this.load();
	}
	
	private void initBlocks(){
		blocks = new Block[blockCount];

	}
	
	@Override
	public int blockCount() {
		return blockCount;
	}
	
	@Override
	public boolean isDone() {
		return this.blockCount == blocks.length;
	}

	@Override
	public boolean isBlockDone(int idx) {
		return blocks[idx] != null;
	}
	
	@Override
	public Block getBlock(int idx) {
		return blocks[idx];
	}

	@Override
	public String getCtx(int idx) {
		Block block = blocks[idx];
		if(block != null){
			return block.getCtx();
		}else{
			return null;
		}
	}

	@Override
	public void set(Block block){
		blocks[block.getIdx()] = block;
	}
	
	@Override
	public void drop(Block block){
		blocks[block.getIdx()] = null;
	}


}
