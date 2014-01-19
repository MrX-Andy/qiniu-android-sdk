package com.qiniu.cl.up;

import com.qiniu.cl.ex.CallRetException;
import com.qiniu.cl.ex.RetryException;
import com.qiniu.cl.rs.ChunkCallRet;
import com.qiniu.cl.util.Util;
import com.qiniu.cl.util.HttpHelper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;

import java.util.concurrent.Callable;


public abstract class BlockUpload  implements Callable<ChunkCallRet> {
    public static int CHUNK_SIZE = 1024 * 64;
    public static int FIRST_CHUNK = 1024 * 16;
    public static int triedTimes = 2;

    protected HttpClient httpClient;
    protected String orginHost;

    /// 此块开始的位置
    protected long offset;
    /// 此块的长度
    protected int length;
    protected SliceUpload sliceUpload;

    @Override
    public ChunkCallRet call() throws Exception {
        int flen = Math.min(length, FIRST_CHUNK);
        ChunkCallRet ret = uploadMkblk(flen, 0);
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

    private  void checkChunkCallRet(ChunkCallRet ret) throws Exception{
        if(ret == null || !ret.ok()){
            clean();
            throw new CallRetException(ret);
        }
    }

    private ChunkCallRet uploadMkblk(int len, int time) {
        String url = getMkblkUrl();
        return upload(url, 0, len, time);
    }

    private ChunkCallRet uploadChunk(ChunkCallRet ret, int start, int len, int time) {
            String url = getBlkUrl(ret);
            return upload(url, start, len, time);
    }

    private ChunkCallRet upload(String url, int start,int len, int time)  {
        try{
            HttpPost post = HttpHelper.buildUpPost(url, sliceUpload.authorizer);
            post.setEntity(buildHttpEntity(start, len));
            HttpResponse response = httpClient.execute(post);
            ChunkCallRet nret = Util.handleResult(response);
            Util.checkChunkCallRet(nret);
            sliceUpload.addSuccessLength(len);
            return nret;
        } catch (RetryException e) {
            if (time < triedTimes) {
                return upload(url, start, len, time + 1);
            }else{
                return e.getRet();
            }
        } catch(CallRetException e){
            return e.getRet();
        } catch (Exception e) {
            return new ChunkCallRet(400, e);
        }
    }

    private String getMkblkUrl() {
        String url = orginHost + "/mkblk/" + length;
        return url;
    }

    private String getBlkUrl(ChunkCallRet ret) {
        String url = ret.getHost() + "/bput/" + ret.getCtx() + "/" + ret.getOffset();
        return url;
    }

    protected abstract HttpEntity buildHttpEntity(int start, int len);

    protected abstract void clean();

}
