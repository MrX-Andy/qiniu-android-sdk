package com.qiniu.client.up.slice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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
 * @author xc
 *
 */
public abstract class SliceUpload extends Upload{
    /// 七牛服务器要求固定为4M
    protected static final int BLOCK_SIZE = 1024 * 1024 * 4;

    /** 错误后尝试次数 */
    public static int triedTimes = 3;
    /** 读入内存的 BLOCK 最大个数 */
    public int streamBlockLimit = 3;
    private Resumable resume;
        
    private Exception taskException;

	private int blockCount;
    
	public SliceUpload(ThreadPoolExecutor threadPool, Authorizer authorizer,
			long contentLength, String key, String mimeType) {
		super(threadPool, authorizer, contentLength, key, mimeType);
		this.blockCount = (int)((contentLength + BLOCK_SIZE - 1) / BLOCK_SIZE);
	}
	
	public SliceUpload(ThreadPoolExecutor threadPool, Resumable resume, Authorizer authorizer,
			long contentLength, String key, String mimeType) {
		super(threadPool, authorizer, contentLength, key, mimeType);
		this.resume = resume;
		this.blockCount = (int)((contentLength + BLOCK_SIZE - 1) / BLOCK_SIZE);
	}

	public UploadResultCallRet execute(){
		initReusme();
        List<Future<ChunkUploadCallRet>> fs = new ArrayList<Future<ChunkUploadCallRet>>();
        
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
            String ctx = mkCtx(fs);
            return mkfile(ctx, 0);
        }catch (CallRetException e){
        	e.printStackTrace();
            return e.getRet(UploadResultCallRet.class);
        } catch (Exception e) {
        	e.printStackTrace();
            return new UploadResultCallRet(400, e);
        } finally {
            clearAll();
            done = true;
        }
    }

    private void initReusme() {
    	try {
    		if(resumeClass != null && !NonResume.class.equals(resumeClass)){
    			resume = resumeClass.getConstructor(int.class).newInstance(blockCount);
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
            	int doneCount = 0;
            	// fs 会逐渐增多,直到包含所有的块上传任务,需要while
            	while(true){
                    Util.sleep(300);
                    // 前面的任务不一定比后面的先完成,break forCount保证先处理前面的任务
                    // 添加break和i = doneCount,减少了重复验证,即每个任务有且只验证一次
                   	for (int i = doneCount; i < fs.size(); i++) {
                       Future<ChunkUploadCallRet> f = fs.get(i);
                        if (f != null && f.isDone()) {
                            try {
                                ChunkUploadCallRet ret = f.get();
                                Util.checkCallRet(ret);
                                doneCount++;
                            } catch (Exception e) {
                                taskException = e;
                                return;
                            }
                        }else{
                        	Util.sleep(300);
                        	break;
                        }
                    }
                    if(doneCount == blockCount){
                        return;
                    }
                }
            }
        };
        th.setDaemon(true);
        th.start();
        return th;
    }


    private void submitCall(ThreadPoolExecutor threadPool, int idx, long start, int len,
                            List<Future<ChunkUploadCallRet>> fs) throws IOException {
    	if(resume != null && !resume.isBlockDone(idx)){
	        UploadBlock upload = buildBlockUpload(start, len);
	        Future<ChunkUploadCallRet> f = threadPool.submit(upload);
	        fs.add(f);
    	}
    }
    
    private String mkCtx(List<Future<ChunkUploadCallRet>> fs) throws Exception{
    	if(resume == null){
    		return mkCtx0(fs);
    	}else{
    		return mkCtx1(fs);
    	}
    }

    private String mkCtx0(List<Future<ChunkUploadCallRet>> fs) throws Exception{
        StringBuffer sb = new StringBuffer();
        for (Future<ChunkUploadCallRet> f : fs) {
            ChunkUploadCallRet ret = f.get();
            Util.checkCallRet(ret);
            sb.append(",").append(ret.getCtx());
        }
        String ctx = sb.substring(1);
        return ctx;
    }
    
    private String mkCtx1(List<Future<ChunkUploadCallRet>> fs) throws Exception{
    	// 不用处理f为空：块数量不正确即以为上传有误
    	Iterator<Future<ChunkUploadCallRet>> ifs = fs.iterator();
    	for(int i=0; i< this.blockCount; i++){
    		if(!resume.isBlockDone(i)){
    			Future<ChunkUploadCallRet> f = ifs.next();
    			ChunkUploadCallRet ret = f.get();
    			Util.checkCallRet(ret);
                Block block = new Block(i, ret.getCtx());
                resume.set(block);
    		}
    	}
    	if(ifs.hasNext()){
    		throw new CallRetException(new UploadResultCallRet(400, "wrong block count."));
    	}
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

    protected abstract UploadBlock buildBlockUpload(long start, int len) throws IOException;

    protected  abstract void clean();
    
    public Resumable getResume(){
		return resume;
	}
    
}
