package net.evendanan.android.thumbremote.network;

public class Response
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