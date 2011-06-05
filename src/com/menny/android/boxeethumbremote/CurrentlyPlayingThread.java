package com.menny.android.boxeethumbremote;

import java.io.IOException;
import java.net.URLEncoder;

import android.graphics.BitmapFactory;
import android.os.Handler;

class CurrentlyPlayingThread extends Thread {
	private static final String TAG = CurrentlyPlayingThread.class.toString();
	
	public static final int MESSAGE_NOW_PLAYING_UPDATED = 1;
	public static final int MESSAGE_THUMBNAIL_UPDATED = 2;
	
	private NowPlaying mPlaying;
	private Handler mHandler;
	private Remote mRemote;

	CurrentlyPlayingThread(Handler handler, Remote remote, NowPlaying playing) {
		mRemote = remote;
		mHandler = handler;
		mPlaying = playing;
	}

	public void run() {
		if (!getCurrentlyPlaying())
			return;

		mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_NOW_PLAYING_UPDATED));
		
		if (mPlaying.getThumbnailUrl() == null)
			return;
		
		if (!getThumbnail())
			return;

		mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_THUMBNAIL_UPDATED));
	}

	private boolean getCurrentlyPlaying() {

		HttpRequestBlocking r = new HttpRequestBlocking(mRemote
				.getRequestString("getcurrentlyplaying()"));
		r.fetch();
		
		if (!r.success())
			return false;

		mPlaying.clear();
		mPlaying.addEntriesFrom(r.response());

		r = new HttpRequestBlocking(mRemote.getRequestString("getguistatus()"));
		r.fetch();
		
		if (!r.success())
			return false;

		mPlaying.addEntriesFrom(r.response());
		
		return true;
	}

	private boolean getThumbnail() {
		final String request = mRemote.getRequestPrefix()
				+ String.format("getthumbnail(%s)", URLEncoder.encode(mPlaying
						.getThumbnailUrl()));
		HttpRequestBlocking r;
		r = new HttpRequestBlocking(request);
		r.fetch();
		if (!r.success()) {
			return false;
		}
		String shorter = r.response().replaceAll("<html>", "").replaceAll(
				"</html>", "");
		byte[] thumb;
		try {
			thumb = iharder.base64.Base64.decode(shorter.getBytes());
		} catch (IOException e) {
			thumb = null;
			e.printStackTrace();
		}
		if (thumb != null)
			mPlaying.setThumbnail(BitmapFactory.decodeByteArray(thumb, 0, thumb.length));

		return true;
	}
}
