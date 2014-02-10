package com.qiniu.client.up.slice;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.qiniu.client.auth.Authorizer;
import com.qiniu.client.ex.CallRetException;
import com.qiniu.client.rs.ChunkUploadCallRet;
import com.qiniu.client.rs.UploadResultCallRet;
import com.qiniu.client.up.Upload;
import com.qiniu.client.up.slice.resume.Block;
import com.qiniu.client.up.slice.resume.NonResume;
import com.qiniu.client.up.slice.resume.Resumable;
import com.qiniu.client.util.HttpHelper;
import com.qiniu.client.util.Util;


/**
 * 资源分块分片上传到服务器
 *
 */
public abstract class SliceUpload extends Upload{
    /// 七牛服务器要求固定为4M
    protected static final int BLOCK_SIZE = 1024 * 1024 * 4;

    /** 错误后尝试次数 */
    public static int triedTimes = 3;
    /** 读入内存的 BLOCK 最大个数 */
    public int streamBlockLimit = 10;
    /// 断点续传记录实例
    private Resumable resume;
        
    private Exception taskException;

	private int blockCount;
    
	public SliceUpload(ThreadPoolExecutor threadPool, Authorizer authorizer,
			long contentLength, String key, String mimeType) {
		super(threadPool, authorizer, contentLength, key, mimeType);
		this.blockCount = (int)((contentLength + BLOCK_SIZE - 1) / BLOCK_SIZE);
	}

	public UploadResultCallRet execute(){
		initReusme();
        List<Future<ChunkUploadCallRet>> fs = new LinkedList<Future<ChunkUploadCallRet>>();
        
        try {
        	// 启动监控
            Thread monitor = monitor(fs, blockCount, threadPool);
            // 分块上传
            for(int i=0; i< blockCount; i++){
                long start = i * BLOCK_SIZE;
                int len = (int) Math.min(BLOCK_SIZE, contentLength - start);
                submitCall(threadPool, i, start, len, fs);
                checkThreadException();
            }

            // 等待所有块上传完成或产生异常 
            // 监控在所有块上传完成或产生异常时停止
            try {
                monitor.join();
            } catch (Exception e) {
                e.printStackTrace();
            }

            checkThreadException();

            // 创建文件
            String ctx = mkCtx();
            UploadResultCallRet ret = mkfile(ctx, 0);
            cleanResume();
            return ret;
        }catch (CallRetException e){
        	e.printStackTrace();
        	saveResume();
            return e.getRet(UploadResultCallRet.class);
        } catch (Exception e) {
        	e.printStackTrace();
        	saveResume();
            return new UploadResultCallRet(400, e);
        } finally {
            clearAll();
            done = true;
        }
    }

   
    /**
     * 监控直到所有块上传完成，或 有块上传失败
     * 监控在所有块上传完成或产生异常时停止
     * @param fs
     * @param blockCount
     * @param threadPool
     * @return
     */
    private Thread monitor(final List<Future<ChunkUploadCallRet>> fs, 
    		final int blockCount, final ThreadPoolExecutor threadPool){
        Thread th = new Thread(){
            @Override
            public void run(){
            	// fs 会逐渐添加,直到处理所有的块上传任务,需要while
            	while(true){
                    Util.sleep(800);
                    int doneCount = resume.doneCount();
                   	for (int i = 0; i < fs.size();) {
                       Future<ChunkUploadCallRet> f = fs.get(i);
                        if (f != null && f.isDone()){
                            try {
                                ChunkUploadCallRet ret = f.get();
                                Util.checkCallRet(ret);
                                doneCount++;
                                setResume(ret);
                                fs.remove(i);
                            } catch (Exception e) {
                                taskException = e;
                                return;
                            }
                        }else{
                        	i++;
                        }
                    }
                    if(doneCount == blockCount){
                        return;
                    }
                    saveResume();
                }
            }
        };
        th.setDaemon(true);
        th.start();
        return th;
    }

    private void submitCall(ThreadPoolExecutor threadPool, int blockIdx, long start, int len,
                            List<Future<ChunkUploadCallRet>> fs) throws IOException {
    	if(!resume.isBlockDone(blockIdx)){
	        UploadBlock upload = buildBlockUpload(blockIdx, start, len);
	        Future<ChunkUploadCallRet> f = threadPool.submit(upload);
	        fs.add(f);
    	}
    }
    
    private String mkCtx() throws Exception{
        StringBuilder sb = new StringBuilder();
		for(int i=0; i< this.blockCount; i++){
			Block b = resume.getBlock(i);
			sb.append(",").append(b.getCtx());
		}
		String ctx = sb.substring(1);
        return ctx;
    }
    
    private UploadResultCallRet mkfile(String ctx, int time) throws Exception {
        try{
            String url = buildMkfileUrl();
            HttpPost post = HttpHelper.buildUpPost(url, authorizer);
            post.setEntity(new StringEntity(ctx));
            HttpResponse response = httpClient.execute(post);
            UploadResultCallRet ret =  Util.wrap(UploadResultCallRet.class, Util.handleResult(response));
            Util.checkCallRet(ret);
            return ret;
       } catch(CallRetException e){
        	if (time < triedTimes) {
                return mkfile(ctx, time + 1);
            }
        	throw e;
        }
    }

    private String buildMkfileUrl() {
        String url = host + "/mkfile/" + contentLength + "/key/" + Util.encodeBase64URLSafeString(key);
        if(null != mimeType && !(mimeType.trim().length() == 0)){
            url += "/mimeType/" + Util.encodeBase64URLSafeString(mimeType);
        }
        return url;
    }
    
    protected int getMemeryBlockCount(){
    	return Math.min(streamBlockLimit, threadPool.getMaximumPoolSize())
    			- threadPool.getActiveCount();
    }
    
	/**
	 * 当指定到断点记录类型为null,或指定的断点记录创建失败时,
	 * 使用默认的断点记录类型: 不保存断点记录
	 */
	protected void initReusme() {
    	try {
    		if(resumeClass != null){
    			resume = resumeClass.getConstructor(int.class, String.class)
    					.newInstance(blockCount, key);
    		}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	// 默认为不保存断点记录
    	if(resume == null){
    		resume = new NonResume(blockCount, key);
    	}
		try{
			resume.load();
			lastUploadLength = resume.doneCount() * BLOCK_SIZE;
		}catch(Exception e1){
			e1.printStackTrace();
		}
	}
    
    private void cleanResume(){
    	try {
	    	if(resume != null){
	    		resume.clean();
	    	}
    	} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    
    private void setResume(ChunkUploadCallRet ret){
    	if(resume != null && !resume.isBlockDone(ret.getBlockIdx())){
    		Block block = new Block(ret.getBlockIdx(), ret.getCtx(), true);
    	    resume.set(block);
    	}
    }
    
    private void saveResume(){
    	try {
	    	if(resume != null){
	    		resume.save();
	    	}
    	} catch (Exception e) {
			e.printStackTrace();
		}
    }

	private void checkThreadException() throws Exception{
        if(taskException != null){
            throw Util.exceptionCause(taskException, CallRetException.class, 0);
        }
    }


    private void clearAll(){
        try{
            mimeType = null;
            host = null;
            key = null;
            clean();
        }catch(Exception e){
        	
        }
    }
    
    protected void addSuccessLength(long size){
        super.addSuccessLength(size);
    }

    protected abstract UploadBlock buildBlockUpload(int blockIdx, long start, int len) throws IOException;

    protected  abstract void clean();
    
    public Resumable getResume(){
		return resume;
	}
    
}
