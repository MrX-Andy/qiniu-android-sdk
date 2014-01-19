package com.qiniu.cl.util;

import android.util.Base64;

import com.qiniu.cl.conf.Config;
import com.qiniu.cl.ex.CallRetException;
import com.qiniu.cl.ex.RetryException;
import com.qiniu.cl.rs.ChunkCallRet;

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
	
    public static String base64UrlSafeEncode(String data) {
        return Base64.encodeToString(toByte(data), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    public static ChunkCallRet handleResult(HttpResponse response) {
        try {
            StatusLine status = response.getStatusLine();
            int statusCode = status.getStatusCode();
            String responseBody = EntityUtils.toString(
                    response.getEntity(), "UTF-8");
            return new ChunkCallRet(statusCode, responseBody);
        } catch (Exception e) {
            return new ChunkCallRet(400, "can not load response." + e.getMessage());
        }
    }

    public static void checkChunkCallRet(ChunkCallRet ret) throws RetryException, CallRetException {
        if(ret == null || !ret.ok()){
            if(ret.canRetry()){
                throw new RetryException(ret);
            }else{
                throw new CallRetException(ret);
            }
        }
    }

    public static void sleep(int millis){
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
        }
    }

    public static Exception ExceptionCause(Exception e, Class<?> c, int time){
        if(time <= 0 || e.getClass().equals(c)){
            return e;
        }else {
            Exception tmp = (Exception) e.getCause();
            return tmp == null ? e : ExceptionCause(tmp, c, time - 1);
        }
    }


}
