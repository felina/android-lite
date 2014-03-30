package com.felina.photographer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtil extends BroadcastReceiver {
	
	private static String LOG_TAG = Constants.LOG_TAG + ".NetworkConnectedListener";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (isConnected(context)) {
			UploadUtils.start(context);
		}
	}
	
	/**
	 * checks if the device is connected to the network 
	 * @param context
	 * @return true if connected and false if not
	 * (code sampled from the training pages on developer.android.com)
	 */
	public static boolean isConnected(Context context) {
		ConnectivityManager cm =
		        (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null &&
		                      activeNetwork.isConnected();
		
		return isConnected;
	}
}
