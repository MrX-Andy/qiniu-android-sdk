package com.qiniu.client.up.slice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.qiniu.client.auth.Authorizer;

public class RandomAccessFileUpload extends SliceUpload {
	protected RandomAccessFile file;
	private final Lock fileUploadLock;
	
	public RandomAccessFileUpload(File file, Authorizer authorizer,
			long totalLength, String key, String mimeType) {
		this(null, file, authorizer, totalLength, key, mimeType);
	}
	
	public RandomAccessFileUpload(ThreadPoolExecutor threadPool, File file,
			Authorizer authorizer, long totalLength, String key, String mimeType) {
		super(threadPool, authorizer, totalLength, key, mimeType);
		try {
			this.file = new RandomAccessFile(file, "r");
			fileUploadLock = new ReentrantLock();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected UploadBlock buildBlockUpload(long start, int len)
			throws IOException {
		return new RandomAccessFileUploadBlock(this, httpClient, host, start, len, file, fileUploadLock);
	}

	@Override
	protected void clean() {
		if(file != null){
			try {
				file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
