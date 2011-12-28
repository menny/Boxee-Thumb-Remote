package net.evendanan.android.thumbremote;

public interface MediaStateListener {

	public abstract void onMediaPlayingStateChanged(ServerState serverState);

	public abstract void onMediaPlayingProgressChanged(ServerState serverState);

	public abstract void onMediaMetadataChanged(ServerState serverState);

	public abstract void onKeyboardStateChanged(ServerState serverState);
}