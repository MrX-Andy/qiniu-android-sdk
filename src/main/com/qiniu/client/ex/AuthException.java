package com.qiniu.client.ex;

public class AuthException extends RuntimeException{

	public AuthException() {
		super();
	}

	public AuthException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public AuthException(String detailMessage) {
		super(detailMessage);
	}

	public AuthException(Throwable throwable) {
		super(throwable);
	}


}
