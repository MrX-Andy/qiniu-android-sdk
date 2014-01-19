package com.qiniu.cl.up;

import android.util.Log;

import com.qiniu.cl.auth.Authorizer;
import com.qiniu.cl.conf.Config;
import com.qiniu.cl.ex.CallRetException;
import com.qiniu.cl.ex.RetryException;
import com.qiniu.cl.rs.ChunkCallRet;
import com.qiniu.cl.util.Util;
import com.qiniu.cl.util.HttpHelper;
import com.qiniu.cl.util.PausableThreadPoolExecutor;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by xc on 1/16/14.
 */
public abstract class SliceUpload {
    protected String host = Config.UP_HOST;
    /// 七牛服务器要求固定为4M
    protected static final int BLOCK_SIZE = 1024 * 1024 * 4;
    /** 最大总线程数 */
    public static int threadsLimit = 6;
    /** 错误后尝试次数 */
    public static int triedTimes = 2;
    /** 读入内存的 BLOCK 最大个数,只对 StreamSlice 有效 */
    public static int streamBlockLimit;
    /** 传递到回调通知中 */
    public Object passParam;
    protected HttpClient httpClient;
    protected int threadsCount;
    /// 需要成功上传的长度
    protected long length = 0;
    protected boolean done = false;
    protected Authorizer authorizer;
	protected String key;
    protected String mimeType;

    private Lock successLengthLock = new ReentrantLock();
    private long successLength = 0;
    private Lock uploadLengthLock = new ReentrantLock();
    private long uploadLength = 0;
    private Exception threadException;

    public ChunkCallRet execute() throws RuntimeException{
        init();

        long blockCount = (length + BLOCK_SIZE - 1) / BLOCK_SIZE;
        threadsCount = (int) Math.min(blockCount, threadsLimit);

        httpClient = HttpHelper.buildHttpClient(threadsCount);

        ThreadPoolExecutor threadPool = new PausableThreadPoolExecutor(threadsCount, threadsCount, 0,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        List<Future<ChunkCallRet>> fs = new ArrayList<Future<ChunkCallRet>>();

        try {
            Thread monitor = monitor(fs, blockCount, threadPool);

            for(long i=0; i< blockCount; i++){
                long start = i * BLOCK_SIZE;
                int len = (int) Math.min(BLOCK_SIZE, length - start);
                submitCall(threadPool, start, len, fs, 0);
                checkThreadException();
            }

            try {
                monitor.join();
            } catch (Exception e) {
                e.printStackTrace();
            }

            checkThreadException();

            String ctx = mkCtx(fs);
            return mkfile(ctx, 0);
        }catch (CallRetException e){
            return e.getRet();
        } catch (Exception e) {
            return new ChunkCallRet(400, e);
        } finally {
            clearAll();
            shutDown(httpClient);
            shutDown(threadPool);
            done = true;
        }
    }


    private String mkCtx(List<Future<ChunkCallRet>> fs) throws Exception{
        StringBuffer sb = new StringBuffer();
        for (Future<ChunkCallRet> f : fs) {
            ChunkCallRet ret = f.get();
            Util.checkChunkCallRet(ret);
            sb.append(",").append(ret.getCtx());
        }
        String ctx = sb.substring(1);
        return ctx;
    }

    private void checkThreadException() throws Exception{
        if(threadException != null){
            throw Util.ExceptionCause(threadException, CallRetException.class, 2);
        }
    }

    private Thread monitor(final List<Future<ChunkCallRet>> fs, final long blockCount, final ThreadPoolExecutor threadPool){
        Thread th = new Thread(){
            @Override
            public void run(){
                while(true){
                    Util.sleep(300);
                    long doneCount = 0;
                    for (int i = 0; i < fs.size(); i++) {
                        Future<ChunkCallRet> f = fs.get(i);
                        if (f != null && f.isDone()) {
                            try {
                                ChunkCallRet ret = f.get();
                                Util.checkChunkCallRet(ret);
                                doneCount++;
                            } catch (Exception e) {
                                threadException = e;
                                return;
                            }
                        }
                        Util.sleep(100);
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


    private void submitCall(ThreadPoolExecutor threadPool, long start, int len,
                            List<Future<ChunkCallRet>> fs, int time) {
        try {
            BlockUpload upload = buildBlockUpload(start, len);
            Future<ChunkCallRet> f = threadPool.submit(upload);
            fs.add(f);
        } catch (Exception e) {
            Log.i("upload", "thread-" + Thread.currentThread().getId() + " : " + e.getMessage());
            if (time >= triedTimes) {
                throw new RuntimeException(e.getMessage(), e);
            } else {
                submitCall(threadPool, start, len, fs, time + 1);
            }
        }
    }

    private ChunkCallRet mkfile(String ctx, int time) {
        try{
            String url = buildMkfileUrl();
            HttpPost post = HttpHelper.buildUpPost(url, authorizer);
            post.setEntity(new StringEntity(ctx));
            HttpResponse response = httpClient.execute(post);
            ChunkCallRet ret =  Util.handleResult(response);
            Util.checkChunkCallRet(ret);
            return ret;
        } catch (RetryException e) {
            if (time < triedTimes) {
                return mkfile(ctx, time + 1);
            }else{
                return e.getRet();
            }
        } catch(CallRetException e){
            return e.getRet();
        } catch (Exception e) {
            return new ChunkCallRet(400, e);
        }
    }

    private String buildMkfileUrl() {
        String url = host + "/mkfile/" + length + "/key/" + Util.base64UrlSafeEncode(key);
        if(null != mimeType && !(mimeType.trim().length() == 0)){
            url += "/mimeType/" + Util.base64UrlSafeEncode(mimeType);
        }
        return url;
    }

    protected void init(){
        threadException = null;
        successLength = 0;
        uploadLength = 0;
        done = false;
    }

    private void clearAll(){
        try{
            mimeType = null;
            host = null;
            key = null;
            clean();
        }catch(Exception e){}
    }

    private void shutDown(HttpClient httpClient) {
        if(httpClient != null){
            try{
                httpClient.getConnectionManager().shutdown();
            } catch (Exception e) {

            }
        }
    }

    private void shutDown(ThreadPoolExecutor threadPool) {
        if (threadPool != null) {
            try {
                threadPool.shutdown();
            } catch (Exception e) {

            }
        }
    }

    void addSuccessLength(long size){
        successLengthLock.lock();
        try{
            successLength += size;
        }finally{
            successLengthLock.unlock();
        }
    }

    void addUploadLength(long size){
        uploadLengthLock.lock();
        try{
            uploadLength += size;
        }finally{
            uploadLengthLock.unlock();
        }
    }

    /**已成功上传的数据量*/
    public long getSuccessLength() {
        return successLength;
    }

    /**上传的数据量,包含重复上传的数据,不含协议报头的数据量*/
    public long getUploadLength() {
        return uploadLength;
    }

    public long getContentLength() {
        return length;
    }

    public boolean isDone(){
        return done;
    }
    
    protected abstract BlockUpload buildBlockUpload(long start, int len) throws IOException;

    protected  abstract void clean();
}
