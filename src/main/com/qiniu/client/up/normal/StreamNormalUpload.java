package com.qiniu.client.up.normal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;

import com.qiniu.client.auth.Authorizer;

public class StreamNormalUpload extends NormalUpload {
	protected InputStream is;
	
	public StreamNormalUpload(InputStream is, Authorizer authorizer,
			long totalLength, String key, String mimeType) {
		this(null, is, authorizer, totalLength, key, mimeType);
	}
	
	public StreamNormalUpload(ThreadPoolExecutor threadPool, InputStream is,
			Authorizer authorizer, long totalLength, String key, String mimeType) {
		super(threadPool, authorizer, totalLength, key, mimeType);
			this.is = is;
	}
	
	protected AbstractContentBody buildFileBody(){
		if(mimeType != null){
			return new MyInputStreamBody(is, mimeType, getFileName());
		}else{
			return new MyInputStreamBody(is, getFileName());
		}
	}
	
	private String getFileName(){
		return key != null ? key : UUID.randomUUID().toString();
	}
	
	protected class MyInputStreamBody extends InputStreamBody{

		public MyInputStreamBody(InputStream in, String mimeType, String filename) {
			super(in, mimeType, filename);
		}
		
		public MyInputStreamBody(InputStream in, String filename) {
			super(in, filename);
		}
		
		public void writeTo(final OutputStream out) throws IOException {
	        if (out == null) {
	            throw new IllegalArgumentException("Output stream may not be null");
	        }
	        try {
	            byte[] tmp = new byte[4096];
	            int l;
	            while ((l = this.getInputStream().read(tmp)) != -1) {
	                out.write(tmp, 0, l);
	                addSuccessLength(l); // add
	            }
	            out.flush();
	        } finally {
	            this.getInputStream().close();
	        }
	    }
		
	}

}
