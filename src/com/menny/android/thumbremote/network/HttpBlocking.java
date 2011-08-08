package com.menny.android.thumbremote.network;

import java.io.IOException;

public interface HttpBlocking {
	Response fetch() throws IOException;
}
