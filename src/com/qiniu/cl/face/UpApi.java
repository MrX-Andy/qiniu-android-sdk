package com.qiniu.cl.face;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

import com.qiniu.cl.rs.ChunkCallRet;
import com.qiniu.cl.up.SliceUpload;
import com.qiniu.cl.util.Util;

public class UpApi implements Runnable {
	private SliceUpload upload;
	private Handler handler;

	public UpApi(SliceUpload upload, Handler handler) {
		this.upload = upload;
		this.handler = handler;
	}
	
	public static void execute(SliceUpload upload, Handler handler){
		new UpApi(upload, handler).execute();
	}

	public void execute() {
		new Thread(this).start();
	}

	@Override
	public void run() {
		try {
			UploadTask uploadTask = new UploadTask();
			FutureTask<ChunkCallRet> task = new FutureTask<ChunkCallRet>(
					uploadTask);
			new Thread(task).start();

			while (upload != null && !upload.isDone()) {
				Util.sleep(300);
				onUpload(false, false, upload.passParam, upload.getSuccessLength(),
						upload.getContentLength(), null, null, null);
			}

			while (!task.isDone()) {
				Util.sleep(100);
			}
			ChunkCallRet ret = task.get();
			onUpload(true, true, upload.passParam, upload.getContentLength(),
					upload.getContentLength(), ret, null, null);
		} catch (Exception e) {
			onUpload(true, false, upload.passParam, 0, 0, null, null, e);
		}
	}

	private void onUpload(boolean done, boolean success, Object passParam, long successUpload,
			long total, ChunkCallRet ret, String msg, Exception e) {
		UploadParam p = new UploadParam();
		p.done = done;
		p.success = success;
		p.passParam = passParam;
		p.successUpload = successUpload;
		p.total = total;
		p.ret = ret;
		p.msg = msg;
		p.exception = e;

		Message message = new Message();
		message.obj = p;
		handler.sendMessage(message);
	}

	@SuppressLint("HandlerLeak")
	public static class UploadHandler extends Handler {
		public boolean success;
		public boolean done;
		public Object passParam;
		public Exception exception;
		public long successUpload;
		public long total;
		public ChunkCallRet ret;
		public String msg;

		public void handleMessage(Message msg) {
			UploadParam p = (UploadParam) msg.obj;
			this.done = p.done;
			this.success = p.success;
			this.passParam = p.passParam;
			this.successUpload = p.successUpload;
			this.total = p.total;
			this.ret = p.ret;
			this.msg = p.msg;
			this.exception = p.exception;
			handler();
		}

		protected void handler() {

		}
	}

	private class UploadTask implements Callable<ChunkCallRet> {

		@Override
		public ChunkCallRet call() throws Exception {
			return upload.execute();
		}

	}

	private class UploadParam {
		boolean success;
		boolean done;
		Object passParam;
		long successUpload;
		long total;
		ChunkCallRet ret;
		String msg;
		Exception exception;
	}

}
