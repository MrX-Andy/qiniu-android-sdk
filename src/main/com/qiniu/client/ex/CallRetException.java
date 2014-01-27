package com.qiniu.client.ex;

import com.qiniu.client.rs.CallRet;
import com.qiniu.client.util.Util;

/**
 * Created by xc on 1/16/14.
 */
public class  CallRetException extends Exception{
    private CallRet ret;

    public CallRetException(CallRet ret) {
        this.ret = ret;
    }

    public CallRetException(String detailMessage, CallRet ret) {
        super(detailMessage);
        this.ret = ret;
    }

    public CallRetException(String detailMessage, Throwable throwable, CallRet ret) {
        super(detailMessage, throwable);
        this.ret = ret;
    }

    public CallRetException(Throwable throwable, CallRet ret) {
        super(throwable);
        this.ret = ret;
    }

    public String toString(){
        return getRet().toString();
    }

    public CallRet getRet() {
        return ret;
    }
    
    public <T extends CallRet> T getRet(Class<T> c) {
        return Util.wrap(c, ret);
    }

}
