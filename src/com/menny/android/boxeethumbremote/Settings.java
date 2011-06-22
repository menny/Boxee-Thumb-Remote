/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.menny.android.boxeethumbremote;

import com.menny.android.boxeethumbremote.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;

public class Settings {
	private static final String TAG = Settings.class.toString();
	
	/**
	 * Current page (i.e. gesture or dpad)
	 */
	public static final String SERVER_NAME_KEY = "name";
	public static final String IS_MANUAL_KEY = "is_manual";
	public static final String HOST_KEY = "host";
	public static final String PORT_KEY = "port";
	public static final String AUTH_REQUIRED_KEY = "auth";
	public static final String USER_KEY = "user";
	public static final String PASSWORD_KEY = "password";
	public static final String SENSITIVITY_KEY = "sensitivity";
	public static final String REQUIRE_WIFI_KEY = "require_wifi";
	public static final String TIMEOUT_KEY = "timeout";
	private final String VOLUME_STEP_SIZE_KEY;
	private final int VOLUME_STEP_SIZE_DEFAULT_VALUE;
	private final String HANDLE_HARD_BACK_KEY;
	private final boolean HANDLE_HARD_BACK_DEFAULT;
	private final String KEEP_SCREEN_ON_KEY;
	private final boolean KEEP_SCREEN_ON_DEFAULT;
	
	
	private SharedPreferences mPreferences;

	public Settings(Context context) {
		mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		// Attempt to set default values if they have not yet been set
		PreferenceManager.setDefaultValues(context, R.layout.preferences, false);
		
		VOLUME_STEP_SIZE_KEY = context.getString(R.string.volume_step_size_key);
		VOLUME_STEP_SIZE_DEFAULT_VALUE = context.getResources().getInteger(R.integer.volume_step_size_default_value);
		
		HANDLE_HARD_BACK_KEY = context.getString(R.string.handle_back_hard_key);
		HANDLE_HARD_BACK_DEFAULT = context.getResources().getBoolean(R.bool.handle_back_hard_default);
		
		KEEP_SCREEN_ON_KEY = context.getString(R.string.keep_screen_on_key);
		KEEP_SCREEN_ON_DEFAULT = context.getResources().getBoolean(R.bool.keep_screen_on_default);
	}

	public int getVolumeStep() {
		return mPreferences.getInt(VOLUME_STEP_SIZE_KEY, VOLUME_STEP_SIZE_DEFAULT_VALUE);
	}
	
	public boolean getHandleBack() {
		return mPreferences.getBoolean(HANDLE_HARD_BACK_KEY, HANDLE_HARD_BACK_DEFAULT);
	}
	
	public boolean getKeepScreenOn(){
		return mPreferences.getBoolean(KEEP_SCREEN_ON_KEY, KEEP_SCREEN_ON_DEFAULT);
	}
	
	public String getServerName() {
		return mPreferences.getString(SERVER_NAME_KEY, "");
	}
	
	public String getHost() {
		return mPreferences.getString(HOST_KEY, "");
	}
	
	public int getPort() {
		return mPreferences.getInt(PORT_KEY, 8800);
	}
	
	public String getUser() {
		return mPreferences.getString(USER_KEY, "");
	}
	
	public String getPassword() {
		return mPreferences.getString(PASSWORD_KEY, "");
	}
	
	public boolean isManual() {
		return mPreferences.getBoolean(IS_MANUAL_KEY, false);
	}
	
	public int getTimeout() {
		return Integer.parseInt(mPreferences.getString(TIMEOUT_KEY, "1000"));
	}
	
	public boolean isAuthRequired() {
		return mPreferences.getBoolean(AUTH_REQUIRED_KEY, false);
	}
	
	public boolean requiresWifi() {
		return mPreferences.getBoolean(REQUIRE_WIFI_KEY, true);
	}
	
	public BoxeeServer constructServer() {
		return new BoxeeServer(getServerName(), getHost(), getPort(), isAuthRequired());
	}
	
	public void putServer(BoxeeServer server, boolean isManual) {
		putServer(server.address().getHostAddress(), server.port(), server.name(), server.authRequired(), isManual);
	}
	
	public void putServer(String address, int port, String name, boolean auth, boolean isManual) {
		Log.i(TAG, "Storing server as: " + name + ", manual: " + isManual);
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString(HOST_KEY, address);
		editor.putInt(PORT_KEY, port);
		editor.putString(SERVER_NAME_KEY, name);
		editor.putBoolean(AUTH_REQUIRED_KEY, auth);
		editor.putBoolean(IS_MANUAL_KEY, isManual);
		editor.commit();	
	}

	public void listen(OnSharedPreferenceChangeListener listener) {
		mPreferences.registerOnSharedPreferenceChangeListener(listener);
	}

	public void unlisten(OnSharedPreferenceChangeListener listener) {
		mPreferences.unregisterOnSharedPreferenceChangeListener(listener);
	}
}
