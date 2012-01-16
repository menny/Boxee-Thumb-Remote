package net.evendanan.android.thumbremote.network;

public class Response
{
	private final boolean mSuccess;
	private final int mResponseCode;
	private final String mResponse;
	
	Response(boolean success, int responseCode, String response)
	{
		mSuccess = success;
		mResponse = response;
		mResponseCode = responseCode;
	}
	
	/**
	 * Returns whether the fetch resulted in a 200.
	 */
	public boolean success() {
		return mSuccess;
	}
	
	public int responseCode(){
		return mResponseCode;
	}

	/**
	 * Returns the fetched content, or null if the fetch failed.
	 */
	public String response() {
		return mResponse;
	}
}