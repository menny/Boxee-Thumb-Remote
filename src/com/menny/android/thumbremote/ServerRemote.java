package com.menny.android.thumbremote;

import java.io.IOException;

public interface ServerRemote {

	int getVolume() throws IOException;
	
	void setVolume(int percent) throws IOException;

	void up() throws IOException;

	void down() throws IOException;

	void left() throws IOException;

	void right() throws IOException;

	void back() throws IOException;

	void select() throws IOException;

	void keypress(int unicode) throws IOException;

	void flipPlayPause() throws IOException;

	void stop() throws IOException;

	void seekRelative(double pct) throws IOException;

	void setServer(ServerAddress server);
	
	void setUiView(UiView uiView);
}