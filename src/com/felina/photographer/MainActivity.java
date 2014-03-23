package com.felina.photographer;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

import android.widget.TextView;

public class MainActivity extends Activity {
	
	private static String LOG_TAG = Constants.LOG_TAG +"Main";
	private static String EMAIL;
	private static String TOKEN;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		String uuid = new UUIDFactory(this).getDeviceUUID();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
