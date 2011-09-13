package net.evendanan.android.thumbremote.service;

import net.evendanan.android.thumbremote.R;
import net.evendanan.android.thumbremote.ui.RemoteUiActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

public class ServerRemoteService extends Service {
	private static final String TAG = "ServerRemoteService";
    
	public static final String KEY_DATA_TITLE = "KEY_DATA_TITLE";
	
	private static final int NOTIFICATION_PLAYING_ID = 1;

	private NotificationManager mNotificationManager;

	private IBinder mBinder;

	@Override
	public void onCreate() {
		super.onCreate();
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mBinder = new Binder();
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	boolean sticky = false;
    	if (intent != null)
    	{
    		String title = intent.getStringExtra(KEY_DATA_TITLE);
    		if (TextUtils.isEmpty(title))
    			cancelPlayingNotification();
    		else
    		{
    			sticky = true;
    			showPlayingNotification(title);
    		}	
    	}
    	else
    	{
    		cancelPlayingNotification();
    	}
    	if (sticky)
    	{
	        // We want this service to continue running until it is explicitly
	        // stopped, so return sticky.
	        return START_STICKY;
    	}
    	else
    	{
    		return START_NOT_STICKY;
    	}
    }
	
	@Override
	public void onDestroy() {
		super.onDestroy();

		cancelPlayingNotification();
	}
	
	private void showPlayingNotification(String title)
	{
		Notification notification = new Notification(R.drawable.notification_playing, getString(R.string.server_is_playing, title), System.currentTimeMillis());

		Intent notificationIntent = new Intent(this, RemoteUiActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(this,
				getText(R.string.app_name), getString(R.string.server_is_playing, title),
				contentIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		//notification.defaults = 0;// no sound, vibrate, etc.
		// notifying
		mNotificationManager.notify(NOTIFICATION_PLAYING_ID, notification);
	}
	
	private void cancelPlayingNotification()
	{
		mNotificationManager.cancel(NOTIFICATION_PLAYING_ID);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

}
