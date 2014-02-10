package com.qiniu.client.up;


import android.os.Handler;
import android.os.Message;

import com.qiniu.client.rs.UploadResultCallRet;

public abstract class UploadHandler extends Handler {
	private Object passParam;
	private long currentUploadLength;
	private long contentLength;
	private long lastUploadLength;
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
	public long getCurrentUploadLength() {
		return currentUploadLength;
	}

	public void setCurrentUploadLength(long currentUploadLength) {
		this.currentUploadLength = currentUploadLength;
	}

	public long getContentLength() {
		return contentLength;
	}

	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}

	public long getLastUploadLength() {
		return lastUploadLength;
	}

	public void setLastUploadLength(long uploadLength) {
		this.lastUploadLength = uploadLength;
	}
}

