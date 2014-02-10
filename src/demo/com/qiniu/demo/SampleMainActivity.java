package com.qiniu.demo;

import java.util.UUID;

import com.qiniu.client.auth.Authorizer;
import com.qiniu.client.auth.BasicAuthorizer;
import com.qiniu.client.rs.UploadResultCallRet;
import com.qiniu.client.up.InputParam;
import com.qiniu.client.up.InputParam.StreamInputParam;
import com.qiniu.client.up.UpApi;
import com.qiniu.client.up.Upload;
import com.qiniu.client.up.UploadHandler;
import com.qiniu.client.up.normal.StreamNormalUpload;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class SampleMainActivity extends Activity {
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
	
	private UploadHandler uploadHandler = new UploadHandler(){

		@Override
		protected void onProcess() {
			InputParam p = (InputParam)this.getPassParam();
			long now = System.currentTimeMillis();
			long time = (now - start)/1000;
			long v = this.getCurrentUploadLength()/1000/(time + 1);
			String m = p.path + " : " + p.name + " : "  + p.size + "  : "+ p.lastMofified;
			String txt = m + "\n共: " + getContentLength() + "KB, 历史已上传: "
					+ getLastUploadLength()/1024 + "KB, 本次已上传: "
					+ getCurrentUploadLength()/1024 + 
				"KB, 耗时: " + time + "秒, 速度: " + v + "KB/s";
			progress.setText(txt);
		}

		@Override
		protected void onSuccess(UploadResultCallRet ret) {
			String o = progress.getText().toString();
			progress.setText(o + "\n" + ret);
		}

		@Override
		protected void onFailure(Exception e) {
			String o = progress.getText().toString();
			progress.setText(o + "\n" + e);
		}

	};
	
	private void doUpload(Uri uri) {
		Authorizer authorizer = new BasicAuthorizer();
		StreamInputParam p = InputParam.streamInputParam(uri, this);
		
		String key = UUID.randomUUID().toString();
		p.name = key + "__" + p.name;
		
		Upload upload = new StreamNormalUpload(p.is, authorizer, p.size,
				p.name, p.mimeType);
		upload.passParam = p;
		progress.setText(p.name);
		start = System.currentTimeMillis();
		UpApi.execute(upload, uploadHandler);
	}
	
}
