package com.qiniu.cl.auth;

public interface Authorizer {
	void buildNewUploadToken();
	String getUploadToken();
}
