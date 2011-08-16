package com.menny.android.thumbremote.ui;

import android.os.AsyncTask;

public abstract class DoServerRemoteAction extends AsyncTask<Void, Void, Exception>
{	/**
	 * sometimes, when there is no network, or stuff, I get lots of Toast windows
	 * which do not disapear for a long time. I say, if the same error happens too often
	 * there is no need to show it.
	 */
	private static String msLastErrorMessage = null;
	private static long msLastErrorMessageTime = 0;
	private static final long MINIMUM_ms_TIME_BETWEEN_ERRORS = 1000;
	
	private final boolean mLongErrorMessageDelay;
	private final RemoteUiActivity mActivity;
	
	protected DoServerRemoteAction(RemoteUiActivity activity, boolean longDelayOfErrorMessage)
	{
		mActivity = activity;
		mLongErrorMessageDelay = longDelayOfErrorMessage;
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
	
	@Override
	protected void onPostExecute(Exception result) {
		super.onPostExecute(result);
		if (result == null)
		{
			mActivity.requestUpdateASAP(100);
			return;
		}
		if (mActivity.mThisAcitivityPaused) return;
		String errorMessage = result.getMessage();
		if (errorMessage == null) errorMessage = "";
		
		//checking for repeating error
		final long currentTime = System.currentTimeMillis();
		if ((!errorMessage.equals(msLastErrorMessage)) || ((currentTime - msLastErrorMessageTime) > MINIMUM_ms_TIME_BETWEEN_ERRORS))
		{
			mActivity.showMessage(errorMessage, mLongErrorMessageDelay? 2500 : 1250);
		}
		msLastErrorMessage = errorMessage;
		msLastErrorMessageTime = currentTime;
	}

	protected abstract void callRemoteFunction() throws Exception;
}