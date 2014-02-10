package com.qiniu.client.rs;

public class CallRet {
	protected int statusCode;
	protected String response;
	protected Exception exception;
	
	/**
	 * 子类必须实现此构造函数
	 * @param ret
	 */
	public CallRet(CallRet ret){
		if(ret != null){
			this.statusCode = ret.statusCode;
			this.response = ret.response;
			this.exception = ret.exception;
			doUnmarshal();
		}
	}

	public CallRet(int statusCode, String response) {
		this.statusCode = statusCode;
		this.response = response;
		doUnmarshal();
	}

	public CallRet(int statusCode, Exception e) {
		this.statusCode = statusCode;
		this.exception = e;
	}

	public boolean ok() {
		return this.statusCode / 100 == 2 && this.exception == null;
	}
	
	public void doUnmarshal() {
		try {
			if(this.response != null && this.response.trim().startsWith("{")){
				unmarshal();
			}
		} catch (Exception e) {
			e.printStackTrace();
			if(this.exception == null){
				this.exception = e;
			}
		}
		
	}

	protected void unmarshal() throws Exception {
		
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getResponse() {
		return response;
	}

	public Exception getException() {
		return exception;
	}

	public String toString() {
		return "{\"statusCode\":\"" + statusCode + "\",\"response\":"
				+ (response == null ? "null" : "\"" + response + "\"" ) + ",\"exception\":" 
				+ (exception == null ? "null" : "\"" + exception + "\"") + "}";
	}

}
