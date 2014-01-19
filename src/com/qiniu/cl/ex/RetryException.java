package com.qiniu.cl.ex;

import com.qiniu.cl.rs.ChunkCallRet;

/**
 * Created by xc on 1/17/14.
 */
public class RetryException extends Exception{
    private ChunkCallRet ret;

    public RetryException(ChunkCallRet ret) {
        this.ret = ret;
    }

    public RetryException(String detailMessage, ChunkCallRet ret) {
        super(detailMessage);
        this.ret = ret;
    }

    public RetryException(String detailMessage, Throwable throwable, ChunkCallRet ret) {
        super(detailMessage, throwable);
        this.ret = ret;
    }

    public RetryException(Throwable throwable, ChunkCallRet ret) {
        super(throwable);
        this.ret = ret;
    }

    public String toString(){
        return getRet().toString();
    }

    public ChunkCallRet getRet() {
        return ret;
    }

}
