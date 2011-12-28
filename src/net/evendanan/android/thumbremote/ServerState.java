package net.evendanan.android.thumbremote;

import android.graphics.Bitmap;

public interface ServerState {
	String getMediaFilename();
	String getMediaType();
	String getMediaTitle();
	String getShowTitle();
	String getShowSeason();
	String getShowEpisode();
	String getMediaPlot();
	
	String getMediaTotalTime();
	String getMediaCurrentTime();
	int getMediaProgressPercent();
	
	Bitmap getMediaPoster();
	boolean isMediaActive();
	boolean isMediaPlaying();
	
	boolean isKeyboardActive();
	String getKeyboardText();
}
