package com.qiniu.client.up;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.HttpClient;

import com.qiniu.client.auth.Authorizer;
import com.qiniu.client.auth.BasicAuthorizer;
import com.qiniu.client.rs.UploadResultCallRet;
import com.qiniu.client.up.InputParam.FileInputParam;
import com.qiniu.client.up.InputParam.StreamInputParam;
import com.qiniu.client.up.normal.FileNormalUpload;
import com.qiniu.client.up.normal.StreamNormalUpload;
import com.qiniu.client.up.slice.RandomAccessFileUpload;
import com.qiniu.client.up.slice.StreamSliceUpload;
import com.qiniu.client.up.slice.resume.Resumable;
import com.qiniu.client.util.HttpHelper;
import com.qiniu.client.util.PausableThreadPoolExecutor;
import com.qiniu.client.util.Util;

/**
 * 多文件上传队列简单管理器
 * @author xc
 *
 */
public class UpApiSequence implements Runnable{
    /** 同时正在上传文件的最大值 */
    public static int activeFileLimit = 3;
    /** 最大线程数 */
	public static int threadsLimit = 6;
	/** 资源大于此值的将采用分片上传,小于等于的直传. */
    public static int sliceShed = 1024 * 1024 * 4;
	private PausableThreadPoolExecutor threadPool;
	private HttpClient httpClient;
	private Authorizer authorizer;
	/** 断点续传记录类类型 */
	protected Class<? extends Resumable> resume;
	/** 等待上传队列 */
	private LinkedList<UpApi> waiting;
	/** 正在执行上传队列,其最大值受activeFileLimit限制 */
	private List<UpApi> process;
	private boolean isShutdown = false;
	
	public UpApiSequence(){
		this(null);
	}
	
	public UpApiSequence(Authorizer authorizer){
		initAuthorizer();
		waiting = new LinkedList<UpApi>();
		process = new ArrayList<UpApi>();
	}
	
	/**
	 * 开启线程执行
	 * 若调用join等必须加入如超时限制
	 * @return
	 */
	public Thread execute(){
		Thread t = new Thread(this);
		t.start();
		return t;
	}
	
	@Override
	public void run() {
		try{
			initThreadPool();
			initHttpClient();
			isShutdown = false;
			while(true){
				if(isShutdown){
					break;
				}
				next();
				Util.sleep(400);
			}
		}finally{
			
		}
	}
	
	private void next(){
		if(process.size() <= activeFileLimit && !waiting.isEmpty()){
			UpApi api = waiting.pollFirst();
			process.add(api);
			api.httpClient = this.httpClient;
			api.threadPool = this.threadPool;
			api.setOuterHttpClientManager(true);
			api.setOuterThreadPoolManager(true);
			api.setResumable(resume);
			api.handler = wrapProxy(api.handler, api);
			api.execute();
		}
	}
	
	/**
	 * 待上传的文件数
	 * @return
	 */
	public int getWaitingCount(){
		return waiting.size();
	}
	
	public List<InputParam> getWaitingInputParam(){
		return getInputParams(waiting);
	}
	
	public List<InputParam> getProcessInputParam(){
		return getInputParams(process);
	}
	
	private List<InputParam> getInputParams(List<UpApi> apis){
		List<InputParam> ips = new ArrayList<InputParam>();
		for(UpApi api : apis){
			ips.add((InputParam)api.upload.passParam);
		}
		return ips;
	}
	
	/**
	 * 尝试停止执行,不开启新任务,"不"阻断已执行的任务,"不"关闭线程池
	 */
	public void tryStop(){
		isShutdown = true;
	}
	
	/**
	 * 尝试停止执行,尝试关闭线程池,"不"阻断已执行的任务
	 */
	public void tryShutDown(){
		tryStop();
		threadPool.shutdown();
	}
	
	/**
	 * 停止执行,关闭线程池,关闭http链接
	 * 
	 */
	public void tryHardShutDown(){
		tryShutDown();
		Util.sleep(500);
		threadPool.shutdownNow();
		shutdownHttpClient();
	}
	
	/**
	 * 关闭http链接
	 */
	private void shutdownHttpClient(){
		httpClient.getConnectionManager().shutdown();
	}
	
	public void tryPause(){
		if(threadPool != null){
			threadPool.pause();
		}
	}
	
	public void tryResume(){
		if(threadPool != null){
			threadPool.resume();
		}
	}
	
	private void add(UpApi api){
		waiting.add(api);
	}
	
	/**
	 * 不要在队列里加入名称内容都想同的资源.内容相同,key都为null可能导致错误.
	 * 若在队列中有相同的key,则忽略新加如的Upload
	 * @param handler
	 * @param uploads
	 */
	public void add(UploadHandler handler, Upload ...uploads){
		for(Upload up : uploads){
			if(!has(up)){
				this.add(new UpApi(up, handler));
			}
		}
	}
	
	private boolean has(Upload up) {
		return has(up, waiting) || has(up, process);
	}
	
	private boolean has(Upload up, List<UpApi> seq){
		String key = up.key;
		for(UpApi api : seq){
			if(key != null && key.equals(api.upload.key)){
				return true;
			}
		}
		return false;
	}

	public void add(UploadHandler handler, String ...files){
		for(String file : files){
			try {
				this.add(handler, buildUpload(InputParam.fileInputParam(file)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void add(UploadHandler handler, File ...files){
		for(File file : files){
			try {
				this.add(handler, buildUpload(InputParam.fileInputParam(file)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void add(UploadHandler handler, FileInputParam ...inputParams){
		for(FileInputParam p : inputParams){
			this.add(handler, buildUpload(p));
		}
	}
	
	public void add(UploadHandler handler, StreamInputParam ...inputParams){
		for(StreamInputParam p : inputParams){
			this.add(handler, buildUpload(p));
		}
	}
	
	private Upload buildUpload(FileInputParam p) {
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
	
	private Upload buildUpload(StreamInputParam p) {
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
	
	private void initAuthorizer() {
		if(authorizer == null){
			authorizer = new BasicAuthorizer();
		}
	}

	private UploadHandler wrapProxy(final UploadHandler handler, final UpApi api) {
		UploadHandler wrap = new UploadHandler(){
			
			@Override
			protected void onProcess() {
				handler.onProcess();
			}

			@Override
			protected void onSuccess(UploadResultCallRet ret) {
				process.remove(api);
				handler.onSuccess(ret);
			}

			@Override
			protected void onFailure(Exception e) {
				process.remove(api);
				handler.onFailure(e);
			}

			@Override
			public void setPassParam(Object passParam) {
				handler.setPassParam(passParam);
			}

			@Override
			public void setCurrentUploadLength(long successLength) {
				handler.setCurrentUploadLength(successLength);
			}

			@Override
			public void setContentLength(long contentLength) {
				handler.setContentLength(contentLength);
			}

			@Override
			public void setLastUploadLength(long uploadLength) {
				handler.setLastUploadLength(uploadLength);
			}

		};
		
		return wrap;
	}

	public PausableThreadPoolExecutor getThreadPool() {
		return threadPool;
	}

	public void setThreadPool(PausableThreadPoolExecutor threadPool) {
		this.threadPool = threadPool;
	}

	public HttpClient getHttpClient() {
		return httpClient;
	}

	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	public Authorizer getAuthorizer() {
		return authorizer;
	}

	public void setAuthorizer(Authorizer authorizer) {
		this.authorizer = authorizer;
	}
	public void setResumable(Class<? extends Resumable> resume) {
		if(this.resume == null){
			this.resume = resume;
		}
	}
	
	public Class<? extends Resumable> getResumable() {
		return resume;
	}
    
}
