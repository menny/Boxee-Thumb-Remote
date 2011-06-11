/* The following code was written by Menny Even Danan
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.menny.android.boxeethumbremote;

import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

/**
 * NowPlaying represents information we know about the currently playing item in
 * Boxee. It parses the information from Boxee's getcurrentlyplaying() function.
 */
final class NowPlaying {
	private final static String TAG = NowPlaying.class.toString();

	final static Pattern LIST_ITEM = Pattern.compile(
			"^<li>([A-Za-z ]+):([^\n<]+)", Pattern.MULTILINE);

	private HashMap<String, String> mEntries = new HashMap<String, String>();
	private Bitmap mThumbnail;
	
	long mCurTime = 0;
	int mElapsedTime = 0;
	int mDuration = 0;

	public void clear() {
		mEntries.clear();
		mCurTime = 0;
		mElapsedTime = 0;
		mDuration = 0;
	}
	
	public void addEntriesFrom(String s) {
		Matcher m = LIST_ITEM.matcher(s);
		while (m.find()) {
			mEntries.put(m.group(1), m.group(2));
			
			// Special handling for elapsed Time
			if (m.group(1).equals("Time")) {
				mCurTime = new Date().getTime();
				mElapsedTime = hmsToSeconds(m.group(2));
			}
			if (m.group(1).equals("Duration")) {
				mDuration = hmsToSeconds(m.group(2));
			}
			//Log.i(TAG, m.group(1) + ": " + m.group(2));
		}
	}

	boolean isNowPlaying() {
		return mEntries.containsKey("Time");
	}

	boolean isPaused() {
		String playstatus = mEntries.containsKey("PlayStatus") ? mEntries.get("PlayStatus") : "";
		return playstatus.equals("Paused");
	}

	boolean isOnNowPlayingScreen() {
		String screen = mEntries.containsKey("ActiveWindowName") ? mEntries.get("ActiveWindowName") : "";

		return screen.equals("Fullscreen video");
	}

	String getFilename() {
		return mEntries.containsKey("Filename") ? mEntries.get("Filename") : "";
	}

	String getThumbnailUrl() {
		return mEntries.containsKey("Thumb") ? mEntries.get("Thumb") : "";
	}

	String getTitle() {
		String showTitle = mEntries.get("Show Title");
		String title = mEntries.get("Title");
		String filename = mEntries.get("Filename");

		if (!TextUtils.isEmpty(showTitle) && !TextUtils.isEmpty(title))
			return showTitle + " - " + title;

		if (!TextUtils.isEmpty(title))
			return title;

		if (!TextUtils.isEmpty(filename))
			return filename;

		return "";
	}

	int getElapsedSeconds() {
		if (mCurTime == 0)
			return mElapsedTime;
		
		return (int)((new Date().getTime() - mCurTime) / 1000) + mElapsedTime;
	}
	
	String getElapsed() {
		if (isPaused())
			return mEntries.containsKey("Time") ? mEntries.get("Time") : "";
			
		return secondsToHms(getElapsedSeconds());
	}

	String getDuration() {
		return mEntries.containsKey("Duration") ? mEntries.get("Duration") : "";
	}
	
	int getDurationSeconds() {
		return mDuration;
	}

	int getPercentage() {
		int duration = mDuration;
		
		if (duration == 0)
			return 100;

		return getElapsedSeconds() * 100 / duration;
	}

	public Bitmap getThumbnail() {
		return mThumbnail;
	}

	public void setThumbnail(Bitmap thumbnail) {
		mThumbnail = thumbnail;
	}

	public static int hmsToSeconds(String hms) {
		if (TextUtils.isEmpty(hms))
			return 0;

		int seconds = 0;
		String[] times = hms.split(":");

		// seconds
		seconds += Integer.parseInt(times[times.length - 1]);

		// minutes
		if (times.length >= 2)
			seconds += Integer.parseInt(times[times.length - 2]) * 60;

		// hours
		if (times.length >= 3)
			seconds += Integer.parseInt(times[times.length - 3]) * 3600;

		Log.i(TAG, hms + " = " + seconds + " seconds");
		return seconds;
	}
	
	public static String secondsToHms(int seconds) {
		int s = seconds % 60;
		int m = seconds / 60 % 60;
		int h = seconds / 3600;

		if (h > 0)
			return String.format("%d:%02d:%02d", h, m, s);
		else
			return String.format("%02d:%02d", m, s);
	}
}
