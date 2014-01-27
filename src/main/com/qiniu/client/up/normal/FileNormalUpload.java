package com.qiniu.client.up.normal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.FileBody;

import com.qiniu.client.auth.Authorizer;

public class FileNormalUpload extends NormalUpload {
	protected File file;
	
	public FileNormalUpload(File file, Authorizer authorizer,
			long totalLength, String key, String mimeType) {
		this(null, file, authorizer, totalLength, key, mimeType);
	}
	
	public FileNormalUpload(ThreadPoolExecutor threadPool, File file,
			Authorizer authorizer, long totalLength, String key, String mimeType) {
		super(threadPool, authorizer, totalLength, key, mimeType);
			this.file = file;
	}
	
	protected AbstractContentBody buildFileBody(){
		if(mimeType != null){
			return new MyFileBody(file, mimeType);
		}else{
			return new MyFileBody(file);
		}
	}
	
	protected class MyFileBody extends FileBody{

		public MyFileBody(File file) {
			super(file);
		}
		
		public MyFileBody(File file, String mimeType) {
			super(file, mimeType);
		}

		 public void writeTo(final OutputStream out) throws IOException {
		        if (out == null) {
		            throw new IllegalArgumentException("Output stream may not be null");
		        }
		        InputStream in = new FileInputStream(this.getFile());
		        try {
		            byte[] tmp = new byte[4096];
		            int l;
		            while ((l = in.read(tmp)) != -1) {
		                out.write(tmp, 0, l);
		                addSuccessLength(l); // add
		            }
		            out.flush();
		        } finally {
		            in.close();
		        }
		    }
		
	}

}
