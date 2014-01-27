package com.qiniu.client.up.slice;

import com.qiniu.client.ex.CallRetException;
import com.qiniu.client.ex.RetryException;
import com.qiniu.client.rs.ChunkUploadCallRet;
import com.qiniu.client.util.Util;
import com.qiniu.client.util.HttpHelper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;

import java.util.concurrent.Callable;


public abstract class UploadBlock  implements Callable<ChunkUploadCallRet> {
    public static int CHUNK_SIZE = 1024 * 256;
    public static int FIRST_CHUNK = 1024 * 64;
    public static int triedTimes = 3;

    protected HttpClient httpClient;
    protected String orginHost;

    /// 此块开始的位置
    protected long offset;
    /// 此块的长度
    protected int length;
    protected SliceUpload sliceUpload;
    
    public UploadBlock(SliceUpload sliceUpload, HttpClient httpClient,
            String host, long offset, int len){
    	this.sliceUpload = sliceUpload;
		this.httpClient = httpClient;
		this.orginHost = host;
        this.offset = offset;
        this.length = len;
    }

    @Override
    public ChunkUploadCallRet call() throws Exception {
        int flen = Math.min(length, FIRST_CHUNK);
        ChunkUploadCallRet ret = uploadMkblk(flen, 0);
        checkChunkCallRet(ret);
        if (length > FIRST_CHUNK) {
            int count = (length - FIRST_CHUNK + CHUNK_SIZE - 1) / CHUNK_SIZE;
            for(int i = 0; i < count; i++) {
                int start = CHUNK_SIZE * i + FIRST_CHUNK;
                int len = Math.min(length - start, CHUNK_SIZE);
                ret = uploadChunk(ret, start, len, 0);
                checkChunkCallRet(ret);
            }
        }
        clean();
        return ret;
    }

    private  void checkChunkCallRet(ChunkUploadCallRet ret) throws Exception{
        if(ret == null || !ret.ok()){
            clean();
            throw new CallRetException(ret);
        }
    }

    private ChunkUploadCallRet uploadMkblk(int len, int time) {
        String url = getMkblkUrl();
        return upload(url, 0, len, time);
    }

    private ChunkUploadCallRet uploadChunk(ChunkUploadCallRet ret, int start, int len, int time) {
            String url = getBlkUrl(ret);
            return upload(url, start, len, time);
    }

    private ChunkUploadCallRet upload(String url, int start,int len, int time)  {
        try{
            HttpPost post = HttpHelper.buildUpPost(url, sliceUpload.getAuthorizer());
            long crc32 = buildCrc32(start, len);
            post.setEntity(buildHttpEntity(start, len));
            HttpResponse response = httpClient.execute(post);
            ChunkUploadCallRet nret = Util.wrap(ChunkUploadCallRet.class, Util.handleResult(response));
            Util.checkChunkCallRet(nret, crc32);
            sliceUpload.addSuccessLength(len);
            return nret;
        } catch (RetryException e) {
            if (time < triedTimes) {
                return upload(url, start, len, time + 1);
            }else{
                return e.getRet(ChunkUploadCallRet.class);
            }
        } catch(CallRetException e){
            return e.getRet(ChunkUploadCallRet.class);
        } catch (Exception e) {
            return new ChunkUploadCallRet(400, e);
        }
    }

    private String getMkblkUrl() {
        String url = orginHost + "/mkblk/" + length;
        return url;
    }

    private String getBlkUrl(ChunkUploadCallRet ret) {
        String url = ret.getHost() + "/bput/" + ret.getCtx() + "/" + ret.getOffset();
        return url;
    }

    protected abstract HttpEntity buildHttpEntity(int start, int len);
    
    protected abstract long buildCrc32(int start, int len);

    protected abstract void clean();

}
