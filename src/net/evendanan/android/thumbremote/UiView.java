package net.evendanan.android.thumbremote;

public interface UiView extends MediaStateListener, ServerConnectionListener {
	void showMessage(final String userMessage, final int messageTime);
}
