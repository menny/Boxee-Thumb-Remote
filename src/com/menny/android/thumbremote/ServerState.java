package com.menny.android.thumbremote;

import android.graphics.Bitmap;

public interface ServerState {
	String getMediaTitle();
	
	String getMediaTotalTime();
	String getMediaCurrentTime();
	int getMediaProgressPercent();
	
	Bitmap getMediaPoster();
	boolean isMediaActive();
	boolean isMediaPlaying();
}
