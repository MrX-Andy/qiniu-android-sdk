qiniu-android-sdk
=================
1.多线程上传
2.多文件上传队列
3.断点续传

private Handler uploadHandler = new Upload.UploadHandler(){
		@Override
		protected void handler(){
			//do something
		}
}

SliceUpload upload = new StreamSliceUpload(authorizer, p.is, p.size,
				p.name, p.mimeType);

new Upload(upload, uploadHandler).execute();


