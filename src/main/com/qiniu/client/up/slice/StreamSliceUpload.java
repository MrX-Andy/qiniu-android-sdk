package com.qiniu.client.up.slice;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import com.qiniu.client.auth.Authorizer;
import com.qiniu.client.util.Util;

public class StreamSliceUpload extends SliceUpload{
    protected InputStream is;
    private List<ByteRef> buffers;

	public StreamSliceUpload(InputStream is, Authorizer authorizer,
			long totalLength, String key, String mimeType) {
		this(null, is, authorizer, totalLength, key, mimeType);
	}

	public StreamSliceUpload(ThreadPoolExecutor threadPool, InputStream is,
			Authorizer authorizer, long totalLength, String key, String mimeType) {
		super(threadPool, authorizer, totalLength, key, mimeType);
		this.is = is;
		buffers = new ArrayList<ByteRef>();
	}

    @Override
    protected UploadBlock buildBlockUpload(int blockIdx, long start, int len) throws IOException {
        ByteRef br = getByteArray(buffers);
        byte[] b = new byte[len];
        is.read(b, 0, len);
        br.setBuf(b);
        StreamUploadBlock bu = new StreamUploadBlock(this, httpClient, host, blockIdx, start, len, br);
        return bu;
    }

    @Override
    protected void clean() {
        if(is != null){
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /// ByteRef 的消耗者在其被消耗之后，必须保证b.isEmpty()为true--> clean()
    /// 避免读太多的数据到内存中，在内存中块的数量过多时，会阻塞。
    private ByteRef getByteArray(List<ByteRef> buffers){
        while(true){
        	// 会死锁吗??
        	int count = getMemeryBlockCount();
            if(buffers.size() < count){
                ByteRef b = new ByteRef();
                buffers.add(b);
                return b;
            }
            for(int i=0; i < count; i++){
                ByteRef b = buffers.get(i);
                if(b == null){
                    b = new ByteRef();
                    buffers.add(b);
                }
                if(b.isEmpty()){
                    return b;
                }
            }
            Util.sleep(400);
        }
    }

	protected class ByteRef {
		private byte[] buf;

		public byte[] getBuf() {
			return buf;
		}

		protected void setBuf(byte[] buf) {
			this.buf = buf;
		}

		public void clean() {
			this.buf = null;
		}

		public boolean isEmpty() {
			return buf == null;
		}
	}
}
