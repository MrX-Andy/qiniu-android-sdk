package com.qiniu.client.up.normal;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.StringBody;

import com.qiniu.client.auth.Authorizer;
import com.qiniu.client.config.Config;
import com.qiniu.client.ex.CallRetException;
import com.qiniu.client.rs.UploadResultCallRet;
import com.qiniu.client.up.Upload;
import com.qiniu.client.up.slice.resume.Resumable;
import com.qiniu.client.util.Util;

/**
 * 资源作为一个整体，直接上传到服务器
 * @author xc
 *
 */
public abstract class NormalUpload extends Upload {

	public NormalUpload(ThreadPoolExecutor threadPool, Authorizer authorizer,
			long contentLength, String key, String mimeType) {
		super(threadPool, authorizer, contentLength, key, mimeType);
	}

	@Override
	public UploadResultCallRet execute() {

		try {
			MultipartEntity requestEntity = new MultipartEntity();
			requestEntity.addPart("token",
					new StringBody(authorizer.getUploadToken()));
			requestEntity.addPart("file", buildFileBody());
			buildKeyPart(requestEntity);

			HttpPost post = new HttpPost(host);
			post.setHeader("User-Agent", Config.USER_AGENT);
			post.setEntity(requestEntity);
			HttpResponse response = httpClient.execute(post);
			UploadResultCallRet ret = Util.wrap(UploadResultCallRet.class,
					Util.handleResult(response));
			Util.checkCallRet(ret);
			return ret;
		} catch (CallRetException e) {
			e.printStackTrace();
			return e.getRet(UploadResultCallRet.class);
		} catch (Exception e) {
			e.printStackTrace();
			return new UploadResultCallRet(400, e);
		} finally {
            done = true;
        }
	}
	
	protected abstract AbstractContentBody buildFileBody();
	
	protected void buildKeyPart(MultipartEntity requestEntity) throws UnsupportedEncodingException{
		if(key != null){
			requestEntity.addPart("key", new StringBody(key,Charset.forName("utf-8")));
		}
	}
	
	public Resumable getResume(){
		return null;
	}

}
