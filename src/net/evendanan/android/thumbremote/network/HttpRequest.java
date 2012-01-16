package net.evendanan.android.thumbremote.network;

import android.text.TextUtils;
import android.util.Log;

/*
 * Controls the live cycle of a network connection.
 * Tries to keep the connection alive as long as possible, thus, reusing HTTP connections, and reduce sockets and objects creations.
 */
public class HttpRequest {

	private static final String TAG = "HttpRequest";
	// We set these for all requests.
	private static int msTimeout = 2000;
	private static String msPassword;
	private static String msUser;
	private static HttpBlocking msRequester = null;
	
	public synchronized static void setTimeout(int timeout_ms) {
		close();
		msTimeout = timeout_ms;
	}

	public synchronized static void setUserPassword(String user, String password) {
		close();
		msUser = user;
		msPassword = password;
	}
	
	public synchronized static boolean hasCredentials()
	{
		return !TextUtils.isEmpty(msUser) && !TextUtils.isEmpty(msPassword);
	}

	public synchronized static Response getHttpResponse(final String url)
	{
		if (msRequester == null)
		{
			msRequester = new ReusableHttpClientBlocking(msTimeout, msUser, msPassword);
		}
		try
		{
			return msRequester.fetch(url);
		}
		catch(Exception e)
		{
			Log.w(TAG, "Caught an exception while fetching url "+url+". Error: "+e.getMessage());
			close();
			e.printStackTrace();
			return new Response(false, 404, "Failed to connect to server");
		}
	}

	public synchronized static void close() {
		if (msRequester != null)
			msRequester.close();
		msRequester = null;
	}
}
