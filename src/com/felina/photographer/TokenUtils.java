package com.felina.photographer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.felina.photographer.Constants.Extra;

public class TokenUtils {
	
	private static String LOG_TAG = Constants.LOG_TAG + ".TokenUtils";
	
	/**
	 * reads the previously saved token from the preference file
	 * @param context 
	 * @return the token read
	 */
	public static synchronized String readToken(Context context) {
		Log.d(LOG_TAG, "readToken");
		SharedPreferences prefs = context.getSharedPreferences(Extra.TOKEN_PREF_FILE, Context.MODE_PRIVATE);
		return prefs.getString(Extra.TOKEN_PREF, Constants.NULL_TOKEN);
	}
	
	/**
	 * writes the token to the preference file
	 * @param context
	 * @param token
	 */
	public static synchronized void writeToken(Context context, String token) {
		Log.d(LOG_TAG, "writeToken");
		SharedPreferences prefs = context.getSharedPreferences(Extra.TOKEN_PREF_FILE, Context.MODE_PRIVATE);
		prefs.edit().putString(Extra.TOKEN_PREF, token).commit();
	}
}
