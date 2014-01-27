package com.qiniu.client.up;


import android.os.Handler;
import android.os.Message;

import com.qiniu.client.rs.UploadResultCallRet;

public abstract class UploadHandler extends Handler {
	private Object passParam;
	private long successLength;
	private long contentLength;
	private long uploadLength;
	
	private UploadResultCallRet ret;
	private Exception e;
	
	@Override
	public void handleMessage(Message msg) {
		if(ret != null){
			onSuccess(ret);
		}else if(e != null){
			onFailure(e);
		}else{
			onProcess();
		}
	}
	
	public void sendUploading(){
		try{
			send();
		}catch(Exception e){
			
		}
	}
	
	public void sendFinished(UploadResultCallRet ret){
		try{
			this.ret = ret != null ? ret : new UploadResultCallRet(null);
			send();
		}catch(Exception e){
			
		}
	}
	
	public void sendFailed(Exception e){
		try{
			this.e = e != null ? e : new Exception();
			send();
		}catch(Exception ex){
			
		}
	}

	private void send() {
		Message msg = new Message();
		msg.obj = this;
		sendMessage(msg);
	}
	
	protected abstract void onProcess();
	
	protected abstract void onSuccess(UploadResultCallRet ret);
	
	protected abstract void onFailure(Exception e);

	public Object getPassParam() {
		return passParam;
	}

	public void setPassParam(Object passParam) {
		this.passParam = passParam;
	}

	public long getSuccessLength() {
		return successLength;
	}

	public void setSuccessLength(long successLength) {
		this.successLength = successLength;
	}

	public long getContentLength() {
		return contentLength;
	}

	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}

	public long getUploadLength() {
		return uploadLength;
	}

	public void setUploadLength(long uploadLength) {
		this.uploadLength = uploadLength;
	}
	
}

