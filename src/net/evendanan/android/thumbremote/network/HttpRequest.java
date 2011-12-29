package net.evendanan.android.thumbremote.network;

import android.text.TextUtils;

public class HttpRequest {

	// We set these for all requests.
	private static int msTimeout = 2000;
	private static String msPassword;
	private static String msUser;

	public static void setTimeout(int timeout_ms) {
		msTimeout = timeout_ms;
	}

	public static void setUserPassword(String user, String password) {
		msUser = user;
		msPassword = password;
	}
	
	public static boolean hasCredentials()
	{
		return !TextUtils.isEmpty(msUser) && !TextUtils.isEmpty(msPassword);
	}

	public synchronized static Response getHttpResponse(final String url)
	{
		try
		{
			HttpBlocking requester = new HttpClientBlocking(url, msTimeout, msUser, msPassword);
			return requester.fetch();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return new Response(false, "");
		}
	}
}
