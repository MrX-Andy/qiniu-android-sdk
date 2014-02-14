package com.qiniu.client.up;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.client.HttpClient;

import com.qiniu.client.auth.Authorizer;
import com.qiniu.client.config.Config;
import com.qiniu.client.rs.UploadResultCallRet;
import com.qiniu.client.up.InputParam.FileInputParam;
import com.qiniu.client.up.InputParam.StreamInputParam;
import com.qiniu.client.up.normal.FileNormalUpload;
import com.qiniu.client.up.normal.StreamNormalUpload;
import com.qiniu.client.up.slice.RandomAccessFileUpload;
import com.qiniu.client.up.slice.StreamSliceUpload;
import com.qiniu.client.up.slice.resume.Resumable;

/**
 * 资源上传基类
 * @author xc
 *
 */
public abstract class Upload {
	/** 资源大于此值的将采用分片上传,小于等于的直传. */
    public static int sliceShed = 1024 * 1024 * 4;
    
    public static Upload buildUpload(Authorizer authorizer, FileInputParam p) {
		Upload up = null;
		if(p.size > sliceShed){
			up = new RandomAccessFileUpload(p.file, authorizer, p.size,
						p.name, p.mimeType);
		}else{
			up = new FileNormalUpload(p.file, authorizer, p.size,
					p.name, p.mimeType);
		}
		
		up.passParam = p;
		return up;
	}
	
    public static Upload buildUpload(Authorizer authorizer, StreamInputParam p) {
		Upload up = null;
		if(p.size > sliceShed){
			up = new StreamSliceUpload(p.is, authorizer, p.size,
					p.name, p.mimeType);
		}else{
			up = new StreamNormalUpload(p.is, authorizer, p.size,
					p.name, p.mimeType);
		}
		up.passParam = p;
		return up;
	}
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
	protected long lastUploadLength = 0;
    
	private Lock successLengthLock = new ReentrantLock();
    private long currentUploadLength = 0;
    
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
            currentUploadLength += size;
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
    public long getCurrentUploadLength() {
        return currentUploadLength;
    }

    public long getContentLength() {
        return contentLength;
    }
    
    public long getLastUploadLength() {
		return lastUploadLength;
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
