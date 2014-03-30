package com.felina.photographer;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.felina.android.api.FelinaClient;
import com.loopj.android.http.JsonHttpResponseHandler;

public class UploadUtils {
	
	private static String LOG_TAG = Constants.LOG_TAG + ".UploadUtils";
	private static FelinaClient fClient;
	private static String EMAIL;
	private static String TOKEN;
	private static Boolean STATE_BUSY = false;
	private static ArrayList<File> fileList;
	
	/**
	 * Starts the file upload with a saved file
	 * @param context
	 */
	public static void start(Context context) {
		start(context, getNextFile());
	}
	
	/**
	 * Starts a file upload if not currently busy in an upload cycle
	 * @param context
	 * @param f file to be uploaded
	 */
	public static void start(Context context, File f) {
		Log.d(LOG_TAG, "start");
		checkStatics(context);
		if (!STATE_BUSY && f != null && !TOKEN.equals(Constants.NULL_TOKEN)) {
			Log.d(LOG_TAG, "not busy");
			STATE_BUSY = true;
			startUpload(context, Constants.RETRY_LIMIT, f);
		}
	}
	
	/**
	 * Entry to the upload cycle. Logs in to server and starts upload.
	 * @param context
	 * @param retry
	 * @param f
	 */
	private static void startUpload(final Context context, final int retry, final File f) {
		Log.d(LOG_TAG, "startUpload");
		
		checkStatics(context);
		
		if (retry == 0 || !NetworkUtil.isConnected(context) || TOKEN.equals(Constants.NULL_TOKEN)){
			Log.d(LOG_TAG, "startUpload retry limit reached or not connected or null token");
			STATE_BUSY = false;
			return;
		}
				
		fClient.login(EMAIL, TOKEN, new JsonHttpResponseHandler(){
			@Override
			public void onSuccess(JSONObject response) {
				try {
					if (response.getBoolean("res")) {
						upload(context, Constants.RETRY_LIMIT, f);
					} else {
						TOKEN = null;
						TokenUtils.writeToken(context, Constants.NULL_TOKEN);
						STATE_BUSY = false;
					}
				} catch (JSONException e) {
					e.printStackTrace();
					startUpload(context, retry-1, f);
				}
			}
			
			@Override
			public void onFailure(Throwable e, JSONObject errorResponse) {
				startUpload(context, retry-1, f);
			}
		});
	}
	
	/**
	 * Starts the image upload to the server
	 * @param retry the number of times the client should attempt to upload the image on failures
	 * @param f the image to be uploaded
	 */
	private static void upload(final Context context, final int retry, final File f) {
		Log.d(LOG_TAG, "upload");
		if (retry == 0 || !NetworkUtil.isConnected(context)) {
			Log.d(LOG_TAG, "upload retry limit reached or not connected");
			STATE_BUSY = false;
			return;
		}
		
		fClient.postImg(f, "image/jpeg", new JsonHttpResponseHandler(){
			@Override
			public void onSuccess(JSONObject response) {
				Log.d(LOG_TAG, "upload success");
		        if (f.exists()) {
		            if (f.delete()) {
		        		Log.d(LOG_TAG, "file deleted");
		            } else {
		        		Log.d(LOG_TAG, "file not deleted");
		            }
		        }
		        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
		        Uri.parse("file://" +  Environment.getExternalStorageDirectory())));
		        
		        File next = getNextFile();
		        if (next != null) {
		        	Log.d(LOG_TAG, next.getAbsolutePath());
		        	startUpload(context, Constants.RETRY_LIMIT, next);
		        } else {
		        	STATE_BUSY = false;	
		        }
			}
			
			@Override
			public void onFailure(Throwable e, JSONObject errorResponse) {
				upload(context, retry-1, f);
			}
		});
	}
	
	/**
	 * Checks if the static variables have been assigned values
	 * @param context
	 */
	private static void checkStatics(Context context) {
		if (EMAIL == null) {
			EMAIL = new UUIDFactory(context).getDeviceUUID() + Constants.DOMAIN;
		}
		if (TOKEN == null || TOKEN.equals(Constants.NULL_TOKEN)) {
			TOKEN = TokenUtils.readToken(context);
		}
		if (fClient == null) {
			fClient = new FelinaClient(context);
		}
	}
	
	/**
	 * 
	 * @return the list of files in the felina directory
	 */
	private static File getNextFile() {
		if (fileList == null) {
			fileList = new ArrayList<File>();
		}
		
		if (fileList.isEmpty()) {
			String path = Environment.getExternalStorageDirectory() + File.separator + Constants.STORAGE_FOLDER;
			File dir = new File(path);
			if (dir != null) {
				File[] files = dir.listFiles();
				if (files != null && files.length > 0) {
					for (File f: files) {
						fileList.add(f);
					}
					return fileList.remove(fileList.size()-1);
				}
			}
		} else {
			return fileList.remove(fileList.size()-1);
		}
		
		return null;
	}
}
