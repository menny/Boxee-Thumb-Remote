package com.menny.android.thumbremote;

import android.graphics.Bitmap;

public interface ServerState {
	String getMediaType();
	String getMediaTitle();
	String getMediaPlot();
	
	String getMediaTotalTime();
	String getMediaCurrentTime();
	int getMediaProgressPercent();
	
	Bitmap getMediaPoster();
	boolean isMediaActive();
	boolean isMediaPlaying();
}
