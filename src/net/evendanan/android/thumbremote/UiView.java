package net.evendanan.android.thumbremote;

public interface UiView {
//	void showError(int id, boolean longDelay);
//	void showError(String s, boolean longDelay);
	
	void onPlayingStateChanged(ServerState serverState);
	void onPlayingProgressChanged(ServerState serverState);
	void onMetadataChanged(ServerState serverState);
}
