/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.menny.android.thumbremote.boxee;

import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.menny.android.thumbremote.R;
import com.menny.android.thumbremote.Remote;
import com.menny.android.thumbremote.Server;
import com.menny.android.thumbremote.network.CallbackHandler;
import com.menny.android.thumbremote.network.HttpRequestThread;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

/**
 * Controls interaction with the Boxee server. No UI management.
 * 
 * @author chatham
 * 
 */
public class BoxeeRemote implements Remote {

	/**
	 * Keycodes, from boxee API
	 */
	private final static int CODE_LEFT = 272;
	private final static int CODE_RIGHT = 273;
	private final static int CODE_UP = 270;
	private final static int CODE_DOWN = 271;
	private final static int CODE_SELECT = 256;
	private final static int CODE_BACK = 275;
	//special key for backspace
	private final static int CODE_BACKSPACE = 61704;
	private final static int KEY_ASCII = 0xF100;

	static public final int BAD_PORT = -1;
	public final static String TAG = BoxeeRemote.class.toString();
	private ErrorHandler mError;
	private String mHost;
	private int mPort;
	private boolean mRequireWifi;

	private NetworkInfo mWifiInfo;

	public BoxeeRemote(Context context, ErrorHandler error) {
		ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiInfo = connectivity
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		mError = error;
		mWifiInfo = wifiInfo;
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#changeVolume(int)
	 */
	@Override
	public void changeVolume(final int percent) {
		if (!hasServers())
			return;

		final String getvolume = getRequestPrefix()
				+ String.format("getVolume()");
		// TODO: Move around
		final Pattern VOLUME_RE = Pattern.compile("^<li>([0-9]+)",
				Pattern.MULTILINE);

		// First ask for the current volume.
		try {
			new HttpRequestThread(getvolume, new CallbackHandler() {
				public void HandleResponse(boolean success, String resp) {
					if (!success) {
						mError.ShowError("Problem fetching URL " + getvolume, false);
					}

					// Parse out the current volume and send a setvolume
					// request.
					Matcher m = VOLUME_RE.matcher(resp);
					if (m != null && m.find()) {
						int current_volume = Integer.parseInt(m.group(1));
						int new_volume = Math.max(0, Math.min(100,
								current_volume + percent));
						Log.d(TAG, "Setting volume to " + new_volume);
						final String setvolume = getRequestPrefix()
								+ String.format("setVolume(%d)", new_volume);
						sendHttpCommand(setvolume);
					} else {
						Log.d(TAG, "Could not parse RE " + resp);
					}
				}
			});
		} catch (MalformedURLException e) {
			mError.ShowError("Malformed URL: " + getvolume, true);
		}
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#getRequestPrefix()
	 */
	@Override
	public String getRequestPrefix() {
		return String.format("http://%s:%d/xbmcCmds/xbmcHttp?command=", mHost,
				mPort);
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#getRequestString(java.lang.String)
	 */
	@Override
	public String getRequestString(String command) {
		return getRequestPrefix() + command;
	}

	private void sendHttpCommand(final String request) {
		Log.d(TAG, "Fetching " + request);
		try {
			new HttpRequestThread(request, new CallbackHandler() {
				public void HandleResponse(boolean success, String resp) {
					if (!success) {
						mError.ShowError("Problem fetching URL " + request, true);
					}
				}
			});
		} catch (MalformedURLException e) {
			mError.ShowError("Malformed URL: " + request, false);
		}
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#up()
	 */
	@Override
	public void up() {
		sendKeyPress(CODE_UP);
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#down()
	 */
	@Override
	public void down() {
		sendKeyPress(CODE_DOWN);
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#left()
	 */
	@Override
	public void left() {
		sendKeyPress(CODE_LEFT);
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#right()
	 */
	@Override
	public void right() {
		sendKeyPress(CODE_RIGHT);
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#back()
	 */
	@Override
	public void back() {
		sendKeyPress(CODE_BACK);
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#select()
	 */
	@Override
	public void select() {
		sendKeyPress(CODE_SELECT);
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#sendBackspace()
	 */
	@Override
	public void sendBackspace()
	{
		sendKeyPress(CODE_BACKSPACE);
	}
	
	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#keypress(int)
	 */
	@Override
	public void keypress(int unicode) {
		sendKeyPress(unicode + KEY_ASCII);
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#flipPlayPause()
	 */
	@Override
	public void flipPlayPause() {
		sendHttpCommand(getRequestPrefix() + "Pause");
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#stop()
	 */
	@Override
	public void stop() {
		sendHttpCommand(getRequestPrefix() + "Stop");
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#seek(double)
	 */
	@Override
	public void seek(double pct) {
		sendHttpCommand(getRequestPrefix() + "SeekPercentageRelative(" + pct
				+ ")");
	}

	private void sendKeyPress(int keycode) {
		if (!hasServers()) {
			mError.ShowError("Run scan or go to settings and set the host", true);
			return;
		}

		if (mPort == BAD_PORT) {
			mError.ShowError("Run scan or go to settings and set the port", true);
			return;
		}

		if (mRequireWifi && !mWifiInfo.isAvailable()) {
			mError.ShowError(R.string.no_wifi, true);
			return;
		}

		String request = getRequestPrefix()
				+ String.format("SendKey(%d)", keycode);

		sendHttpCommand(request);
	}

	@Override
	public void setServer(Server server) {
		mHost = server.address().getHostAddress();
		mPort = server.port();
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#setRequireWifi(boolean)
	 */
	@Override
	public void setRequireWifi(boolean requireWifi) {
		mRequireWifi = requireWifi;
	}

	/* (non-Javadoc)
	 * @see com.menny.android.thumbremote.boxee.Remote#hasServers()
	 */
	@Override
	public boolean hasServers() {
		return !TextUtils.isEmpty(mHost);
	}

}