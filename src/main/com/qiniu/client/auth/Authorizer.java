package com.qiniu.client.auth;

public interface Authorizer {
	void buildNewUploadToken();
	String getUploadToken();
}
