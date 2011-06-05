package com.menny.android.boxeethumbremote;

/**
 * Callback interface for receiving the result of an asynchronous fetch. This
 * will be called from the HttpRequest thread when the fetch completes.
 */
interface CallbackHandler {
	/**
	 * Handle the response.
	 * 
	 * @param success
	 *            true if the fetch completed with a 200 status code
	 * 
	 * @param content
	 *            content of the fetched page, or null if the fetch was
	 *            unsuccessful
	 */
	public void HandleResponse(boolean success, String content);
}