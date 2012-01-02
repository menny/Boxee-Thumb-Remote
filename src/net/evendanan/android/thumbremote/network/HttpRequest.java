package net.evendanan.android.thumbremote.network;

import android.text.TextUtils;

/*
 * Controls the live cycle of a network connection.
 * Tries to keep the connection alive as long as possible, thus, reusing HTTP connections, and reduce sockets and objects creations.
 */
public class HttpRequest {

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
			close();
			e.printStackTrace();
			return new Response(false, "");
		}
	}

	public synchronized static void close() {
		if (msRequester != null)
			msRequester.close();
		msRequester = null;
	}
}
