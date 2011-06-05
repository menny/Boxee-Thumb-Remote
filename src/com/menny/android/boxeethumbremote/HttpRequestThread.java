package com.menny.android.boxeethumbremote;

import java.net.MalformedURLException;

import android.util.Log;

/**
 * A thread object which performs an HTTP get request, which can be used to
 * perform non-blocking fetches.
 */
class HttpRequestThread extends Thread {
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