package com.qiniu.client.up;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.client.HttpClient;

import com.qiniu.client.auth.Authorizer;
import com.qiniu.client.config.Config;
import com.qiniu.client.rs.UploadResultCallRet;
import com.qiniu.client.up.slice.resume.Resumable;

/**
 * 资源上传基类
 * @author xc
 *
 */
public abstract class Upload {
	public abstract UploadResultCallRet execute();
	
	public String host = Config.UP_HOST;
	protected HttpClient httpClient;
	
	/** 传递到回调通知中 */
    public Object passParam;
    /** 上传凭证生成器 */
    protected Authorizer authorizer;
	protected String key;
    protected String mimeType;
	
    // 只对分块上传有效
    protected ThreadPoolExecutor threadPool;
    // 只对分块上传有效
    protected Class<? extends Resumable> resumeClass;
    
	protected long contentLength = 0;
    private Lock successLengthLock = new ReentrantLock();
    private long successLength = 0;
    
    protected boolean done = false;
    
    public Upload(Authorizer authorizer,
			long contentLength, String key, String mimeType) {
    	this(null, authorizer, contentLength, key, mimeType);
    }
    
    public Upload(ThreadPoolExecutor threadPool, Authorizer authorizer,
			long contentLength, String key, String mimeType) {
		this.contentLength = contentLength;
		this.authorizer = authorizer;
		this.key = key;
		this.mimeType = mimeType;
		this.threadPool = threadPool; 
    }
    
    public void setHttpClient(HttpClient httpClient){
    	this.httpClient = httpClient;
    }
    
    protected void addSuccessLength(long size){
        successLengthLock.lock();
        try{
            successLength += size;
        }finally{
            successLengthLock.unlock();
        }
    }

    public ThreadPoolExecutor getThreadPool() {
		return threadPool;
	}

	public void setThreadPool(ThreadPoolExecutor threadPool) {
		this.threadPool = threadPool;
	}
	
	public Authorizer getAuthorizer() {
		return authorizer;
	}

    /**已成功上传的数据量*/
    public long getSuccessLength() {
        return successLength;
    }

    public long getContentLength() {
        return contentLength;
    }

    public boolean isDone(){
        return done;
    }

	public void setResumable(Class<? extends Resumable> resume) {
		if(this.resumeClass == null){
			this.resumeClass = resume;
		}
	}
	
	public Class<? extends Resumable> getResumable() {
		return resumeClass;
	}
	
	public abstract Resumable getResume();
    
}
