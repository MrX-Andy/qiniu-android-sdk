package com.qiniu.demo;

import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.qiniu.R;
import com.qiniu.cl.auth.Authorizer;
import com.qiniu.cl.auth.BasicAuthorizer;
import com.qiniu.cl.face.UpApi;
import com.qiniu.cl.face.UriParam;
import com.qiniu.cl.up.SliceUpload;
import com.qiniu.cl.up.StreamSliceUpload;

public class MainActivity extends Activity {
	public static final int FILE_SELECT_CODE = 0;

	private Button btnUpload;
	private TextView progress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initWidget();
	}

	private void initWidget() {
		progress = (TextView) findViewById(R.id.textView1);
		btnUpload = (Button) findViewById(R.id.button1);
		btnUpload.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (view.equals(btnUpload)) {
					Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
					intent.setType("*/*");
					intent.addCategory(Intent.CATEGORY_OPENABLE);
					try {
						startActivityForResult(
								Intent.createChooser(intent, "请选择一个要上传的文件"),
								FILE_SELECT_CODE);
					} catch (android.content.ActivityNotFoundException ex) {
						Toast.makeText(getApplicationContext(), "请安装文件管理器",
								Toast.LENGTH_LONG).show();
					}
				}
			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK)
			return;
		if (requestCode == FILE_SELECT_CODE) {
			doUpload(data.getData());
			return;
		}
	}

	long start = 0;
	
	private Handler uploadHandler = new UpApi.UploadHandler(){
		@Override
		protected void handler(){
			long now = System.currentTimeMillis();
			long time = (now - start)/1000;
			long v = this.successUpload/1000/(time + 1);
			
			UriParam p = (UriParam)this.passParam;
			String m = "";
			if(p != null){
				m = "m  " + p.path + "/" + p.name + " : "  + p.size + "  : "+ p.lastMofified;
			}
			String txt = m + "\n共: " + this.total + "B, 已上传: " + this.successUpload + 
					"B, 耗时: " + time + "秒, 速度: " + v + "KB/s";
			if(this.success){
				progress.setText("上传成功! ==> " + txt + "\n hash: " + ret.getHash() + ", key: "+ ret.getKey());
			}else if(this.done){
				progress.setText("上传失败! ==> " + this.exception + "  -- " + txt);
			}else{
				progress.setText(txt);
			}
		}
	};
	
	private void doUpload(Uri uri) {
		Authorizer authorizer = new BasicAuthorizer();
		UriParam p = UriParam.uri2UriParam(uri, this);
		
		String key = UUID.randomUUID().toString();
		p.name = key + "__" + p.name;
		
		SliceUpload.streamBlockLimit = 3;
		// 确保 p.is, p.size 正确, 不能为 null
		StreamSliceUpload upload = new StreamSliceUpload(authorizer, p.is, p.size,
				p.name, p.mimeType);
		upload.passParam = p;
		
		start = System.currentTimeMillis();
		new UpApi(upload, uploadHandler).execute();
	}
	
}
