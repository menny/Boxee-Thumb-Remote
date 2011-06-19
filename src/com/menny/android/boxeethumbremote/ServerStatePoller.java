/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.menny.android.boxeethumbremote;

import java.io.IOException;
import java.net.URLEncoder;

import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

public final class ServerStatePoller {
	private static final String TAG = ServerStatePoller.class.toString();
	
	public static final int MESSAGE_NOW_PLAYING_UPDATED = 1;
	public static final int MESSAGE_MEDIA_METADATA_UPDATED = 2;

	public static final long REGULAR_DELAY = 500;
	public static final long BACKGROUND_DELAY = REGULAR_DELAY * 3;
	
	private final Object mWaiter = new Object();
	private final NowPlaying mPlaying;
	private final Handler mHandler;
	private final BoxeeRemote mRemote;
	
	private long mWaitTime = REGULAR_DELAY;

	
	private boolean mRun;

	public ServerStatePoller(Handler handler, BoxeeRemote remote, NowPlaying playing) {
		mRemote = remote;
		mHandler = handler;
		mPlaying = playing;
		
		mRun = true;
	}
	private final Thread mPollerThread = new Thread()
	{	
		
		public void run() {
			Log.d(TAG, "Starting ServerStatePoller.");
			String currentlyRunningTitle = "";
			while(mRun)
			{
				try
				{
					if (getCurrentlyPlayingStatus())
					{
						//GUI! Update!
						mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_NOW_PLAYING_UPDATED));
						//now we check for thumb. If needed (a new title, or no title)
						if (!currentlyRunningTitle.equals(mPlaying.getTitle()))
						{
							currentlyRunningTitle = mPlaying.getTitle();
							if (currentlyRunningTitle == null) currentlyRunningTitle = "";
							getThumbnail();
							
							mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_MEDIA_METADATA_UPDATED));
						}
					}
				}
				finally
				{
					if (mRun)
					{
						synchronized (mWaiter) {
							//refreshing state every 500 ms
							try {
								mWaiter.wait(mWaitTime );
							} catch (InterruptedException e) {
								e.printStackTrace();
								mRun = false;
							}
						}
					}
				}
			}
			Log.d(TAG, "ServerStatePoller ended.");
		}
	};
	
	public void poll() {
		mRun = true;
		mWaitTime = REGULAR_DELAY;
		mPollerThread.start();
	}
	
	public void checkStateNow()
	{
		synchronized (mWaiter) {
			mWaiter.notifyAll();
		}
	}
	
	public void moveToBackground() {
		mWaitTime = BACKGROUND_DELAY;
	}
	
	public void comeBackToForeground() {
		mWaitTime = REGULAR_DELAY;
	}
	
	
	public void stop() {
		mRun = false;
		synchronized (mWaiter) {
			mWaiter.notifyAll();
		}
	}
	
	private boolean getCurrentlyPlayingStatus() {

		HttpRequestBlocking r = new HttpRequestBlocking(mRemote.getRequestString("getcurrentlyplaying()"));
		r.fetch();
		
		if (!r.success())
			return false;
		//I'm clearing AFTER, so I wont clear on network errors
		mPlaying.clear();
		mPlaying.addEntriesFrom(r.response());

		r = new HttpRequestBlocking(mRemote.getRequestString("getguistatus()"));
		r.fetch();
		
		if (!r.success())
			return false;

		mPlaying.addEntriesFrom(r.response());
		
		return true;
	}

	private boolean getThumbnail() {
		final String request = mRemote.getRequestPrefix()
				+ String.format("getthumbnail(%s)", URLEncoder.encode(mPlaying.getThumbnailUrl()));
		HttpRequestBlocking r;
		r = new HttpRequestBlocking(request);
		r.fetch();
		if (!r.success()) {
			return false;
		}
		String shorter = r.response().replaceAll("<html>", "").replaceAll(
				"</html>", "");
		byte[] thumb;
		try {
			thumb = iharder.base64.Base64.decode(shorter.getBytes());
		} catch (IOException e) {
			thumb = null;
			e.printStackTrace();
		}
		if (thumb != null)
			mPlaying.setThumbnail(BitmapFactory.decodeByteArray(thumb, 0, thumb.length));
		else
			mPlaying.setThumbnail(null);
		return true;
	}
}
