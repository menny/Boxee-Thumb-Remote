/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.menny.android.thumbremote.network;

import java.net.MalformedURLException;

import android.util.Log;

/**
 * A thread object which performs an HTTP get request, which can be used to
 * perform non-blocking fetches.
 */
public class HttpRequestThread extends Thread {
	static final String TAG = "HttpRequest";
	final private CallbackHandler mHandler;
	final private HttpRequestBlocking mReq;

	/**
	 * Constructor
	 * 
	 * @param url
	 *            url to fetch
	 * 
	 * @param handler
	 *            handler which will receive notification when the fetch
	 *            completes or has an error
	 */
	public HttpRequestThread(String url, CallbackHandler handler)
			throws MalformedURLException {
		mReq = new HttpRequestBlocking(url);
		mHandler = handler;
		Log.d(TAG, String.format("started request(remote=%s)", url));
		start();
	}

	public void run() {
		Log.d(TAG, "Get thread started");
		mReq.fetch();
		mHandler.HandleResponse(mReq.success(), mReq.response());
	}
}
