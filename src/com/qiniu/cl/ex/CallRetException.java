package com.qiniu.cl.ex;

import com.qiniu.cl.rs.ChunkCallRet;

/**
 * Created by xc on 1/16/14.
 */
public class CallRetException extends Exception{
    private ChunkCallRet ret;

    public CallRetException(ChunkCallRet ret) {
        this.ret = ret;
    }

    public CallRetException(String detailMessage, ChunkCallRet ret) {
        super(detailMessage);
        this.ret = ret;
    }

    public CallRetException(String detailMessage, Throwable throwable, ChunkCallRet ret) {
        super(detailMessage, throwable);
        this.ret = ret;
    }

    public CallRetException(Throwable throwable, ChunkCallRet ret) {
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
