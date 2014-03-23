package com.felina.photographer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
	private static Boolean SHOW_UUID = false;
	private TextView uuidText;
	private Button nextButton;
	private File imageFile;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		uuidText = (TextView) findViewById(R.id.uuidTxt);
		nextButton = (Button) findViewById(R.id.nextBtn);
		nextButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				getToken();
			}
		});
		
		if(fClient == null) {
			fClient = new FelinaClient(this);
		}
		
		if (savedInstanceState != null) {
			EMAIL = savedInstanceState.getString(Extra.EMAIL_PREF);
			TOKEN = savedInstanceState.getString(Extra.TOKEN_PREF);
			startCamera();
		} else {
			EMAIL = new UUIDFactory(this).getDeviceUUID() + Constants.DOMAIN;
			setToken();
		}
	}
	
	/**
	 * Tries to get the saved token from SharedPreferences. 
	 * Failing that calls getToken()
	 */
	private void setToken() {
		Log.d(LOG_TAG, "setToken");
		if(TOKEN == null) {
			synchronized (MainActivity.class) {
				SharedPreferences prefs = getSharedPreferences(Extra.TOKEN_PREF_FILE, MODE_PRIVATE);
				TOKEN = prefs.getString(Extra.TOKEN_PREF, null);			
				if(TOKEN == null) {
					getToken();
				} else {
					startCamera();
				}
			}
		} else {
			startCamera();
		}
	}
	
	/**
	 * Requests a token from the server
	 * Failing that it displays the UUID
	 */
	private void getToken() {
		Log.d(LOG_TAG, "getToken");
		fClient.token(EMAIL, new JsonHttpResponseHandler(){
			@Override
			public void onSuccess(JSONObject response) {
				try {
					if(response.getBoolean("res")) {
						TOKEN = response.getString("token");
						saveToken(TOKEN);
						startCamera();
					} else {
						showUUID();
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void onFailure(Throwable e, JSONObject errorResponse) {
				e.printStackTrace();
			}
		});
	}
	
	/**
	 * Saves the token to SharedPreferences
	 * @param token The token to be saved
	 */
	private void saveToken(String token) {
		Log.d(LOG_TAG, "saveToken");
		synchronized (MainActivity.class) {
			SharedPreferences prefs = getSharedPreferences(Extra.TOKEN_PREF_FILE, MODE_PRIVATE);
			prefs.edit().putString(Extra.TOKEN_PREF, token).commit();
		}
	}
	
	/**
	 * Launches the camera to take a picture
	 */
	private void startCamera() {
		Log.d(LOG_TAG, "Starting camera");
		SHOW_UUID = false;
		uuidText.setVisibility(View.INVISIBLE);
		nextButton.setVisibility(View.INVISIBLE);
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
		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		File image = new File(dir, timeStamp+".jpg");
		return image;
	}
	
	/**
	 * Displays the UUID and next button.
	 */
	private void showUUID() {
		Log.d(LOG_TAG, "Showing UUID");
		SHOW_UUID = true;
		uuidText.setText(EMAIL);
		uuidText.setVisibility(View.VISIBLE);
		nextButton.setVisibility(View.VISIBLE);
	}
	
	/**
	 * uploads the image to the server
	 */
	private void uploadImage() {
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Constants.REQUEST_IMAGE_CAPTURE) {
			switch (resultCode) {
			case RESULT_OK:
				break;
			case RESULT_CANCELED: finish();
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(Extra.EMAIL_PREF, EMAIL);
		outState.putString(Extra.TOKEN_PREF, TOKEN);
		
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
