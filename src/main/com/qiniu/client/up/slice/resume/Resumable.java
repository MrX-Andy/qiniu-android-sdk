package com.qiniu.client.up.slice.resume;

public interface Resumable {
	
	/**
	 * 总块数
	 * @return
	 */
	int blockCount();
	
	/**
	 * 整个资源是否已成功上传
	 * @return
	 */
	boolean isDone();
	
	/**
	 * 检查一个块是否已成功上传
	 * @param idx
	 * @return
	 */
	boolean isBlockDone(int idx);
	
	/**
	 * 获取指定块的控制信息ctx内容
	 * @param idx
	 * @return
	 */
	String getCtx(int idx);
	
	/**
	 * 获取完整的ctx内容，以“,”分隔，按其在源文件中的位置排序。
	 * @return
	 */
	String getCtxes();
	
	void add(Block block);
	
	void remove(Block block);
	
	/**
	 * 加载已保存的上传信息
	 */
	void load();
	
	/**
	 * 保存上传块信息
	 */
	void save();
	
	/**
	 * 清除上传信息
	 */
	void clean();
	
	/**
	 * 清除所有的上传信息
	 */
	void cleanAll();
}
