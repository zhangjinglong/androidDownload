package com.ivali.zhang.download;

import com.ivali.zhang.download1.DownloadServices;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
	
	Button  down_btn;
	Button  down_btn1;
	String url="http://dblt.xiazaiba.com/Soft/Ivali/QHBao/qhbao_1.8.1_XiaZaiBa.apk";
	DownloadServices services;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		services=new DownloadServices();
		down_btn=(Button) findViewById(R.id.down_btn);
		down_btn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				DownloadHelper.test(MainActivity.this);
			}
		});
		down_btn1=(Button) findViewById(R.id.down_btn1);
		down_btn1.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				//启动服务
				 Intent intent = new Intent(MainActivity.this,DownloadServices.class);  
				 intent.putExtra("url",url);
				 startService(intent);
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
