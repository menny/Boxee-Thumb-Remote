/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.menny.android.thumbremote;

import java.util.Random;

import com.menny.android.thumbremote.network.HttpRequest;
import com.menny.android.thumbremote.network.Response;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.util.Log;

public final class ServerStatePoller {
	private static final String TAG = "ServerStatePoller";
	
	private static final int MAX_NETWORK_ERRORS = 3;
	
	private static final long REGULAR_DELAY = 500;
	private static final long BACKGROUND_DELAY = REGULAR_DELAY * 3;
	
	private final Object mWaiter = new Object();
	private final ServerStateUrlsProvider mUrlsProvider;
	private final WifiManager mWifi;
	
	private boolean mInBackground = false;
	private int mErrorsAllowedLeft = MAX_NETWORK_ERRORS;
	
	private boolean mRun;

	public ServerStatePoller(ServerStateUrlsProvider provider, Context context) {
		mUrlsProvider = provider;
		mWifi =  (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		mRun = true;
	}
	private final Thread mPollerThread = new Thread()
	{	
		
		private WifiLock mWifiLock = null;

		public void run() {
			Log.d(TAG, "Starting ServerStatePoller.");
			mWifiLock  = mWifi.createWifiLock(WifiManager.WIFI_MODE_FULL, "ThumbRemote"+(new Random()).nextInt());
			mWifiLock.acquire();
			while(mRun)
			{
				
				try
				{
					mUrlsProvider.startOver();
					while(mUrlsProvider.requiresServerStateData())
					{
						String[] responses = getResponsesFromServer(mUrlsProvider.getServerStateUrls());
						if (responses != null)
						{
							mErrorsAllowedLeft = MAX_NETWORK_ERRORS;
							mUrlsProvider.onServerStateResponsesAvailable(responses);
						}
						else
						{
							mErrorsAllowedLeft--;
							if (mErrorsAllowedLeft == 0)
							{
								mUrlsProvider.onServerStateRetrievalError();
							}
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
								mWaiter.wait(mInBackground? BACKGROUND_DELAY : REGULAR_DELAY );
							} catch (InterruptedException e) {
								e.printStackTrace();
								mRun = false;
							}
						}
					}
				}
			}
			mWifiLock.release();
			Log.d(TAG, "ServerStatePoller ended.");
		}
	};
	
	public void poll() {
		mRun = true;
		mInBackground = false;
		mPollerThread.start();
	}
	
	public void checkStateNow()
	{
		synchronized (mWaiter) {
			mWaiter.notifyAll();
		}
	}
	
	public void moveToBackground() {
		mInBackground = true;
	}
	
	public void comeBackToForeground() {
		mInBackground = false;
	}
	
	
	public void stop() {
		mRun = false;
		synchronized (mWaiter) {
			mWaiter.notifyAll();
		}
	}
	
	private String[] getResponsesFromServer(String[] urls) {
		String[] responses = new String[urls.length];
		for(int i=0; i<urls.length; i++)
		{
			final String url = urls[i];
			Response r = HttpRequest.getHttpResponse(url);
			if (!r.success())
				return null;
			responses[i] = r.response();
		}
		
		return responses;
	}
}
