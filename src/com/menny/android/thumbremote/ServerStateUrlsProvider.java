package com.menny.android.thumbremote;

public interface ServerStateUrlsProvider {

	void startOver();

	boolean requiresServerStateData();

	String[] getServerStateUrls();

	void onServerStateResponsesAvailable(String[] responses);

	void onServerStateRetrievalError();

}
