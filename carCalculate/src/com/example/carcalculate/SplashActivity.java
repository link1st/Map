package com.example.carcalculate;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
public class SplashActivity extends Activity {

	// private TextView versionText;
	// 启动界面显示的时间 2s
	private static final int sleepTime = 2000;

	@Override
	protected void onCreate(Bundle arg0) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);// 不显示程序的标题栏
		final View view = View.inflate(this, R.layout.activity_splash, null);
		setContentView(view);
		
		super.onCreate(arg0);
	//	initFile();
	}

	@Override
	protected void onStart() {
		super.onStart();

		new Thread(new Runnable() {
			public void run() {

				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
				}
				startActivity(new Intent(SplashActivity.this, CarRoute.class));
				finish();

			}
		}).start();

	}

	@SuppressLint("SdCardPath")
	public void initFile() {

		File dir = new File("/sdcard/test");

		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
}
