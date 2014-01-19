package com.qiniu.cl.up;

import com.qiniu.cl.auth.Authorizer;
import com.qiniu.cl.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StreamSliceUpload extends SliceUpload{
    protected InputStream is;
    private List<ByteRef> buffers;

    public StreamSliceUpload(Authorizer authorizer, InputStream is, long totalLength, String key,
                             String mimeType){
        this.is = is;
        this.length = totalLength;
        this.authorizer = authorizer;
        this.key = key;
        this.mimeType = mimeType;
        buffers = new ArrayList<ByteRef>();
    }

    @Override
    protected BlockUpload buildBlockUpload(long start, int len) throws IOException {
        int refCount = streamBlockLimit > 0 ? streamBlockLimit : threadsCount + 1 + threadsCount/3;
        ByteRef br = getByteArray(buffers, refCount);
        byte[] b = new byte[len];
        is.read(b, 0, len);
        br.setBuf(b);
        StreamBlockUpload bu = new StreamBlockUpload(this, httpClient, host, start, len, br);
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
    private ByteRef getByteArray(List<ByteRef> buffers, int count){
        while(true){
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
                Util.sleep(100);
            }
            Util.sleep(300);
        }
    }


   protected class ByteRef{
        private byte[] buf;

        public byte[] getBuf() {
            return buf;
        }
        private void setBuf(byte[] buf) {
            this.buf = buf;
        }
        public void clean(){this.buf = null;}
        public boolean isEmpty() {
            return buf == null;
        }
    }
}
