/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.menny.android.thumbremote.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.text.TextUtils;
import android.util.Log;

/**
 * Performs a blocking HTTP get request, without much flexibility.
 */
public class HttpRequestBlocking {
	public static class Response
	{
		private final boolean mSuccess;
		private final String mResponse;
		
		Response(boolean success, String response)
		{
			mSuccess = success;
			mResponse = response;
		}
		
		/**
		 * Returns whether the fetch resulted in a 200.
		 */
		public boolean success() {
			return mSuccess;
		}

		/**
		 * Returns the fetched content, or null if the fetch failed.
		 */
		public String response() {
			return mResponse;
		}
	}
	
	private static final String TAG = "HttpRequestBlocking";
	private URL mUrl;

	// We set these for all requests.
	private static int mTimeout = 2000;
	private static String mPassword;
	private static String mUser;

	public static void setTimeout(int timeout_ms) {
		mTimeout = timeout_ms;
	}

	public static void setUserPassword(String user, String password) {
		mUser = user;
		mPassword = password;
	}
	
	public static boolean hasCredentials()
	{
		return !TextUtils.isEmpty(mUser) && !TextUtils.isEmpty(mPassword);
	}

	public static Response getHttpResponse(final String url)
	{
		HttpRequestBlocking requester = new HttpRequestBlocking(url);
		return requester.fetch();
	}
	
	/**
	 * Constructor.
	 */
	private HttpRequestBlocking(String url) {
		try {
			mUrl = new URL(url);
		} catch (MalformedURLException e) {
			mUrl = null;
			Log.e(TAG, "MalformedURLException: "+url);
			e.printStackTrace();
		}
	}

	

	/**
	 * Perform the blocking fetch.
	 */
	public Response fetch() {
		if (mUrl == null)
			return new Response(false, null);

		Log.d(TAG, "Fetching " + mUrl.toString());
		
		try {
			HttpURLConnection connection = (HttpURLConnection) mUrl.openConnection();
			connection.setConnectTimeout(mTimeout);
			connection.setReadTimeout(mTimeout);
			maybeAddAuthentication(connection);
			connection.connect();
			InputStream is = connection.getInputStream();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			int bytesRead;
			byte[] buffer = new byte[1024];
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			os.flush();
			os.close();
			is.close();
			Log.d(TAG, String.format(
					"finished request(size=%d, remote=%s)", os.size(), mUrl
							.toString()));
			String result = os.toString();
			boolean success = connection.getResponseCode() == 200;
			return new Response(success, result);
		} catch (IOException e) {
			return new Response(false, null);
		}
	}

	private void maybeAddAuthentication(HttpURLConnection conn) {
		if (mPassword != null && mPassword.length() != 0) {
			byte[] enc = iharder.base64.Base64.encodeBytesToBytes((mUser + ":" + mPassword).getBytes());
			conn.setRequestProperty("Authorization", "Basic " + new String(enc));
		}
	}
}