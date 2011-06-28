/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.menny.android.thumbremote.ui;

import java.util.ArrayList;
import java.util.HashSet;

import com.menny.android.thumbremote.R;
import com.menny.android.thumbremote.ServerAddress;
import com.menny.android.thumbremote.ServerConnector;
import com.menny.android.thumbremote.ServerState;
import com.menny.android.thumbremote.ServerStatePoller;
import com.menny.android.thumbremote.Settings;
import com.menny.android.thumbremote.ShakeListener.OnShakeListener;
import com.menny.android.thumbremote.UiView;
import com.menny.android.thumbremote.boxee.BoxeeConnector;
import com.menny.android.thumbremote.boxee.BoxeeDiscovererThread;
import com.menny.android.thumbremote.network.HttpRequestBlocking;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class RemoteUiActivity extends Activity implements
		OnSharedPreferenceChangeListener, BoxeeDiscovererThread.Receiver, OnClickListener, OnShakeListener, UiView {

	public final static String TAG = RemoteUiActivity.class.toString();

	//
	private final static HashSet<Character> msPunctuation = new HashSet<Character>();
	static
	{
		String punctuation = "!@#$%^&*()[]{}/?|'\",.<>\n ";
		for(char c : punctuation.toCharArray())
			msPunctuation.add(c);
	}
	
	private static RemoteUiActivity msActivity = null;

	public static void onExternalImportantEvent(String event) {
		final RemoteUiActivity realActivity = msActivity;
		if (realActivity != null)
		{
			Log.i(TAG, "Got an important external event '"+event+"'!");
			realActivity.pauseIfPlaying();
		}
	}
	
	public static void onNetworkAvailable()
	{
		final RemoteUiActivity realActivity = msActivity;
		if (realActivity != null && !realActivity.mThisAcitivityPaused)
		{
			Log.i(TAG, "Got network! Trying to reconnect...");
			realActivity.mRemote = new BoxeeConnector();
			realActivity.setServer();
		}
	}
	
	private static final int MESSAGE_MEDIA_PLAYING_CHANGED = 97565;
	private static final int MESSAGE_MEDIA_PLAYING_PROGRESS_CHANGED = MESSAGE_MEDIA_PLAYING_CHANGED + 1;
	private static final int MESSAGE_MEDIA_METADATA_CHANGED = MESSAGE_MEDIA_PLAYING_PROGRESS_CHANGED + 1;
	
	// Menu items
	private static final int MENU_SETTINGS = Menu.FIRST;
	private static final int MENU_HELP = MENU_SETTINGS+1;
	private static final int MENU_EXIT = MENU_HELP+1;
	
	// ViewFlipper
	private static final int PAGE_NOTPLAYING = 0;
	private static final int PAGE_NOWPLAYING = 1;
	private ViewFlipper mFlipper;

	// Other Views
	ImageView mImageThumbnail;
	Button mButtonPlayPause;
	TextView mTextTitle;
	TextView mTextElapsed;
	TextView mDuration;
	ProgressBar mElapsedBar;

	private static final int NOTIFICATION_PLAYING_ID = 1;

	private static final int DIALOG_NO_PASSWORD = 1;
	
	private NotificationManager mNotificationManager;

	boolean mThisAcitivityPaused = true;
	
	private Settings mSettings;
	private ServerConnector mRemote;
	private BoxeeDiscovererThread mServerDiscoverer;
	private ServerAddress mServerAddress = null;
	private ServerStatePoller mStatePoller = null; 

	//Not ready for prime time
	//private ShakeListener mShakeDetector;
	
	private Point mTouchPoint = new Point();
	private boolean mDragged = false;
	private boolean mIsMediaActive = false;
	private ProgressDialog mPleaseWaitDialog;

	private Handler mHandler;

	private final Runnable mRequestStatusUpdateRunnable = new Runnable() {
		@Override
		public void run() {
			if (mStatePoller != null)
				mStatePoller.checkStateNow();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MESSAGE_MEDIA_PLAYING_CHANGED:
					refreshPlayingStateChanged();
					break;
				case MESSAGE_MEDIA_PLAYING_PROGRESS_CHANGED:
					refreshPlayingProgressChanged();
					break;
				case MESSAGE_MEDIA_METADATA_CHANGED:
					refreshMetadataChanged();
					break;
				}
			}
		};
		
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		mRemote = new BoxeeConnector();

		setContentView(R.layout.main);

		// Setup flipper
		mFlipper = (ViewFlipper) findViewById(R.id.now_playing_flipper);
		mFlipper.setInAnimation(this, android.R.anim.slide_in_left);
		mFlipper.setOutAnimation(this, android.R.anim.slide_out_right);

		// Find other views
		mImageThumbnail = (ImageView) findViewById(R.id.thumbnail);
		mButtonPlayPause = (Button) findViewById(R.id.buttonPlayPause);
		mTextTitle = (TextView) findViewById(R.id.textNowPlayingTitle);
		mTextElapsed = (TextView) findViewById(R.id.textElapsed);
		mDuration = (TextView) findViewById(R.id.textDuration);
		mElapsedBar = (ProgressBar) findViewById(R.id.progressTimeBar);

		mSettings = new Settings(this);

		loadPreferences();

		setButtonAction(R.id.back, KeyEvent.KEYCODE_BACK);
		setButtonAction(R.id.buttonPlayPause, 0);
		setButtonAction(R.id.buttonStop, 0);
		setButtonAction(R.id.buttonSmallSkipBack, 0);
		setButtonAction(R.id.buttonSmallSkipFwd, 0);
		
		//mShakeDetector = new ShakeListener(getApplicationContext());
		//mShakeDetector.setOnShakeListener(this);
		msActivity = this;
		
		mStatePoller = new ServerStatePoller(mRemote);
		mStatePoller.poll();
		
		startHelpOnFirstRun();
	}

	private void startHelpOnFirstRun() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		final boolean ranBefore = preferences.getBoolean("has_ran_before", false);
		if (!ranBefore)
		{
			Editor e = preferences.edit();
			e.putBoolean("has_ran_before", true);
			e.commit();
			
			startHelpActivity();
		}
	}

	private void startHelpActivity() {
		Intent i = new Intent(getApplicationContext(), HelpUiActivity.class);
		startActivity(i);
	}
	
	private void startSetupActivity() {
		Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
		startActivity(i);
	}
	
	@Override
	protected void onPause() {
		mThisAcitivityPaused = true;
		//mShakeDetector.pause();
		mSettings.unlisten(this);
		mHandler.removeCallbacks(mRequestStatusUpdateRunnable);
		
		super.onPause();
		if (mStatePoller != null)
			mStatePoller.moveToBackground();
		
		if (mPleaseWaitDialog != null)
			mPleaseWaitDialog.dismiss();
		mPleaseWaitDialog = null;
	}
	
	@Override
	protected void onDestroy() {
		msActivity = null;
		mNotificationManager.cancel(NOTIFICATION_PLAYING_ID);
		if (mStatePoller != null)
			mStatePoller.stop();
		mStatePoller = null;
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mThisAcitivityPaused = false;
		
		mSettings.listen(this);
		
		if ((mServerAddress == null || !mServerAddress.valid()) && !mServerDiscoverer.isLookingForServers())
			setServer();
		
		//mShakeDetector.resume();
		
		mImageThumbnail.setKeepScreenOn(mSettings.getKeepScreenOn());
		
		if (mStatePoller == null)
		{
			mStatePoller = new ServerStatePoller(mRemote);
			mStatePoller.poll();
		}
		else
		{
			mStatePoller.comeBackToForeground();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_SETTINGS, 0, R.string.settings).setIcon(
				android.R.drawable.ic_menu_preferences);
		
		menu.add(Menu.NONE, MENU_HELP, 0, R.string.help).setIcon(
				android.R.drawable.ic_menu_help);
		
		menu.add(Menu.NONE, MENU_EXIT, 0, R.string.exit_app).setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId())
		{
		case MENU_EXIT:
			finish();
			return true;
		case MENU_HELP:
			startHelpActivity();
			return true;
		case MENU_SETTINGS:
			startSetupActivity();
			return true;
		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}
	
	private void pauseIfPlaying()
	{
		if (mRemote.isMediaPlaying())
		{
			remoteFlipPlayPause();
		}
	}

	@Override
	public void onClick(View v) {
		final int id = v.getId();
		switch (id) {
		
		case R.id.buttonPlayPause:
			remoteFlipPlayPause();
			break;

		case R.id.buttonStop:
			remoteStop();
			break;

		case R.id.buttonSmallSkipBack:
		case R.id.buttonSmallSkipFwd:
			final int duration = hmsToSeconds(mRemote.getMediaTotalTime());
			if (duration > 0)
			{
				final double howFar = (id == R.id.buttonSmallSkipFwd)? 10f : -10f;
				final double newSeekPosition = howFar * 100f / duration;
				remoteSeek(newSeekPosition);
			}
			break;

		case R.id.back:
			remoteBack();
			break;
		}

	}

	void requestUpdateASAP(int delay_ms) {
		mHandler.postDelayed(mRequestStatusUpdateRunnable,delay_ms);
	}

	private void flipTo(int page) {
		if (mFlipper.getDisplayedChild() != page)
			mFlipper.setDisplayedChild(page);
	}

	@Override
	public void onPlayingStateChanged(ServerState serverState) {
		mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_MEDIA_PLAYING_CHANGED));
	}
	
	@Override
	public void onPlayingProgressChanged(ServerState serverState) {
		mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_MEDIA_PLAYING_PROGRESS_CHANGED));		
	}
	
	@Override
	public void onMetadataChanged(ServerState serverState) {
		mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_MEDIA_METADATA_CHANGED));
	}
	
	private void refreshPlayingStateChanged() {
		final boolean isPlaying = mRemote.isMediaPlaying();
		final boolean newIsMediaActive = mRemote.isMediaActive();
		final boolean mediaActiveChanged = newIsMediaActive != mIsMediaActive;
		mIsMediaActive = newIsMediaActive;

		if (!mediaActiveChanged) return;
		
		if (mIsMediaActive) {
			mButtonPlayPause.setBackgroundDrawable(getResources().getDrawable(
					isPlaying ? R.drawable.icon_osd_pause : R.drawable.icon_osd_play));
	
			final String title = mRemote.getMediaTitle();
			mTextTitle.setText(title);
	
			refreshPlayingProgressChanged();
			
			flipTo(PAGE_NOWPLAYING);
			Notification notification = new Notification(R.drawable.notification_playing, getString(R.string.server_is_playing, title), System.currentTimeMillis());

			Intent notificationIntent = new Intent(this, RemoteUiActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

			notification.setLatestEventInfo(getApplicationContext(),
					getText(R.string.app_name), getString(R.string.server_is_playing, title),
					contentIntent);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			notification.flags |= Notification.FLAG_NO_CLEAR;
			//notification.defaults = 0;// no sound, vibrate, etc.
			// notifying
			mNotificationManager.notify(NOTIFICATION_PLAYING_ID, notification);
		}
		else
		{
			flipTo(PAGE_NOTPLAYING);
			
			mTextTitle.setText("");
			mNotificationManager.cancel(NOTIFICATION_PLAYING_ID);
			//no need to keep this one alive. Right?
			if (mThisAcitivityPaused)
			{
				if (mStatePoller != null)
					mStatePoller.stop();
				mStatePoller = null;
			}
		}
	}
	
	private void refreshPlayingProgressChanged()
	{
		mDuration.setText(mRemote.getMediaTotalTime());
		
		mTextElapsed.setText(mRemote.getMediaCurrentTime());

		mElapsedBar.setProgress(mRemote.getMediaProgressPercent());
	}
	
	private void refreshMetadataChanged() {
		mIsMediaActive = mRemote.isMediaActive();
		if (mIsMediaActive)
		{
			mImageThumbnail.setImageBitmap(mRemote.getMediaPoster());
			mTextTitle.setText(mRemote.getMediaTitle());
		}
		else
		{
			mImageThumbnail.setImageResource(R.drawable.remote_background);
			mTextTitle.setText("");
		}
	}

	/**
	 * Handler an android keypress and send it to boxee if appropriate.
	 */
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		final char unicodeChar = (char)event.getUnicodeChar();
		
		Log.d(TAG, "Unicode is " + ((int)unicodeChar));

		
		if (Character.isLetterOrDigit(unicodeChar) || msPunctuation.contains(unicodeChar)) {
			remoteKeypress(unicodeChar);
			
			return true;
		}

		switch (keyCode) {

		case KeyEvent.KEYCODE_BACK:
			if (mSettings.getHandleBack())
			{
				remoteBack();
				return true;
			}
			else
			{
				super.onKeyDown(keyCode, event);
			}

		case KeyEvent.KEYCODE_DPAD_CENTER:
			remoteSelect();
			return true;

		case KeyEvent.KEYCODE_DPAD_DOWN:
			remoteDown();
			return true;

		case KeyEvent.KEYCODE_DPAD_UP:
			remoteUp();
			return true;

		case KeyEvent.KEYCODE_DPAD_LEFT:
			remoteLeft();
			return true;

		case KeyEvent.KEYCODE_DPAD_RIGHT:
			remoteRight();
			return true;

		case KeyEvent.KEYCODE_VOLUME_UP:
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			final int volumeFactor = (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)? -1 : 1;
				new DoServerRemoteAction(this, false) {
				@Override
				protected void callRemoteFunction() throws Exception {
					int volume = mRemote.getVolume();
					int newVolume = Math.max(0, Math.min(100, volume + (volumeFactor * mSettings.getVolumeStep())));
					mRemote.setVolume(newVolume);
				}
			}.execute();
			return true;
			
		default:
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		int x = (int) event.getX();
		int y = (int) event.getY();
		int sensitivity = 30;
		switch (event.getAction()) {

		case MotionEvent.ACTION_UP:
			if (!mDragged) {
				remoteSelect();
				return true;
			}
			break;

		case MotionEvent.ACTION_DOWN:
			mTouchPoint.x = x;
			mTouchPoint.y = y;
			mDragged = false;
			return true;

		case MotionEvent.ACTION_MOVE:
			if (x - mTouchPoint.x > sensitivity) {
				remoteRight();
				mTouchPoint.x += sensitivity;
				mTouchPoint.y = y;
				mDragged = true;
				return true;
			} else if (mTouchPoint.x - x > sensitivity) {
				remoteLeft();
				mTouchPoint.x -= sensitivity;
				mTouchPoint.y = y;
				mDragged = true;
				return true;
			} else if (y - mTouchPoint.y > sensitivity) {
				remoteDown();
				mTouchPoint.y += sensitivity;
				mTouchPoint.x = x;
				mDragged = true;
				return true;
			} else if (mTouchPoint.y - y > sensitivity) {
				remoteUp();
				mTouchPoint.y -= sensitivity;
				mTouchPoint.x = x;
				mDragged = true;
				return true;
			}
			break;
		}

		return false;
	}

	/**
	 * Set up a navigation button in the UI. Sets the focus to false so that we
	 * can capture KEYCODE_DPAD_CENTER.
	 * 
	 * @param id
	 *            id of the button in the resource file
	 * 
	 * @param keycode
	 *            keyCode we should send to Boxee when this button is pressed
	 */
	private void setButtonAction(int id, final int keyCode) {
		Button button = (Button) findViewById(id);
		button.setFocusable(false);
		button.setTag(new Integer(keyCode));
		button.setOnClickListener(this);
	}
	
	/**
	 * Set the state of the application based on prefs. This should be called
	 * after every preference change or when starting up.
	 * 
	 * @param prefs
	 */
	private void loadPreferences() {

		// Setup the proper pageflipper page:
		flipTo(PAGE_NOTPLAYING);
		
		// Setup the HTTP timeout.
		int timeout_ms = mSettings.getTimeout();
		HttpRequestBlocking.setTimeout(timeout_ms);

		// Parse the credentials, if needed.
		String user = mSettings.getUser();
		String password = mSettings.getPassword();
		if (!TextUtils.isEmpty(password)) {
			HttpRequestBlocking.setUserPassword(user, password);
		}

		setServer();
	}

	private void setServer() {
		// Only set the host if manual. Otherwise we'll auto-detect it with
		// Discoverer -> addAnnouncedServers
		if (mSettings.isManual()) {
			mRemote.setServer(mSettings.constructServer());
			requestUpdateASAP(100);
		}
		else {
			if (mPleaseWaitDialog != null)
				mPleaseWaitDialog.dismiss();
			
			mPleaseWaitDialog = ProgressDialog.show(this, "", "Looking for a server...", true);
			mServerDiscoverer = new BoxeeDiscovererThread(this, this);
			mServerDiscoverer.start();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id)
		{
		case DIALOG_NO_PASSWORD:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder
				.setTitle("Credentials required")
				.setMessage("The server "+mServerAddress.name()+" requires username and password in order to be controlled.\nWould you like to enter them now?")
			       .setCancelable(true)
			       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   startSetupActivity();
			        	   dialog.dismiss();
			           }
			       })
			       .setNegativeButton("No", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });
			AlertDialog alert = builder.create();
			
			return alert;
		}
		return super.onCreateDialog(id);
	}

	/**
	 * Callback when user alters preferences.
	 */
	public void onSharedPreferenceChanged(SharedPreferences prefs, String pref) {
		loadPreferences();
	}

	/**
	 * Called when the discovery request we sent in onCreate finishes. If we
	 * find a server matching mAutoName, we use that.
	 * 
	 * @param servers
	 *            list of discovered servers
	 */
	public void addAnnouncedServers(ArrayList<ServerAddress> servers) {
		
		if (mPleaseWaitDialog != null)
			mPleaseWaitDialog.dismiss();
		mPleaseWaitDialog = null;
		
		
		// This condition shouldn't ever be true.
		if (mSettings.isManual()) {
			Log.d(TAG, "Skipping announced servers. Set manually");
			return;
		}

		String preferred = mSettings.getServerName();

		for (int k = 0; k < servers.size(); ++k) {
			ServerAddress server = servers.get(k);
			if (server.name().equals(preferred) || TextUtils.isEmpty(preferred)) {
				if (!server.valid()) {
					Toast.makeText(getApplicationContext(), 
							String.format("Found '%s' but looks broken", server.name()),
							Toast.LENGTH_SHORT).show();
					continue;
				} else {
					mServerAddress = server;
					mRemote.setServer(mServerAddress);
					final String serverName = server.name();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setTitle(getString(R.string.app_name)+" - "+serverName);
						}
					});
					

					if (server.authRequired())
					{
						if (!HttpRequestBlocking.hasCredentials())
						{
							showDialog(DIALOG_NO_PASSWORD);
						}
					}
					
					requestUpdateASAP(100);
					return;
				}
			}
		}

		mServerAddress = null;
		
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setTitle(getString(R.string.app_name));
			}
		});
		Toast.makeText(getApplicationContext(), 
				"Could not find any servers. Try specifying it in the Settings (press MENU)",
				Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onShake() {
		Log.d(TAG, "Shake detect!");
		pauseIfPlaying();
	}
		
	private void remoteFlipPlayPause() {
		new DoServerRemoteAction(this, false) {
			@Override
			protected void callRemoteFunction() throws Exception {
				mRemote.flipPlayPause();
			}
		}.execute();
	}

	private void remoteBack() {
		new DoServerRemoteAction(this, false) {
			@Override
			protected void callRemoteFunction() throws Exception {
				mRemote.back();
			}
		}.execute();
	}

	private void remoteSeek(final double newSeekPosition) {
		new DoServerRemoteAction(this, false) {
			@Override
			protected void callRemoteFunction() throws Exception {
				mRemote.seekRelative(newSeekPosition);
			}
		}.execute();
	}

	private void remoteStop() {
		new DoServerRemoteAction(this, false) {
			@Override
			protected void callRemoteFunction() throws Exception {
				mRemote.stop();
			}
		}.execute();
	}

	private void remoteLeft() {
		new DoServerRemoteAction(this, false) {
			@Override
			protected void callRemoteFunction() throws Exception {
				mRemote.left();
			}
		}.execute();
	}

	private void remoteRight() {
		new DoServerRemoteAction(this, false) {
			@Override
			protected void callRemoteFunction() throws Exception {
				mRemote.right();
			}
		}.execute();
	}

	private void remoteUp() {
		new DoServerRemoteAction(this, false) {
			@Override
			protected void callRemoteFunction() throws Exception {
				mRemote.up();
			}
		}.execute();
	}

	private void remoteDown() {
		new DoServerRemoteAction(this, false) {
			@Override
			protected void callRemoteFunction() throws Exception {
				mRemote.down();
			}
		}.execute();
	}

	private void remoteSelect() {
		new DoServerRemoteAction(this, false) {
			@Override
			protected void callRemoteFunction() throws Exception {
				mRemote.select();
			}
		}.execute();
	}

	private void remoteKeypress(final char unicodeChar) {
		new DoServerRemoteAction(this, false) {
			@Override
			protected void callRemoteFunction() throws Exception {
				mRemote.keypress(unicodeChar);
			}
		}.execute();
	}

	private static int hmsToSeconds(String hms) {
		if (TextUtils.isEmpty(hms))
			return 0;

		int seconds = 0;
		String[] times = hms.split(":");

		// seconds
		seconds += Integer.parseInt(times[times.length - 1]);

		// minutes
		if (times.length >= 2)
			seconds += Integer.parseInt(times[times.length - 2]) * 60;

		// hours
		if (times.length >= 3)
			seconds += Integer.parseInt(times[times.length - 3]) * 3600;

		return seconds;
	}
}