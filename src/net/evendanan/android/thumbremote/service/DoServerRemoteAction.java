package net.evendanan.android.thumbremote.service;

import org.apache.http.client.HttpResponseException;

import android.os.AsyncTask;

abstract class DoServerRemoteAction extends AsyncTask<Void, Void, Exception>
{	
	public static interface DoServerRemoteActionListener
	{
		void onRemoteActionError(String userMessage, int errorCode, boolean longMessageDelay);

		void onRemoteActionSuccess(String successMessage, boolean longDelayMessage);
	}
	/**
	 * sometimes, when there is no network, or stuff, I get lots of Toast windows
	 * which do not disapear for a long time. I say, if the same error happens too often
	 * there is no need to show it.
	 */
	private static String msLastErrorMessage = null;
	private static long msLastErrorMessageTime = 0;
	private static final long MINIMUM_ms_TIME_BETWEEN_ERRORS = 1000;
	private final DoServerRemoteActionListener mListener;
	private final boolean mLongShowMessage;
	
	protected DoServerRemoteAction(DoServerRemoteActionListener listener, boolean longShowMessage)
	{
		mListener = listener;
		mLongShowMessage = 	longShowMessage;
	}
	
	@Override
	protected Exception doInBackground(Void... params) {
		try
		{
			callRemoteFunction();
			return null;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return e;
		}
	}
	
	protected String getSuccessMessage()
	{
		return null;
	}
	
	@Override
	protected void onPostExecute(Exception result) {
		super.onPostExecute(result);
		if (result == null)
		{
			mListener.onRemoteActionSuccess(getSuccessMessage(), mLongShowMessage);
			return;
		}
		
		String errorMessage = result.getMessage();
		if (errorMessage == null) errorMessage = "";
		int errorCode = 404;
		if (result instanceof HttpResponseException)
		{
			HttpResponseException httpError = (HttpResponseException)result;
			errorCode = httpError.getStatusCode();
		}
		//checking for repeating error
		final long currentTime = System.currentTimeMillis();
		if ((!errorMessage.equals(msLastErrorMessage)) || ((currentTime - msLastErrorMessageTime) > MINIMUM_ms_TIME_BETWEEN_ERRORS))
		{
			mListener.onRemoteActionError(errorMessage, errorCode, mLongShowMessage);
		}
		msLastErrorMessage = errorMessage;
		msLastErrorMessageTime = currentTime;
	}

	protected abstract void callRemoteFunction() throws Exception;
}