package com.qiniu.client.util;

import java.lang.reflect.Constructor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import android.util.Base64;

import com.qiniu.client.ex.CallRetException;
import com.qiniu.client.ex.RetryException;
import com.qiniu.client.rs.CallRet;
import com.qiniu.client.rs.ChunkUploadCallRet;
import com.qiniu.client.config.Config;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;


public class Util {
	public static byte[] toByte(String s){
		try {
			return s.getBytes(Config.CHARSET);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
    public static String encodeBase64URLSafeString(String data) {
        return Base64.encodeToString(toByte(data), Base64.URL_SAFE | Base64.NO_WRAP);
    }
    
    public static String encodeBase64URLSafeString(byte[] binaryData) {
        return Base64.encodeToString(binaryData, Base64.URL_SAFE | Base64.NO_WRAP);
    }
    
    /** 保留尾部的“=” */
	public static byte[] encodeBase64URLSafe(byte[] binaryData) {
		return Base64.encode(binaryData, Base64.URL_SAFE | Base64.NO_WRAP);
	}
    
    public static CallRet handleResult(HttpResponse response) {
        try {
            StatusLine status = response.getStatusLine();
            int statusCode = status.getStatusCode();
            String responseBody = EntityUtils.toString(
                    response.getEntity(), Config.CHARSET);
            return new CallRet(statusCode, responseBody);
        } catch (Exception e) {
            return new CallRet(400, "can not load response." + e.getMessage());
        }
    }

	@SuppressWarnings("unchecked")
	public static <T extends CallRet> T wrap(Class<T> c, CallRet r){
		T ret = null;
		if(CallRet.class.equals(c)){
			ret = (T)r;
		}else{
			try {
				Constructor<T> cons = c.getConstructor(CallRet.class);
				ret = cons.newInstance(r);
			} catch (Exception e) {
			} 
		}
		return ret;
	}
	
	public static void checkCallRet(CallRet ret) throws CallRetException {
        if(ret == null || !ret.ok()){
        	try{
	        	CallRetException ex = (CallRetException)ret.getException();
	    		if(ex != null){
	    			throw ex;
	    		}
        	}catch(Exception e){
        		
        	}
        	throw new CallRetException(ret);
        }
    }


    public static void checkChunkCallRet(ChunkUploadCallRet ret, long crc32) throws RetryException, CallRetException {
    	checkCallRet(ret);
        if(ret.getCrc32() != crc32){
            throw new RetryException(ret);
        }
    }
    
    public static long crc32(byte[] data){
    	CRC32 crc32 = new CRC32();
    	crc32.update(data);
		return crc32.getValue();
    }

    public static PausableThreadPoolExecutor buildDefaultThreadPool(int threadsCount) {
    	PausableThreadPoolExecutor threadPool = new PausableThreadPoolExecutor(
				(threadsCount - 1)/2 + 1, threadsCount, 800, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(),
				new ThreadPoolExecutor.CallerRunsPolicy());
        return threadPool;
	}
    public static void sleep(int millis){
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
        }
    }

    public static Exception exceptionCause(Exception e, Class<?> c, int time){
        if(time <= 0 || e.getClass().equals(c)){
            return e;
        }else {
            Exception tmp = (Exception) e.getCause();
            return tmp == null ? e : exceptionCause(tmp, c, time - 1);
        }
    }
    
}
