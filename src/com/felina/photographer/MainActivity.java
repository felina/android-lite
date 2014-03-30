package com.felina.photographer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.felina.android.api.FelinaClient;
import com.felina.photographer.Constants.Extra;
import com.loopj.android.http.JsonHttpResponseHandler;

public class MainActivity extends Activity {
	
	private static String LOG_TAG = Constants.LOG_TAG +".Main";
	private static String EMAIL;
	private static String TOKEN;
	private static FelinaClient fClient;
	private TextView uuidText;
	private Button nextButton;
	private File imageFile;
	private View loadingBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		uuidText = (TextView) findViewById(R.id.uuidTxt);
		nextButton = (Button) findViewById(R.id.nextBtn);
		loadingBar = findViewById(R.id.loadingPanel);
		nextButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				getToken(Constants.RETRY_LIMIT);
			}
		});
		
		File storagePath = new File(Environment.getExternalStorageDirectory() + File.separator + Constants.STORAGE_FOLDER);
		if (!storagePath.exists()) {
			Log.d(LOG_TAG, "storagePath does not exist");
			storagePath.mkdirs();
		}
		
		if(fClient == null) {
			fClient = new FelinaClient(this);
		}
		
		if (savedInstanceState != null) {
			EMAIL = savedInstanceState.getString(Extra.EMAIL_PREF);
		} else {
			EMAIL = new UUIDFactory(this).getDeviceUUID() + Constants.DOMAIN;
		}
		
		setToken();
	}
	
	/**
	 * Tries to get the saved token from SharedPreferences. 
	 * Failing that calls getToken()
	 */
	private void setToken() {
		Log.d(LOG_TAG, "setToken");
		if(TOKEN == null) {
			TOKEN = TokenUtils.readToken(this);			
			if(TOKEN.equals(Constants.NULL_TOKEN)) {
				TOKEN = null;
				getToken(Constants.RETRY_LIMIT);
			} else {
				startCamera();
			}
		} else {
			Log.d(LOG_TAG, TOKEN);
			startCamera();
		}
	}
	
	/**
	 * Requests a token from the server
	 * Failing that it displays the UUID
	 */
	private void getToken(final int retry) {
		Log.d(LOG_TAG, "getToken");
		if (retry == 0) {
			Log.d(LOG_TAG, "token retry limit reached");
			nextButton.setEnabled(true);
			return;
		}
		
		nextButton.setEnabled(false);
		
		fClient.token(EMAIL, new JsonHttpResponseHandler(){
			@Override
			public void onSuccess(JSONObject response) {
				try {
					if(response.getBoolean("res")) {
						TOKEN = response.getString("token");
						TokenUtils.writeToken(getApplicationContext(), TOKEN);
						startCamera();
					} else {
						showUUID();
					}
				} catch (JSONException e) {
					e.printStackTrace();
					getToken(retry-1);
				}
				nextButton.setEnabled(true);
			}
			
			@Override
			public void onFailure(Throwable e, JSONObject errorResponse) {
				e.printStackTrace();
				getToken(retry-1);
			}
		});
	}
	
	/**
	 * Launches the camera to take a picture
	 */
	private void startCamera() {
		Log.d(LOG_TAG, "Starting camera");
		uuidText.setVisibility(View.GONE);
		nextButton.setVisibility(View.GONE);
		loadingBar.setVisibility(View.VISIBLE);
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		
		if(intent.resolveActivity(getPackageManager()) != null) {
			imageFile = null;
			try {
				imageFile = createFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (imageFile != null) {
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
				startActivityForResult(intent, Constants.REQUEST_IMAGE_CAPTURE);
			}
		}
	}
	
	/**
	 * Helper function to create the file to store the image
	 * @return file created
	 * @throws IOException
	 */
	private File createFile() throws IOException {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File dir = new File(Environment.getExternalStorageDirectory() + File.separator + Constants.STORAGE_FOLDER);
		File image = new File(dir, timeStamp+".jpg");
		return image;
	}
	
	/**
	 * Displays the UUID and next button.
	 */
	private void showUUID() {
		Log.d(LOG_TAG, "Showing UUID");
		uuidText.setText(EMAIL);
		uuidText.setVisibility(View.VISIBLE);
		nextButton.setVisibility(View.VISIBLE);
		loadingBar.setVisibility(View.GONE);
		TokenUtils.writeToken(this, Constants.NULL_TOKEN);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == Constants.REQUEST_IMAGE_CAPTURE) {
			switch (resultCode) {
			case RESULT_OK:
				if (NetworkUtil.isConnected(this)) {
					UploadUtils.start(this, imageFile);
				}
				startCamera();
				break;
			case RESULT_CANCELED: finish();
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(Extra.EMAIL_PREF, EMAIL);	
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
