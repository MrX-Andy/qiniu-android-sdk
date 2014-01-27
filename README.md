qiniu-android-sdk  
=================  
包依赖 : httpmime-4.2, 可使用 httpmime-4.2.6.jar  
org.apache.http.entity.mime.  
  
一、资源上传（包含文件、输入流）：     
1.直传   
StreamInputParam fp = InputParam.streamInputParam(file);   
Upload upload2 =    
new StreamNormalUpload(fp.is, authorizer, fp.size, fp.name, fp.mimeType);   
upload2.passParam = fp; // 此参数会传递到回调中   
Thread t2 = UpApi.execute(upload2, uploadHandler);    
   
2.分片上传   
FileInputParam fp = InputParam.fileInputParam(file);   
Upload upload2 =    
new RandomAccessFileUpload(fp.file, authorizer, fp.size, fp.name, fp.mimeType);   
Thread t2 = UpApi.execute(upload2, uploadHandler);    
   
3.多文件队列上传   
UpApiSequence seq = new UpApiSequence();   
seq.add(uploadHandler, files);   
Thread t = seq.execute();   
seq.add(uploadHandler, inputParams);   
seq.tryShutDown();   
   
4.回调处理   
private Object passParam;   
private long successLength;   
private long contentLength;   
protected void onProcess();   
protected void onSuccess(UploadResultCallRet ret);   
protected void onFailure(Exception e);   
   
5.续传，保留已完整上传块的记录，已上传的块，不用再上传（待完成）   
//TODO   
   

