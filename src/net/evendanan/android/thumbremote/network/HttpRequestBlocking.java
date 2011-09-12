/* The following code was re-written by Menny Even Danan, from code taken from Supware (http://code.google.com/p/boxeeremote/source/browse/branches/net.supware.boxee/src/net/supware/boxee/HttpRequestBlocking.java)
 * and is released under the APACHE 2.0 license
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package net.evendanan.android.thumbremote.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;

/**
 * Performs a blocking HTTP get request, without much flexibility.
 */
class HttpRequestBlocking implements HttpBlocking {
	private static final String TAG = "HttpRequestBlocking";
	private final URL mUrl;
	private final String mUser;
	private final String mPassword;
	private final int mTimeout;
	
	HttpRequestBlocking(String url, int timeout, String user, String password) throws MalformedURLException {
		mUrl = new URL(url);
		mUser = user;
		mPassword = password;
		mTimeout = timeout;
	}

	/**
	 * Perform the blocking fetch.
	 * @throws IOException 
	 */
	public Response fetch() throws IOException {
		if (mUrl == null)
			return new Response(false, null);

		Log.d(TAG, "Fetching " + mUrl.toString());
		
		HttpURLConnection connection = (HttpURLConnection) mUrl.openConnection();
		connection.setConnectTimeout(mTimeout);
		connection.setReadTimeout(mTimeout);
		maybeAddAuthentication(connection);
		connection.connect();
		InputStream is = connection.getInputStream();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		int bytesRead;
		byte[] buffer = new byte[1024];
		while ((bytesRead = is.read(buffer)) > 0) {
			os.write(buffer, 0, bytesRead);
		}
		os.close();
		is.close();
		Log.d(TAG, String.format(
				"finished request(size=%d, remote=%s)", os.size(), mUrl
						.toString()));
		String result = os.toString();
		boolean success = connection.getResponseCode() == 200;
		return new Response(success, result);
	}

	private void maybeAddAuthentication(HttpURLConnection conn) {
		if (mPassword != null && mPassword.length() != 0) {
			byte[] enc = iharder.base64.Base64.encodeBytesToBytes((mUser + ":" + mPassword).getBytes());
			conn.setRequestProperty("Authorization", "Basic " + new String(enc));
		}
	}
}