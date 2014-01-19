package com.qiniu.cl.up;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.AbstractHttpEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamBlockUpload extends BlockUpload {
    private StreamSliceUpload.ByteRef buffer;

	public StreamBlockUpload(StreamSliceUpload sliceUpload, HttpClient httpClient,
                             String host, long offset, int len, StreamSliceUpload.ByteRef br) {
		this.sliceUpload = sliceUpload;
		this.httpClient = httpClient;
		this.orginHost = host;
        this.offset = offset;
		this.buffer = br;
		this.length = len;
	}

    @Override
    protected HttpEntity buildHttpEntity(int start, int len) {
        return buildHttpEntity(copy2New(start, len));
    }

    @Override
    protected void clean() {
        if(buffer != null){
            buffer.clean();
        }
        buffer = null;
    }

	private byte[] copy2New(int start, int len){
		byte[] b = new byte[len];
		System.arraycopy(this.buffer.getBuf(), start, b, 0, len);
		return b;
	}

	private HttpEntity buildHttpEntity(final byte[] b){
		
		AbstractHttpEntity entity = new AbstractHttpEntity() {
			private boolean consumed = false;
			private ByteArrayInputStream bis = new ByteArrayInputStream(b);
			private long length = b.length;
			@Override
			public boolean isRepeatable() {
				return true;
			}

			@Override
			public long getContentLength() {
				return length;
			}

			@Override
			public InputStream getContent() throws IOException,
					IllegalStateException {
				return bis;
			}

			@Override
			public void  writeTo(OutputStream os) throws IOException {
				consumed = false;
				int uploadLen = 0;
				try{
					byte[] b = new byte[1024 * 2];
					int len = -1;
					while((len = bis.read(b)) != -1){
						os.write(b, 0, len);
						uploadLen += len;
					}
					os.flush();
				}finally{
					if(bis != null){
						try{bis.close();}catch(Exception e){}
						bis = null;
					}
					sliceUpload.addUploadLength(uploadLen);
					consumed = true;
				}
			}

			@Override
			public boolean isStreaming() {
				return !consumed;
			}
		};
		entity.setContentType("application/octet-stream");
		return entity;
	}


}
