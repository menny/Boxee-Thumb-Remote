package net.evendanan.android.thumbremote;

import net.evendanan.android.thumbremote.service.State;

public interface ServerConnectionListener {
	void onServerConnectionStateChanged(State state);
}