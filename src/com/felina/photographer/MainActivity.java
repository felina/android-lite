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
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import co.darwinapp.photographer.R;

import com.felina.android.api.FelinaClient;
import com.felina.photographer.Constants.Extra;
import com.loopj.android.http.JsonHttpResponseHandler;

public class MainActivity extends Activity {
	
	private static String EMAIL;
	private static String TOKEN;
	private static FelinaClient fClient;
	private TextView uuidText;
	private Button nextButton;
	private File imageFile;
	private View loadingBar;
	private View toastView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		uuidText = (TextView) findViewById(R.id.uuidTxt);
		nextButton = (Button) findViewById(R.id.nextBtn);
		loadingBar = findViewById(R.id.loadingPanel);
		toastView = getLayoutInflater().inflate(R.layout.tick, (ViewGroup)findViewById(R.id.toastLayout));

		nextButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				getToken(Constants.RETRY_LIMIT);
			}
		});
		
		File storagePath = new File(Environment.getExternalStorageDirectory() + File.separator + Constants.STORAGE_FOLDER);
		if (!storagePath.exists()) {
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
		TOKEN = TokenUtils.readToken(this);			
		if(TOKEN.equals(Constants.NULL_TOKEN)) {
			TOKEN = null;
			getToken(Constants.RETRY_LIMIT);
		} else {
			startCamera();
		}
	}
	
	/**
	 * Requests a token from the server
	 * Failing that it displays the UUID
	 */
	private void getToken(final int retry) {
		if (retry == 0) {
			showUUID();
			return;
		}
		
		showLoading();
		
		try {
			fClient.token(EMAIL, new JsonHttpResponseHandler() {
				@Override
				public void onSuccess(JSONObject response) {
					try {
						if(response.getBoolean("res")) {
							TOKEN = response.getString("token");
							TokenUtils.writeToken(getApplicationContext(), TOKEN);
							if (NetworkUtil.isConnected(getApplicationContext())) {
								UploadUtils.start(getApplicationContext());	
							}
							startCamera();
						} else {
							showUUID();
							TokenUtils.writeToken(getApplicationContext(), Constants.NULL_TOKEN);
						}
					} catch (JSONException e) {
						e.printStackTrace();
						getToken(retry-1);
					}
					showUUID();
				}
				
				@Override
				public void onFailure(Throwable e, JSONObject errorResponse) {
					e.printStackTrace();
					getToken(retry-1);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Launches the camera to take a picture
	 */
	private void startCamera() {
		showLoading();
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
	 * Shows the loading view
	 */
	private void showLoading() {
		uuidText.setVisibility(View.GONE);
		nextButton.setVisibility(View.GONE);
		loadingBar.setVisibility(View.VISIBLE);
	}
	
	/**
	 * Displays the UUID and next button.
	 */
	private void showUUID() {
		String uuid = EMAIL.substring(0, EMAIL.indexOf(Constants.DOMAIN));
		uuidText.setText(uuid);
		uuidText.setVisibility(View.VISIBLE);
		nextButton.setVisibility(View.VISIBLE);
		loadingBar.setVisibility(View.GONE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == Constants.REQUEST_IMAGE_CAPTURE) {
			switch (resultCode) {
			case RESULT_OK:
				if (NetworkUtil.isConnected(this)) {
					UploadUtils.start(this, imageFile);
				}
				Toast toast = new Toast(this);
				toast.setView(toastView);
				toast.show();
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
