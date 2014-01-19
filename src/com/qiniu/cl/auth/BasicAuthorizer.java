package com.qiniu.cl.auth;

public class BasicAuthorizer implements Authorizer {
	private String uploadToken;
	
	@Override
	public void buildNewUploadToken() {
		uploadToken = "acmKu7Hie1OQ3t31bAovR6JORFX72MMpTicc2xje:XoUn4tl7DnSp0luP14m_C7DlVNk=:eyJzY29wZSI6ImxpdWJpbiIsICJkZWFkbGluZSI6MTM4OTg2NTA3MX0=";
		uploadToken = "acmKu7Hie1OQ3t31bAovR6JORFX72MMpTicc2xje:xQusEBzt2sspSSCjpx4A0QVvET4=:eyJzY29wZSI6ImxpdWJpbiIsICJkZWFkbGluZSI6MTM5OTg2NTA3MX0=";
	}

	@Override
	public String getUploadToken() {
		if(uploadToken == null){
			buildNewUploadToken();
		}
		return uploadToken;
	}

}
