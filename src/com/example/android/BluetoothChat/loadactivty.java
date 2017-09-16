package com.example.android.BluetoothChat;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class loadactivty extends Activity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.load);
		ImageView logo=(ImageView) findViewById(R.id.imageView1);
		Animation anim=AnimationUtils.loadAnimation(loadactivty.this, R.anim.animation);
		logo.setAnimation(anim);
		super.onCreate(savedInstanceState);
		Thread th=new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				runOnUiThread(new Runnable() {
					public void run() {
						finish();
					}
				});
				
			}
		});
		th.start();
	}

}
