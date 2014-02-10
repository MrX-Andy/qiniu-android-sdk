package com.qiniu.client.up;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.http.client.HttpClient;

import com.qiniu.client.rs.UploadResultCallRet;
import com.qiniu.client.up.slice.resume.Resumable;
import com.qiniu.client.util.HttpHelper;
import com.qiniu.client.util.Util;

/**
 * 单个资源上传管理器
 * @author xc
 *
 */
public class UpApi implements Runnable {
	/** 最大总线程数 */
    public static int threadsLimit = 3;
	protected Upload upload;
	protected UploadHandler handler;
	protected HttpClient httpClient;
	protected ThreadPoolExecutor threadPool;
	protected Class<? extends Resumable> resume;
	
	/** 
	 * 外部管理器:当UpApi为外部管理容器创建时,设置此参数
	 * 当此outerThreadPoolManager为false,上传完成将关闭线程
	 *  */
	protected boolean outerThreadPoolManager = false;
	
	/** 
	 * 外部管理器:当UpApi为外部管理容器创建时,设置此参数
	 * 当此outerHttpClientManager为false,上传完成将关闭http链接
	 *  */
	protected boolean outerHttpClientManager = false;
	
	protected UpApi(Upload upload, UploadHandler handler) {
		this(upload, handler, null);
	}
	
	protected UpApi(Upload upload, UploadHandler handler, HttpClient httpClient) {
		this.upload = upload;
		this.handler = handler;
		this.httpClient = httpClient;
	}
	
	public static Thread execute(Upload upload, UploadHandler handler){
		return new UpApi(upload, handler).execute();
	}
	
	public static Thread execute(Upload upload, UploadHandler handler, Class<? extends Resumable> resumeClass){
		UpApi upapi = new UpApi(upload, handler);
		upapi.setResumable(resumeClass);
		return upapi.execute();
	}

	public Thread execute() {
		Thread t = new Thread(this);
		t.start();
		return t;
	}

	@Override
	public void run() {
		try {
			UploadTask uploadTask = new UploadTask();
			FutureTask<UploadResultCallRet> task = new FutureTask<UploadResultCallRet>(
					uploadTask);
			new Thread(task).start();

			while (upload != null && !upload.isDone()) {
				Util.sleep(800);
				onProcess();
			}

			while (!task.isDone()) {
				Util.sleep(50);
			}
			UploadResultCallRet ret = task.get();
			Util.checkCallRet(ret);
			onSuccess(ret);
		} catch (Exception e) {
			onFailure(e);
		}finally{
			clearThreadPool();
			clearHttpClient();
		}
	}

	private void onProcess() {
		if(handler != null){
			buildParams();
			handler.sendUploading();
		}
	}
	
	private void onSuccess(UploadResultCallRet ret) {
		if(handler != null){
			buildParams();
			handler.sendFinished(ret);
		}
	}
	
	private void onFailure(Exception e) {
		if(handler != null){
			buildParams();
			handler.sendFailed(e);
		}
	}


	private void buildParams() {
		handler.setPassParam(upload.passParam);
		handler.setCurrentUploadLength(upload.getCurrentUploadLength());
		handler.setLastUploadLength(upload.getLastUploadLength());
		handler.setContentLength(upload.getContentLength());
	}


	/**
	 * 运行时才初始化变量
	 * @author xc
	 *
	 */
	private class UploadTask implements Callable<UploadResultCallRet> {

		@Override
		public UploadResultCallRet call() throws Exception {
			initThreadPool();
			initHttpClient();
			upload.setHttpClient(httpClient);
			upload.setThreadPool(threadPool);
			upload.setResumable(resume);
			return upload.execute();
		}

	}
	
	private void initThreadPool() {
		if (threadPool == null || threadPool.isShutdown()
				|| threadPool.isTerminated() || threadPool.isTerminating()) {
			threadPool = Util.buildDefaultThreadPool(threadsLimit);
		}
	}
	
	private void initHttpClient() {
		if(httpClient == null){
			httpClient = HttpHelper.getHttpClient();
		}
	}
	
	private void clearThreadPool(){
		if(!outerThreadPoolManager){
			threadPool.shutdown();
		}
	}
	
	private void clearHttpClient(){
		if(!outerHttpClientManager){
			httpClient.getConnectionManager().shutdown();
		}
	}

	public Object getOuterThreadPoolManager() {
		return outerThreadPoolManager;
	}

	/** 
	 * 外部管理器:当UpApi为外部管理容器创建时,设置此参数
	 * 当此outerThreadPoolManager为null,上传完成将关闭线程
	 *  */
	public void setOuterThreadPoolManager(boolean outerThreadPoolManager) {
		this.outerThreadPoolManager = outerThreadPoolManager;
	}

	public Object getOuterHttpClientManager() {
		return outerHttpClientManager;
	}

	/** 
	 * 外部管理器:当UpApi为外部管理容器创建时,设置此参数
	 * 当此outerHttpClientManager为null,上传完成将关闭http链接
	 *  */
	public void setOuterHttpClientManager(boolean outerHttpClientManager) {
		this.outerHttpClientManager = outerHttpClientManager;
	}
	
	public void setResumable(Class<? extends Resumable> resumeClass) {
		this.resume = resumeClass;
	}
	
	public Class<? extends Resumable> getResumable() {
		return resume;
	}
    

}
