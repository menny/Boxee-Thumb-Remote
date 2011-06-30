/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.menny.android.thumbremote;

import java.net.InetAddress;

import com.menny.android.thumbremote.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class Settings {
	private static final String TAG = Settings.class.toString();
	
	private final String VOLUME_STEP_SIZE_KEY;
	private final String VOLUME_STEP_SIZE_DEFAULT_VALUE;
	private final String HANDLE_HARD_BACK_KEY;
	private final boolean HANDLE_HARD_BACK_DEFAULT;
	private final String KEEP_SCREEN_ON_KEY;
	private final boolean KEEP_SCREEN_ON_DEFAULT;
	
	
	private SharedPreferences mPreferences;

	public Settings(Context context) {
		mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		// Attempt to set default values if they have not yet been set
		PreferenceManager.setDefaultValues(context, R.layout.preferences, false);
		Resources res = context.getResources();
		
		VOLUME_STEP_SIZE_KEY = res.getString(R.string.settings_key_volume_step_size);
		VOLUME_STEP_SIZE_DEFAULT_VALUE = res.getString(R.string.settings_key_volume_step_size_default_value);
		
		HANDLE_HARD_BACK_KEY = res.getString(R.string.settings_key_handle_back_hard_key);
		HANDLE_HARD_BACK_DEFAULT = res.getBoolean(R.bool.settings_key_handle_back_hard_default);
		
		KEEP_SCREEN_ON_KEY = res.getString(R.string.settings_key_keep_screen_on_key);
		KEEP_SCREEN_ON_DEFAULT = res.getBoolean(R.bool.settings_key_keep_screen_on_default);
	}

	public int getVolumeStep() {
		String volumeStep = mPreferences.getString(VOLUME_STEP_SIZE_KEY, VOLUME_STEP_SIZE_DEFAULT_VALUE);
		return Integer.parseInt(volumeStep);
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
	
	public InetAddress getHost() {
		String address = mPreferences.getString(HOST_KEY, "");
		try
		{
			if (TextUtils.isEmpty(address)) return null;
			return InetAddress.getByName(address);
		}
		catch(Exception e)
		{
			return null;
		}
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
	
	public ServerAddress constructServer() {
		return new ServerAddress("Boxee", getServerName(), isAuthRequired(), getHost(), getPort());
	}
	
	public void putServer(ServerAddress server, boolean isManual) {
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
